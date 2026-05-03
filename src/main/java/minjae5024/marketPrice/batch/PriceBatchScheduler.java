package minjae5024.marketPrice.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job syncPriceJob;
    private final Job cleanupJob;

    // 매일 새벽 4시에 실행
    @Scheduled(cron = "0 0 4 * * *")
    public void runSyncPriceJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            log.info("Starting Price Sync Job...");
            jobLauncher.run(syncPriceJob, params);
            log.info("Price Sync Job completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during Price Sync Job execution", e);
        }
    }

    // 매월 1일 새벽 5시에 실행
    @Scheduled(cron = "0 0 5 1 * *")
    public void runCleanupJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Starting Monthly Cleanup Job...");
            jobLauncher.run(cleanupJob, params);
            log.info("Cleanup Job completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during Cleanup Job execution", e);
        }
    }
}
