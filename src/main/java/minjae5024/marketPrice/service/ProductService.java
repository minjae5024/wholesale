package minjae5024.marketPrice.service;

import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import minjae5024.marketPrice.entity.DailyPrice;
import minjae5024.marketPrice.entity.Market;
import minjae5024.marketPrice.entity.ProductCategory;
import minjae5024.marketPrice.entity.ProductCode;
import minjae5024.marketPrice.repository.DailyPriceRepository;
import minjae5024.marketPrice.repository.MarketRepository;
import minjae5024.marketPrice.repository.ProductCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final MarketRepository marketRepository;
    private final ProductCodeRepository productCodeRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final ApiService apiService;

    @Cacheable(
            cacheNames = "products",
            key = "T(java.lang.String).format('%s|%s|%s|%s', #marketId, (#category == null ? 'ALL' : #category), (#productName == null ? '' : #productName), (#date == null ? T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd')) : #date.replace('-', '')))",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<PriceItemDto> getProducts(Long marketId, String category, String productName, String date) {
        Optional<Market> marketOptional = marketRepository.findById(marketId);
        if (marketOptional.isEmpty()) {
            return Collections.emptyList();
        }

        String mrktCd = marketOptional.get().getMrktCd();
        String queryDateStr = StringUtils.hasText(date) ? date.replace("-", "") : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate queryDate = LocalDate.parse(queryDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 1. DB에서 먼저 조회
        List<DailyPrice> dbResults;
        if (StringUtils.hasText(productName)) {
            dbResults = dailyPriceRepository.findByMrktCdAndExamineDateAndPrdlstNmContaining(mrktCd, queryDate, productName);
        } else {
            if (category == null || category.equalsIgnoreCase("ALL")) {
                dbResults = dailyPriceRepository.findByMrktCdAndExamineDate(mrktCd, queryDate);
            } else {
                try {
                    String categoryCode = ProductCategory.valueOf(category.toUpperCase()).getApiCode();
                    dbResults = dailyPriceRepository.findByMrktCdAndExamineDateAndCategoryCode(mrktCd, queryDate, categoryCode);
                } catch (IllegalArgumentException e) {
                    return Collections.emptyList();
                }
            }
        }

        // 2. DB에 데이터가 있으면 변환 후 반환
        if (!dbResults.isEmpty()) {
            return dbResults.stream().map(this::convertToDto).collect(Collectors.toList());
        }

        // 3. DB에 없으면 API 호출 (Fallback)
        return fetchFromApiAndSave(mrktCd, category, productName, queryDateStr);
    }

    private List<PriceItemDto> fetchFromApiAndSave(String mrktCd, String category, String productName, String queryDate) {
        if (StringUtils.hasText(productName)) {
            List<ProductCode> foundCodes = productCodeRepository.findByPrdlstNmContaining(productName);
            if (foundCodes.isEmpty()) return Collections.emptyList();
            
            String productCode = foundCodes.get(0).getPrdlstCd();
            ApiResponseDto<PriceItemDto> response = apiService.fetchPriceDataByProductCode(productCode, mrktCd, queryDate).block();
            return isSuccessfulResponse(response) ? response.getResultData().getRow() : Collections.emptyList();
        } else {
            return getProductsByCategoryFromApi(category, mrktCd, queryDate);
        }
    }

    private List<PriceItemDto> getProductsByCategoryFromApi(String category, String mrktCd, String queryDate) {
        List<ProductCategory> categoriesToFetch = new ArrayList<>();
        if (category == null || category.equalsIgnoreCase("ALL")) {
            categoriesToFetch.addAll(Arrays.asList(ProductCategory.values()));
        } else {
            try {
                categoriesToFetch.add(ProductCategory.valueOf(category.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Collections.emptyList();
            }
        }

        return categoriesToFetch.parallelStream()
                .map(pc -> apiService.fetchPriceDataByCategory(pc.getApiCode(), mrktCd, queryDate).block())
                .filter(this::isSuccessfulResponse)
                .flatMap(response -> response.getResultData().getRow().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private PriceItemDto convertToDto(DailyPrice dailyPrice) {
        PriceItemDto dto = new PriceItemDto();
        dto.setPrdlstNm(dailyPrice.getPrdlstNm());
        dto.setSpciesNm(dailyPrice.getSpciesNm());
        dto.setGradNm(dailyPrice.getGradNm());
        dto.setExaminUnit(dailyPrice.getExaminUnit());
        dto.setAmt(dailyPrice.getAmt().toString());
        return dto;
    }

    private boolean isSuccessfulResponse(ApiResponseDto<PriceItemDto> response) {
        return response != null && response.getResultData() != null &&
                response.getResultData().getResult() != null && response.getResultData().getResult().getCode().equals("INFO-000") &&
                response.getResultData().getRow() != null;
    }
}
