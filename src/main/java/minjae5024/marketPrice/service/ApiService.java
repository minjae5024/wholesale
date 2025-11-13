package minjae5024.marketPrice.service;

import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.StringJoiner;

@Service
public class ApiService {

    private final WebClient webClient;

    @Value("${api.key}")
    private String apiKey;
    @Value("${api.url}")
    private String baseUrl;
    @Value("${api.id}")
    private String priceServiceId;

    public ApiService(WebClient.Builder webClientBuilder, @Value("${api.url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<ApiResponseDto<PriceItemDto>> fetchPriceDataByCategory(String categoryCode, String mrktCd, String date) {
        String path = new StringJoiner("/")
                .add(apiKey).add("json").add(priceServiceId).add("1").add("100").toString();

        String queryParams = new StringJoiner("&")
                .add("EXAMIN_DE=" + date)
                .add("FRMPRD_CATGORY_CD=" + categoryCode)
                .add("MRKT_CD=" + mrktCd)
                .toString();

        return callApi(path + "?" + queryParams);
    }

    public Mono<ApiResponseDto<PriceItemDto>> fetchPriceDataByProductCode(String productCode, String mrktCd, String date) {
        String path = new StringJoiner("/")
                .add(apiKey).add("json").add(priceServiceId).add("1").add("100").toString();

        String queryParams = new StringJoiner("&")
                .add("EXAMIN_DE=" + date)
                .add("PRDLST_CD=" + productCode)
                .add("MRKT_CD=" + mrktCd)
                .toString();

        return callApi(path + "?" + queryParams);
    }

    private Mono<ApiResponseDto<PriceItemDto>> callApi(String fullPath) {
        String fullUrl = baseUrl + "/" + fullPath;
        return webClient.get()
                .uri(fullUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<PriceItemDto>>() {})
                .onErrorResume(e -> Mono.just(new ApiResponseDto<>()));
    }
}