package minjae5024.marketPrice.repository;

import minjae5024.marketPrice.entity.DailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {
    List<DailyPrice> findByMrktCdAndExamineDate(String mrktCd, LocalDate examineDate);
    List<DailyPrice> findByMrktCdAndExamineDateAndCategoryCode(String mrktCd, LocalDate examineDate, String categoryCode);
    List<DailyPrice> findByMrktCdAndExamineDateAndPrdlstNmContaining(String mrktCd, LocalDate examineDate, String prdlstNm);
    Optional<DailyPrice> findByMrktCdAndExamineDateAndPrdlstNmAndSpciesNmAndGradNm(
            String mrktCd, LocalDate examineDate, String prdlstNm, String spciesNm, String gradNm
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByExamineDateBefore(LocalDate date);
}
