package com.yunesh.digitalwallet.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.yunesh.digitalwallet.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractIntegrationTest {

    @Test
    void register_validRequest_returns201() throws Exception {
        String email = "test+" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest(
                "Test User",
                email,
                "password123",
                "+9779812345678"
        );

        MvcResult result = mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(201);
        assertThat(body.path("data").path("email").asText()).isEqualTo(email);
        assertThat(body.path("data").path("role").asText()).isEqualTo("USER");
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = "duplicate+" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest(
                "Test User",
                email,
                "password123",
                "+9779812345679"
        );

        mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(409);
    }

    @Test
    void login_validCredentials_returnsTokens() throws Exception {
        String email = "login+" + UUID.randomUUID() + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest(
                "Login User",
                email,
                "password123",
                "+9779812345670"
        );

        mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, "password123");

        MvcResult result = mockMvc.perform(
                        post(api("/auth/login"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(body.path("data").path("refreshToken").asText()).isNotBlank();
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "wrong password");

        MvcResult result = mockMvc.perform(
                        post(api("/auth/login"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(401);
    }

    @Test
    void logout_validToken_returns204() throws Exception {
        String email = "logout+" + UUID.randomUUID() + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest(
                "Logout User",
                email,
                "password123",
                "+9779812345671"
        );

        mockMvc.perform(
                        post(api("/auth/register"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, "password123");

        MvcResult loginResult = mockMvc.perform(
                        post(api("/auth/login"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("refreshToken")
                .asText();

        LogoutRequest logoutRequest = new LogoutRequest(refreshToken);

        mockMvc.perform(
                        post(api("/auth/logout"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest))
                )
                .andExpect(status().isNoContent());
    }
}