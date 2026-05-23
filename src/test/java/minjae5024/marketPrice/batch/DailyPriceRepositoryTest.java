package minjae5024.marketPrice.batch;

import minjae5024.marketPrice.entity.DailyPrice;
import minjae5024.marketPrice.repository.DailyPriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
@org.springframework.transaction.annotation.Transactional
class DailyPriceRepositoryTest {

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Test
    @DisplayName("Upsert 조회를 위한 복합키 조회 테스트")
    void findByUniqueKeyTest() {
        // Given
        LocalDate date = LocalDate.now();
        DailyPrice price = new DailyPrice("M01", date, "사과", "부사", "특", "10kg", 50000L, "400");
        dailyPriceRepository.save(price);

        // When
        Optional<DailyPrice> found = dailyPriceRepository.findByMrktCdAndExamineDateAndPrdlstNmAndSpciesNmAndGradNm(
                "M01", date, "사과", "부사", "특"
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAmt()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("6개월 이전 데이터 삭제 테스트")
    void deleteOldDataTest() {
        // Given
        LocalDate now = LocalDate.now();
        LocalDate sevenMonthsAgo = now.minusMonths(7);
        LocalDate fiveMonthsAgo = now.minusMonths(5);

        dailyPriceRepository.save(new DailyPrice("M01", sevenMonthsAgo, "사과", "부사", "특", "10kg", 50000L, "400"));
        dailyPriceRepository.save(new DailyPrice("M02", fiveMonthsAgo, "배", "신고", "특", "15kg", 60000L, "400"));

        // When
        dailyPriceRepository.deleteByExamineDateBefore(now.minusMonths(6));

        // Then
        assertThat(dailyPriceRepository.findAll()).hasSize(1);
        assertThat(dailyPriceRepository.findAll().get(0).getPrdlstNm()).isEqualTo("배");
    }
}
