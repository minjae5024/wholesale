package minjae5024.marketPrice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = {
    @Index(name = "idx_daily_price_search", columnList = "mrktCd, examineDate, prdlstNm")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_daily_price", columnNames = {"mrktCd", "examineDate", "prdlstNm", "spciesNm", "gradNm"})
})
public class DailyPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mrktCd;

    @Column(nullable = false)
    private LocalDate examineDate;

    @Column(nullable = false)
    private String prdlstNm;

    private String spciesNm;

    private String gradNm;

    private String examinUnit;

    private Long amt;

    private String categoryCode;

    public DailyPrice(String mrktCd, LocalDate examineDate, String prdlstNm, String spciesNm, String gradNm, String examinUnit, Long amt, String categoryCode) {
        this.mrktCd = mrktCd;
        this.examineDate = examineDate;
        this.prdlstNm = prdlstNm;
        this.spciesNm = spciesNm;
        this.gradNm = gradNm;
        this.examinUnit = examinUnit;
        this.amt = amt;
        this.categoryCode = categoryCode;
    }
}
