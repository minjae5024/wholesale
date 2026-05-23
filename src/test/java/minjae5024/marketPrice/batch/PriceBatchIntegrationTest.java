package minjae5024.marketPrice.batch;

import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import minjae5024.marketPrice.entity.DailyPrice;
import minjae5024.marketPrice.entity.ProductCategory;
import minjae5024.marketPrice.repository.DailyPriceRepository;
import minjae5024.marketPrice.repository.MarketRepository;
import minjae5024.marketPrice.service.ApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
@Transactional
class PriceBatchIntegrationTest {

    @Autowired
    private ItemProcessor<PriceBatchConfig.PriceSyncTask, List<DailyPrice>> processor;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @MockBean
    private ApiService apiService;

    @Test
    @DisplayName("Upsert 로직 테스트: 기존 데이터가 있으면 업데이트, 없으면 생성")
    void upsertLogicTest() throws Exception {
        // Given
        String mrktCd = "0110253";
        LocalDate today = LocalDate.now();
        
        DailyPrice existing = new DailyPrice(mrktCd, today, "쌀", "일반계", "특", "20kg", 50000L, "100");
        dailyPriceRepository.save(existing);

        PriceItemDto item = new PriceItemDto();
        item.setPrdlstNm("쌀");
        item.setSpciesNm("일반계");
        item.setGradNm("특");
        item.setExaminUnit("20kg");
        item.setAmt("55,000");

        ApiResponseDto.GridResult<PriceItemDto> gridResult = new ApiResponseDto.GridResult<>();
        gridResult.setRow(List.of(item));

        ApiResponseDto<PriceItemDto> mockResponse = new ApiResponseDto<>();
        mockResponse.setGridResult("Grid_20150406000000000217_1", gridResult);

        when(apiService.fetchPriceDataByCategory(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(mockResponse));

        // When
        PriceBatchConfig.PriceSyncTask task = new PriceBatchConfig.PriceSyncTask(mrktCd, ProductCategory.FOOD_CROPS);
        List<DailyPrice> processedPrices = processor.process(task);

        // Then
        assertThat(processedPrices).isNotNull();
        assertThat(processedPrices.get(0).getAmt()).isEqualTo(55000L);
        
        Optional<DailyPrice> saved = dailyPriceRepository.findByMrktCdAndExamineDateAndPrdlstNmAndSpciesNmAndGradNm(
                mrktCd, today, "쌀", "일반계", "특"
        );
        assertThat(saved).isPresent();
    }
}
