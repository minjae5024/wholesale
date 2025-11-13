package minjae5024.marketPrice.service;

import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import minjae5024.marketPrice.entity.Market;
import minjae5024.marketPrice.entity.ProductCategory;
import minjae5024.marketPrice.entity.ProductCode;
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
        String queryDate = StringUtils.hasText(date) ? date.replace("-", "") : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (StringUtils.hasText(productName)) {
            List<ProductCode> foundCodes = productCodeRepository.findByPrdlstNmContaining(productName);
            if (foundCodes.isEmpty()) {
                return Collections.emptyList();
            }
            String productCode = foundCodes.get(0).getPrdlstCd();
            ApiResponseDto<PriceItemDto> response = apiService.fetchPriceDataByProductCode(productCode, mrktCd, queryDate).block();
            if (isSuccessfulResponse(response)) {
                return response.getResultData().getRow();
            }
            return Collections.emptyList();
        }
        else {
            return getProductsByCategory(category, mrktCd, queryDate);
        }
    }

    private List<PriceItemDto> getProductsByCategory(String category, String mrktCd, String queryDate) {
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

    private boolean isSuccessfulResponse(ApiResponseDto<PriceItemDto> response) {
        return response != null && response.getResultData() != null &&
                response.getResultData().getResult() != null && response.getResultData().getResult().getCode().equals("INFO-000") &&
                response.getResultData().getRow() != null;
    }

}
