package minjae5024.marketPrice.service;

import minjae5024.marketPrice.dto.ApiResponseDto;
import minjae5024.marketPrice.dto.PriceItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.StringJoiner;

@Slf4j
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
                .doOnError(e -> log.error("API call failed for URL: {}. Error: {}", fullUrl, e.getMessage()))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying API call for URL: {}. Retry count: {}", fullUrl, retrySignal.totalRetries() + 1))
                )
                .onErrorResume(e -> {
                    log.error("API call failed after retries for URL: {}. Returning empty response.", fullUrl, e);
                    return Mono.just(new ApiResponseDto<>());
                });
    }
}