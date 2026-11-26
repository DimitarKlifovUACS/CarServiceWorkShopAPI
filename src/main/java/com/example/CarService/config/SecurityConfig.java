package com.example.CarService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.List;

/**
 * Who may call the API:
 * - Swagger UI and the OpenAPI description are public (Render's health check uses /v3/api-docs).
 * - Everything else needs "Authorization: Bearer <access token>" from Auth0 (see application.properties).
 * - Browsers may only call the API from the origins in ALLOWED_ORIGINS (CORS).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint jsonEntryPoint) throws Exception {
        http
                .cors(Customizer.withDefaults()) // uses the corsConfigurationSource bean below
                // CSRF protection is for cookie-based logins; this API only accepts tokens in a header, so it is not needed
                .csrf(csrf -> csrf.disable())
                // No server-side sessions: every request is checked on its own token
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(jsonEntryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(jsonEntryPoint));
        return http.build();
    }

    // 401 responses use the same JSON "problem details" format as every other error
    @Bean
    public AuthenticationEntryPoint jsonEntryPoint(ObjectMapper objectMapper) {
        BearerTokenAuthenticationEntryPoint standard = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            standard.commence(request, response, exception); // sets 401 and the standard WWW-Authenticate header
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing or invalid access token");
            problem.setInstance(URI.create(request.getRequestURI()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), problem);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(3600L); // browsers may cache the preflight answer for an hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
