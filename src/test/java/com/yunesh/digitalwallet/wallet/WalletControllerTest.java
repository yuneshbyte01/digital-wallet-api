package com.yunesh.digitalwallet.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.yunesh.digitalwallet.AbstractIntegrationTest;
import com.yunesh.digitalwallet.auth.LoginRequest;
import com.yunesh.digitalwallet.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest extends AbstractIntegrationTest {

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "wallet+" + UUID.randomUUID() + "@example.com";
        String phone = "+97798" + randomDigits();

        RegisterRequest registerRequest = new RegisterRequest(
                "Wallet User",
                email,
                "password123",
                phone
        );

        mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated());

        jdbcTemplate.update("""
                INSERT INTO wallets (id, user_id, currency, status, version)
                SELECT '00000000-0000-0000-0000-000000000000',
                       id, 'SYSTEM', 'ACTIVE', 0
                FROM users WHERE email = ?
                ON CONFLICT DO NOTHING
                """, email);

        LoginRequest loginRequest = new LoginRequest(email, "password123");

        MvcResult loginResult = mockMvc.perform(
                        post(api("/auth/login"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    @Test
    void getWallet_authenticatedUser_returns200() throws Exception {
        MvcResult result = mockMvc.perform(
                        get(api("/wallets/me"))
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("currency").asText()).isEqualTo("NPR");
        assertThat(body.path("data").path("balance").decimalValue())
                .isEqualByComparingTo(new BigDecimal("0.0"));
    }

    @Test
    void deposit_validAmount_updatesBalance() throws Exception {
        DepositRequest request = new DepositRequest(
                new BigDecimal("1000.00"),
                UUID.randomUUID()
        );

        MvcResult result = mockMvc.perform(
                        post(api("/wallets/deposit"))
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("balance").decimalValue())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    private String randomDigits() {
        String uuidDigits = UUID.randomUUID().toString().replaceAll("\\D", "");
        if (uuidDigits.length() >= 8) {
            return uuidDigits.substring(0, 8);
        }
        return String.format("%1$-" + 8 + "s", uuidDigits).replace(' ', '0');
    }
}