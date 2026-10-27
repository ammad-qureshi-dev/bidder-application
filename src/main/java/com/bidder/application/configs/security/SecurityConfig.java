/* (C) 2025
bidder.app */
package com.bidder.application.configs.security;

import com.bidder.application.controllers.filters.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final List<String> whitelistEndpoints;

  // comma-separated, e.g. "https://biddr-frontend.onrender.com,http://localhost:3000"
  @Value("${client-url}")
  private String clientUrls;

  // under /identity-and-auth/api/v1/auth/** (public) but they act on the logged-in user
  private static final String[] AUTHENTICATED_AUTH_ENDPOINTS = {
    "/identity-and-auth/api/v1/auth/account-verification/send-link",
    "/identity-and-auth/api/v1/auth/account-verification/is-verified"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    List<String> permitAllPatterns = new ArrayList<>(whitelistEndpoints);
    permitAllPatterns.addAll(
        List.of(
            "/identity-and-auth/api/v1/auth/**",
            "/api/*/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/v3/api-docs.yaml",
            "/swagger-resources/**"));

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(AUTHENTICATED_AUTH_ENDPOINTS)
                    .authenticated()
                    .requestMatchers(permitAllPatterns.toArray(new String[0]))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // CORS headers are already added by the CorsFilter, which runs before these handlers
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setContentType("application/json");
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setContentType("application/json");
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        }));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    // origins must match the browser's Origin header exactly, so drop whitespace and trailing
    // slashes
    config.setAllowedOrigins(
        Arrays.stream(clientUrls.split(","))
            .map(String::trim)
            .filter(url -> !url.isEmpty())
            .map(url -> url.replaceAll("/+$", ""))
            .toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
    config.setAllowCredentials(true);
    config.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
