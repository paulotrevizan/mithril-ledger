package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.controller.dto.WalletAmountRequest;
import com.trevizan.mithrilledger.controller.dto.WalletRequest;
import com.trevizan.mithrilledger.domain.Wallet;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.service.WalletService;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    final private Wallet walletCreated =
        Wallet.create("1234", Currency.getInstance("EUR"));

    @Test
    void shouldReturn201WhenCreatingWallet() throws Exception {
        WalletRequest request = new WalletRequest("1234", "EUR");
        when(walletService.createWallet(any(), any())).thenReturn(walletCreated);

        mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location",
                "/api/v1/wallets/" + walletCreated.getId()));
    }

    @Test
    void shouldReturn400WhenOwnerIdIsMissing() throws Exception {
        WalletRequest request = new WalletRequest(null, "EUR");

        mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("OwnerId is required."))
            .andExpect(jsonPath("$.path").value("/api/v1/wallets"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400WhenCurrencyIsInvalid() throws Exception {
        WalletRequest request = new WalletRequest("1234", "XYZ");

        mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Invalid currency code: XYZ"))
            .andExpect(jsonPath("$.path").value("/api/v1/wallets"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn200WhenGetWalletById() throws Exception {
        when(walletService.getWalletById(any())).thenReturn(walletCreated);

        mockMvc.perform(get("/api/v1/wallets/{id}", walletCreated.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(walletCreated.getId().toString()))
            .andExpect(jsonPath("$.ownerId").value("1234"))
            .andExpect(jsonPath("$.balance").value(0))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn404WhenWalletNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(walletService.getWalletById(id)).thenThrow(new WalletNotFoundException(id));

        mockMvc.perform(get("/api/v1/wallets/" + id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Wallet not found: " + id))
            .andExpect(jsonPath("$.path").value("/api/v1/wallets/" + id))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn200WhenCreditWallet() throws Exception {
        UUID walletId = walletCreated.getId();
        BigDecimal amount = BigDecimal.valueOf(100);

        when(walletService.credit(walletId, amount)).thenReturn(walletCreated);

        mockMvc.perform(post("/api/v1/wallets/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WalletAmountRequest(walletId, amount))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(walletId.toString()))
            .andExpect(jsonPath("$.ownerId").value("1234"));
    }

    @Test
    void shouldReturn200WhenDebitWallet() throws Exception {
        UUID walletId = walletCreated.getId();
        BigDecimal amount = BigDecimal.valueOf(50);

        when(walletService.debit(walletId, amount)).thenReturn(walletCreated);

        mockMvc.perform(post("/api/v1/wallets/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WalletAmountRequest(walletId, amount))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(walletId.toString()))
            .andExpect(jsonPath("$.ownerId").value("1234"));
    }

    @Test
    void shouldReturn404WhenCreditWalletNotFound() throws Exception {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100);

        when(walletService.credit(walletId, amount))
            .thenThrow(new WalletNotFoundException(walletId));

        mockMvc.perform(post("/api/v1/wallets/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WalletAmountRequest(walletId, amount))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn404WhenDebitWalletNotFound() throws Exception {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50);

        when(walletService.debit(walletId, amount))
            .thenThrow(new WalletNotFoundException(walletId));

        mockMvc.perform(post("/api/v1/wallets/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WalletAmountRequest(walletId, amount))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

}
