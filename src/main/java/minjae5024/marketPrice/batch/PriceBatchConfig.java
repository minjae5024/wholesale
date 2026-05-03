package minjae5024.marketPrice.batch;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import minjae5024.marketPrice.entity.DailyPrice;
import minjae5024.marketPrice.entity.Market;
import minjae5024.marketPrice.entity.ProductCategory;
import minjae5024.marketPrice.repository.DailyPriceRepository;
import minjae5024.marketPrice.repository.MarketRepository;
import minjae5024.marketPrice.service.ApiService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PriceBatchConfig {

    private final MarketRepository marketRepository;
    private final ApiService apiService;
    private final DailyPriceRepository dailyPriceRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job syncPriceJob(JobRepository jobRepository, Step fetchAndSaveStep) {
        return new JobBuilder("syncPriceJob", jobRepository)
                .start(fetchAndSaveStep)
                .build();
    }

    @Bean
    public Job cleanupJob(JobRepository jobRepository, Step cleanupOldDataStep) {
        return new JobBuilder("cleanupJob", jobRepository)
                .start(cleanupOldDataStep)
                .build();
    }

    @Bean
    public Step cleanupOldDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("cleanupOldDataStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
                    log.info("Cleaning up data older than {}", sixMonthsAgo);
                    dailyPriceRepository.deleteByExamineDateBefore(sixMonthsAgo);
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step fetchAndSaveStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("fetchAndSaveStep", jobRepository)
                .<PriceSyncTask, List<DailyPrice>>chunk(1, transactionManager)
                .reader(priceSyncTaskReader())
                .processor(priceSyncTaskProcessor())
                .writer(dailyPriceListWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<PriceSyncTask> priceSyncTaskReader() {
        List<Market> markets = marketRepository.findAll();
        List<PriceSyncTask> tasks = new ArrayList<>();
        
        for (Market market : markets) {
            for (ProductCategory category : ProductCategory.values()) {
                tasks.add(new PriceSyncTask(market.getMrktCd(), category));
            }
        }
        return new ListItemReader<>(tasks);
    }

    @Bean
    public ItemProcessor<PriceSyncTask, List<DailyPrice>> priceSyncTaskProcessor() {
        return task -> {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.info("Fetching data for Market: {}, Category: {}, Date: {}", task.mrktCd, task.category, date);
            
            ApiResponseDto<PriceItemDto> response = apiService.fetchPriceDataByCategory(
                    task.category.getApiCode(), 
                    task.mrktCd, 
                    date
            ).block();

            if (response == null || response.getResultData() == null || response.getResultData().getRow() == null) {
                return null;
            }

            return response.getResultData().getRow().stream()
                    .map(item -> {
                        DailyPrice existingPrice = dailyPriceRepository.findByMrktCdAndExamineDateAndPrdlstNmAndSpciesNmAndGradNm(
                                task.mrktCd,
                                LocalDate.now(),
                                item.getPrdlstNm(),
                                item.getSpciesNm(),
                                item.getGradNm()
                        ).orElse(null);

                        if (existingPrice != null) {
                            existingPrice.setAmt(parseAmount(item.getAmt()));
                            existingPrice.setExaminUnit(item.getExaminUnit());
                            existingPrice.setCategoryCode(task.category.getApiCode());
                            return existingPrice;
                        } else {
                            return new DailyPrice(
                                    task.mrktCd,
                                    LocalDate.now(),
                                    item.getPrdlstNm(),
                                    item.getSpciesNm(),
                                    item.getGradNm(),
                                    item.getExaminUnit(),
                                    parseAmount(item.getAmt()),
                                    task.category.getApiCode()
                            );
                        }
                    })
                    .collect(Collectors.toList());
        };
    }

    @Bean
    public JpaItemWriter<DailyPrice> dailyPriceWriter() {
        return new JpaItemWriterBuilder<DailyPrice>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public ItemWriter<List<DailyPrice>> dailyPriceListWriter() {
        return chunk -> {
            List<DailyPrice> allPrices = chunk.getItems().stream()
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            if (!allPrices.isEmpty()) {
                dailyPriceWriter().write(new org.springframework.batch.item.Chunk<>(allPrices));
            }
        };
    }

    private Long parseAmount(String amt) {
        if (amt == null) return 0L;
        try {
            return Long.parseLong(amt.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public record PriceSyncTask(String mrktCd, ProductCategory category) {}
}
