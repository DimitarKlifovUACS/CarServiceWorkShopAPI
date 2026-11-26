package com.example.CarService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may call the API. The JwtDecoder (which normally checks tokens against Auth0) is replaced by a mock,
 * so these tests run offline: "good-token" is accepted, "bad-token" is rejected like a forged or expired token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void fakeAuth0() {
        Jwt good = Jwt.withTokenValue("good-token")
                .header("alg", "RS256")
                .subject("google-oauth2|123456")
                .build();
        when(jwtDecoder.decode("good-token")).thenReturn(good);
        when(jwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("Invalid signature"));
    }

    @Test
    void requestWithoutTokenIs401AsJson() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Missing or invalid access token"))
                .andExpect(jsonPath("$.instance").value("/vehicles"));
    }

    @Test
    void invalidTokenIs401() throws Exception {
        mockMvc.perform(get("/vehicles").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void oldApiKeyNoLongerWorks() throws Exception {
        mockMvc.perform(get("/vehicles").header("X-API-Key", "local-dev-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        mockMvc.perform(get("/vehicles").header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerIsPublicAndOffersBearerLogin() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void corsPreflightFromFrontendIsAllowed() throws Exception {
        mockMvc.perform(options("/vehicles")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void corsFromOtherWebsiteIsRejected() throws Exception {
        mockMvc.perform(options("/vehicles")
                        .header(HttpHeaders.ORIGIN, "https://some-other-site.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
