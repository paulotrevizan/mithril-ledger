package com.trevizan.mithrilledger.infrastructure.exchange;

import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;
import com.trevizan.mithrilledger.exception.infrastructure.ExchangeInvalidResponseException;
import com.trevizan.mithrilledger.exception.infrastructure.ExchangeServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpExchangeClient implements ExchangeClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpExchangeClient(
        RestTemplate restTemplate,
        @Value("${exchange.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    @Retry(name = "exchange-service")
    @CircuitBreaker(
        name = "exchange-service",
        fallbackMethod = "circuitBreakerFallback"
    )
    public BigDecimal getRate(String from, String to) {
        String url = baseUrl + "/api/v1/exchanges/rates?from=" +  from + "&to=" + to;

        try {
            ExchangeResponse response = restTemplate.getForObject(url, ExchangeResponse.class);

            if (response == null || response.rate() == null) {
                throw new ExchangeInvalidResponseException(from, to);
            }
            return response.rate();
        } catch (RestClientException ex) {
            throw new ExchangeServiceUnavailableException(
                from,
                to,
                "Exchange service call failed",
                ex
            );
        }
    }

    private BigDecimal circuitBreakerFallback(String from, String to, CallNotPermittedException ex) {
        throw new ExchangeServiceUnavailableException(
            from,
            to,
            "Circuit breaker is OPEN for exchange service",
            ex
        );
    }

}
