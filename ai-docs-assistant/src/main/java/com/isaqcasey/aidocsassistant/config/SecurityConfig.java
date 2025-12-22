package com.isaqcasey.aidocsassistant.config;

import com.isaqcasey.aidocsassistant.Security.JWTFilter;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig
{

    // HASH PASSWORD
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    // CONFIGURE CORS FOR FRONTEND
    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000")); // Vite and other dev servers
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // DETERMINE WHICH URI SHOULD REQUIRED AUTHENTICATION
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JWTFilter jwtFilter, JWTService jWTService) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                // 1. MUST set to STATELESS for JWT APIs
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/",
                            "/user/signup",
                            "/user/login",
                            "/user/status",
                            "/api/status",
                            "/api/**",
                            "/error",
                            "/auth/**",
                            "/oauth2/**"
                    ).permitAll() // public endpoints
                    .anyRequest().authenticated()
                ).oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            // Generate JWT
                            OAuth2User user = (OAuth2User) authentication.getPrincipal();
                            String email = user.getAttribute("email");
                            String token = jWTService.generateToken(email);

                            // Return JWT as JSON
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true,\"email\":\"" + email + "\",\"token\":\"" + token + "\"}");
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
