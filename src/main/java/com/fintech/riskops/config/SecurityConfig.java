package com.fintech.riskops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * For this demo, users are hardcoded in memory.
 * In production: replace UserDetailsService with DB-backed impl,
 * add JWT tokens, and integrate with your IdP (Okta, Auth0, etc.)
 *
 * Roles:
 * - ANALYST:   read-only access + can trigger risk evaluations
 * - RISK_OPS:  analyst permissions + bulk actions + merchant create/update
 * - ADMIN:     full access including delete and CSV export
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Disabled for REST API; use CSRF tokens in browser-facing apps
            .authorizeHttpRequests(auth -> auth
                // Public: H2 console (dev only), actuator health
                .requestMatchers("/h2-console/**", "/actuator/health").permitAll()
                // Dashboard is accessible to all authenticated users
                .requestMatchers(HttpMethod.GET, "/api/v1/merchants/dashboard/**").authenticated()
                // Everything else requires authentication (fine-grained control via @PreAuthorize)
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})  // Basic auth for simplicity; swap for JWT in production
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); // H2 console

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("analyst")
                .password(encoder.encode("analyst123"))
                .roles("ANALYST")
                .build(),
            User.withUsername("riskops")
                .password(encoder.encode("riskops123"))
                .roles("RISK_OPS")
                .build(),
            User.withUsername("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
