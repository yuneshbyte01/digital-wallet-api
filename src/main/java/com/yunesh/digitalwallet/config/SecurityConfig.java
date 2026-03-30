package com.yunesh.digitalwallet.config;

import com.yunesh.digitalwallet.auth.AuthService;
import com.yunesh.digitalwallet.auth.JwtAuthenticationFilter;
import com.yunesh.digitalwallet.ratelimit.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final EncoderConfig encoderConfig;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(authService);
        provider.setPasswordEncoder(encoderConfig.getPasswordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) {
        try {
            return config.getAuthenticationManager();
        } catch (Exception e) {
            throw new RuntimeException("Could not create AuthenticationManager", e);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .headers(headers ->
                        headers.httpStrictTransportSecurity(hsts ->
                                hsts.includeSubDomains(true)
                                        .maxAgeInSeconds(31536000)))

                .addFilterBefore(httpsEnforcementFilter(),
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(
                                        org.springframework.http.HttpMethod.OPTIONS, "/**")
                                .permitAll()
                                .requestMatchers("/api/v1/auth/**").permitAll()
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                ).permitAll()
                                .requestMatchers("/api/v1/admin/**")
                                .hasRole("ADMIN")
                                .requestMatchers("/api/v1/compliance/**")
                                .hasRole("COMPLIANCE_OFFICER")
                                .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public OncePerRequestFilter httpsEnforcementFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request,
                                            @NonNull HttpServletResponse response,
                                            @NonNull FilterChain filterChain)
                    throws ServletException, IOException {

                String proto = request.getHeader("X-Forwarded-Proto");
                if (proto != null && !proto.equals("https")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "HTTPS required");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}