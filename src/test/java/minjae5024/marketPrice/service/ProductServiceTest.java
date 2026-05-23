package minjae5024.marketPrice.service;

import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import minjae5024.marketPrice.entity.DailyPrice;
import minjae5024.marketPrice.entity.Market;
import minjae5024.marketPrice.entity.ProductCategory;
import minjae5024.marketPrice.repository.DailyPriceRepository;
import minjae5024.marketPrice.repository.MarketRepository;
import minjae5024.marketPrice.repository.ProductCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private DailyPriceRepository dailyPriceRepository;
    @Mock
    private ApiService apiService;
    @Mock
    private ProductCodeRepository productCodeRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("DB에 데이터가 있으면 API를 호출하지 않고 DB 데이터를 반환한다")
    void getProducts_fromDb() {
        // Given
        Long marketId = 1L;
        String date = "2024-05-02";
        Market market = new Market("0110253", "서울 양곡도매", 37.4, 127.0);
        given(marketRepository.findById(marketId)).willReturn(Optional.of(market));

        DailyPrice dailyPrice = new DailyPrice("0110253", LocalDate.of(2024, 5, 2), "쌀", "일반", "특", "20kg", 50000L, "100");
        given(dailyPriceRepository.findByMrktCdAndExamineDate(anyString(), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice));

        // When
        List<PriceItemDto> result = productService.getProducts(marketId, "ALL", null, date);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrdlstNm()).isEqualTo("쌀");
        verify(apiService, never()).fetchPriceDataByCategory(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("DB에 데이터가 없으면 API를 호출하여 결과를 반환한다")
    void getProducts_fallbackToApi() {
        // Given
        Long marketId = 1L;
        String date = "2024-05-02";
        Market market = new Market("0110253", "서울 양곡도매", 37.4, 127.0);
        given(marketRepository.findById(marketId)).willReturn(Optional.of(market));

        given(dailyPriceRepository.findByMrktCdAndExamineDateAndCategoryCode(anyString(), any(LocalDate.class), anyString()))
                .willReturn(Collections.emptyList());

        PriceItemDto apiItem = new PriceItemDto();
        apiItem.setPrdlstNm("배추");
        apiItem.setAmt("3000");

        ApiResponseDto<PriceItemDto> apiResponse = new ApiResponseDto<>();
        ApiResponseDto.GridResult<PriceItemDto> gridResult = new ApiResponseDto.GridResult<>();
        gridResult.setRow(List.of(apiItem));
        ApiResponseDto.ResultInfo resultInfo = new ApiResponseDto.ResultInfo();
        resultInfo.setCode("INFO-000");
        gridResult.setResult(resultInfo);
        apiResponse.setGridResult("someKey", gridResult);

        given(apiService.fetchPriceDataByCategory(anyString(), anyString(), anyString()))
                .willReturn(Mono.just(apiResponse));

        // When
        List<PriceItemDto> result = productService.getProducts(marketId, "VEGETABLES", null, date);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrdlstNm()).isEqualTo("배추");
        verify(apiService, times(1)).fetchPriceDataByCategory(anyString(), anyString(), anyString());
    }
}
