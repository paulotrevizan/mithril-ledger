package com.trevizan.mithrilledger.exchange;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EnableWireMock
class ExchangeClientIntegrationTest {

    @Autowired
    private ExchangeClient exchangeClient;

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

        Assertions.assertThatThrownBy(() ->
                exchangeClient.getRate("USD", "EUR"))
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

        Assertions.assertThatThrownBy(() ->
                exchangeClient.getRate("USD", "EUR"))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(SocketTimeoutException.class);
    }

}
