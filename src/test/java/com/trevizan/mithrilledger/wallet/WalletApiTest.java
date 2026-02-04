package com.trevizan.mithrilledger.wallet;

import com.trevizan.mithrilledger.controller.dto.WalletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

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

    @Test
    void shouldCreateAndFetchWalletSuccessfully() throws Exception {
        WalletRequest request = new WalletRequest("1234", "EUR");

        String location = mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

        // then: fetch wallet
        mockMvc.perform(get(location))
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

}
