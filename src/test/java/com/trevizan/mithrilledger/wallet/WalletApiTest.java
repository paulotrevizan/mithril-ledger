package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.controller.dto.TransferRequest;
import com.trevizan.mithrilledger.controller.dto.WalletAmountRequest;
import com.trevizan.mithrilledger.controller.dto.WalletRequest;
import com.trevizan.mithrilledger.domain.exchange.ExchangeClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeClient exchangeClient;

    private UUID createWallet(String ownerId, String currency) throws Exception {
        WalletRequest request = new WalletRequest(ownerId, currency);
        String location = mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

        String id = URI.create(location).getPath();
        id = id.substring(id.lastIndexOf('/') + 1);
        return UUID.fromString(id);
    }

    private BigDecimal creditWallet(UUID walletId, BigDecimal amount) throws Exception {
        WalletAmountRequest request = new WalletAmountRequest(walletId, amount);
        mockMvc.perform(post("/api/v1/wallets/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        return getBalance(walletId);
    }

    private BigDecimal debitWallet(UUID walletId, BigDecimal amount) throws Exception {
        WalletAmountRequest request = new WalletAmountRequest(walletId, amount);
        mockMvc.perform(post("/api/v1/wallets/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        return getBalance(walletId);
    }

    private void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) throws Exception {
        TransferRequest request = new TransferRequest(fromWalletId, toWalletId, amount);
        mockMvc.perform(post("/api/v1/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private BigDecimal getBalance(UUID walletId) throws Exception {
        String content = mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        double balance = objectMapper.readTree(content).get("balance").asDouble();
        return BigDecimal.valueOf(balance);
    }

    @Test
    void shouldReturn200WhenCreateWalletSuccessfully() throws Exception {
        UUID walletId = createWallet("1234", "EUR");

        mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.ownerId").value("1234"))
            .andExpect(jsonPath("$.balance").value(0))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn404WhenWalletDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{id}", "00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.path")
                .value("/api/v1/wallets/00000000-0000-0000-0000-000000000000"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn200WhenCreditAndDebitWalletFlow() throws Exception {
        UUID walletId = createWallet("1234", "EUR");

        assertEquals(BigDecimal.valueOf(100.0), creditWallet(walletId, BigDecimal.valueOf(100.0)));
        assertEquals(BigDecimal.valueOf(60.0), debitWallet(walletId, BigDecimal.valueOf(40.0)));
        assertEquals(BigDecimal.valueOf(60.0), getBalance(walletId));
    }

    @Test
    void shouldReturn409WhenDebitMoreThanBalance() throws Exception {
        UUID walletId = createWallet("1234", "EUR");

        mockMvc.perform(post("/api/v1/wallets/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WalletAmountRequest(walletId, BigDecimal.TEN))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturn201WhenTransferBetweenWallets() throws Exception {
        UUID fromWalletId = createWallet("1234", "EUR");
        UUID toWalletId = createWallet("1235", "EUR");

        creditWallet(fromWalletId, BigDecimal.valueOf(100.0));
        transfer(fromWalletId, toWalletId, BigDecimal.valueOf(50.0));

        assertEquals(BigDecimal.valueOf(50.0), getBalance(fromWalletId));
        assertEquals(BigDecimal.valueOf(50.0), getBalance(toWalletId));
    }

    @Test
    void shouldReturn409WhenTransferWithInsufficientBalance() throws Exception {
        UUID fromWalletId = createWallet("1234", "EUR");
        UUID toWalletId = createWallet("1235", "EUR");

        TransferRequest request = new TransferRequest(fromWalletId, toWalletId, BigDecimal.valueOf(50.0));

        mockMvc.perform(post("/api/v1/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                .value("Wallet " + fromWalletId + " has insufficient balance."))
            .andExpect(jsonPath("$.path").value("/api/v1/wallets/transfer"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn201WhenTransferBetweenWalletsWithDifferentCurrencies() throws Exception {
        UUID fromWalletId = createWallet("1234", "EUR");
        UUID toWalletId = createWallet("1235", "USD");

        BigDecimal creditAmount = BigDecimal.valueOf(100.0);
        when(exchangeClient.getRate(eq("EUR"), eq("USD"))).thenReturn(BigDecimal.valueOf(1.09));

        creditWallet(fromWalletId, creditAmount);
        transfer(fromWalletId, toWalletId, BigDecimal.valueOf(50.0));

        assertEquals(BigDecimal.valueOf(50.0), getBalance(fromWalletId));
        assertEquals(BigDecimal.valueOf(54.50), getBalance(toWalletId));
    }

}
