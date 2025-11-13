package minjae5024.marketPrice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class MarketPriceMapApplication {
	public static void main(String[] args) {
		SpringApplication.run(MarketPriceMapApplication.class, args);
	}
}
