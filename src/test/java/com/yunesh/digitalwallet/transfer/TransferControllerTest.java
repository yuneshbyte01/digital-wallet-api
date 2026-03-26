package com.yunesh.digitalwallet.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.yunesh.digitalwallet.AbstractIntegrationTest;
import com.yunesh.digitalwallet.auth.LoginRequest;
import com.yunesh.digitalwallet.auth.RegisterRequest;
import com.yunesh.digitalwallet.user.SetPinRequest;
import com.yunesh.digitalwallet.wallet.DepositRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferControllerTest extends AbstractIntegrationTest {

    private String senderToken;
    private String senderPhone;
    private String receiverPhone;

    @BeforeEach
    void setUp() throws Exception {
        String senderEmail = "sender+" + UUID.randomUUID() + "@example.com";
        String receiverEmail = "receiver+" + UUID.randomUUID() + "@example.com";
        senderPhone = "+97798" + randomDigits();
        receiverPhone = "+97798" + randomDigits();

        mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new RegisterRequest("Sender", senderEmail, "password123", senderPhone)
                                ))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new RegisterRequest("Receiver", receiverEmail, "password123", receiverPhone)
                                ))
                )
                .andExpect(status().isCreated());

        jdbcTemplate.update("""
                INSERT INTO wallets (id, user_id, currency, status, version)
                SELECT '00000000-0000-0000-0000-000000000000',
                       id, 'SYSTEM', 'ACTIVE', 0
                FROM users WHERE email = ?
                ON CONFLICT DO NOTHING
                """, senderEmail);

        MvcResult loginResult = mockMvc.perform(
                        post(api("/auth/login"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(senderEmail, "password123")
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        senderToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();

        mockMvc.perform(
                        post(api("/users/me/pin"))
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new SetPinRequest("1234")))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post(api("/wallets/deposit"))
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new DepositRequest(new BigDecimal("5000.00"), UUID.randomUUID())
                                ))
                )
                .andExpect(status().isOk());
    }

    @Test
    void transfer_happyPath_returns200() throws Exception {
        TransferRequest request = new TransferRequest(
                receiverPhone,
                new BigDecimal("500.00"),
                "1234",
                UUID.randomUUID(),
                "Test transfer"
        );

        MvcResult result = mockMvc.perform(
                        post(api("/transfers"))
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    void transfer_selfTransfer_returns400() throws Exception {
        TransferRequest request = new TransferRequest(
                senderPhone,
                new BigDecimal("500.00"),
                "1234",
                UUID.randomUUID(),
                "Self transfer"
        );

        MvcResult result = mockMvc.perform(
                        post(api("/transfers"))
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(400);
    }

    @Test
    void transfer_idempotentRequest_returnsSameResponse() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                receiverPhone,
                new BigDecimal("500.00"),
                "1234",
                idempotencyKey,
                "Idempotent transfer"
        );

        String json = objectMapper.writeValueAsString(request);

        MvcResult first = mockMvc.perform(
                        post(api("/transfers"))
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(
                        post(api("/transfers"))
                                .header("Authorization", "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();

        String secondId = objectMapper.readTree(second.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();

        assertThat(firstId).isEqualTo(secondId);
    }

    private String randomDigits() {
        String uuidDigits = UUID.randomUUID().toString().replaceAll("\\D", "");
        if (uuidDigits.length() >= 8) {
            return uuidDigits.substring(0, 8);
        }
        return String.format("%1$-" + 8 + "s", uuidDigits).replace(' ', '0');
    }
}