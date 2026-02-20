package com.trevizan.mithrilledger.exchange;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpServerErrorException;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EnableWireMock
class ExchangeClientIntegrationTest {

    @Autowired
    private ExchangeClient exchangeClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setup() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("exchange-service");
        circuitBreaker.reset();
    }

    @Test
    void shouldReturnRateWhenApiResponds200() {
        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rate\": 0.91}")
            )
        );

        BigDecimal rate = exchangeClient.getRate("USD", "EUR");
        Assertions.assertThat(new BigDecimal("0.91")).isEqualTo(rate);
    }

    @Test
    void shouldThrowExceptionOnServerError() {
        stubFor(post("/api/v1/external/users/validate")
            .willReturn(aResponse()
                .withStatus(500)
            )
        );

        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withStatus(500)
            )
        );

        Assertions.assertThatThrownBy(() -> exchangeClient.getRate("USD", "EUR"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldTimeoutWhenExchangeApiIsSlow() {
        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withFixedDelay(5000)
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rate\": 0.91}")
            )
        );

        Assertions.assertThatThrownBy(() -> exchangeClient.getRate("USD", "EUR"))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void shouldRetryThreeTimesOnServiceExceptionBeforeFallback() {
        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withStatus(500)
            )
        );

        Assertions.assertThatThrownBy(() -> exchangeClient.getRate("USD", "EUR"))
            .isInstanceOf(HttpServerErrorException.class);

        verify(3, getRequestedFor(
            urlPathEqualTo("/api/v1/exchanges/rates"))
        );
    }

    @Test
    void shouldOpenCircuitAfterFourFailedValidationsWithRetryEnabled() {
        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withStatus(500)
            )
        );

        for (int i = 0; i < 4; i++) {
            Assertions.assertThatThrownBy(() -> exchangeClient.getRate("USD", "EUR"))
                .isInstanceOf(RuntimeException.class);
        }

        Assertions.assertThatThrownBy(() -> exchangeClient.getRate("USD", "EUR"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Exchange service unavailable");

        Assertions.assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldCloseCircuitAfterSuccessfulHalfOpenCall() throws InterruptedException {
        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withStatus(500)
            )
        );

        for (int i = 0; i < 4; i++) {
            Assertions.assertThatThrownBy(() -> exchangeClient.getRate("USD", "EUR"))
                .isInstanceOf(RuntimeException.class);
        }

        Assertions.assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(5100);

        stubFor(get(urlPathEqualTo("/api/v1/exchanges/rates"))
            .withQueryParam("from", WireMock.equalTo("USD"))
            .withQueryParam("to", WireMock.equalTo("EUR"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rate\": 0.91}")
            )
        );

        BigDecimal rate = exchangeClient.getRate("USD", "EUR");
        Assertions.assertThat(rate).isEqualByComparingTo("0.91");
        Assertions.assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.CLOSED);
    }

}
