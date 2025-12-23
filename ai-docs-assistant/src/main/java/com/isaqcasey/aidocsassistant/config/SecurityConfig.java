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
    public SecurityFilterChain filterChain(HttpSecurity http, JWTFilter jwtFilter, JWTService jwtService) throws Exception {
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
                            "/error",
                            "/auth/**",
                            "/oauth2/**"
                    ).permitAll() // public endpoints
                    .anyRequest().authenticated()
                ).oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            try {
                                // Generate JWT
                                OAuth2User user = (OAuth2User) authentication.getPrincipal();
                                String email = user.getAttribute("email");
                                
                                if (email == null) {
                                    response.setStatus(400);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"success\":false,\"message\":\"Email attribute not found in OAuth2 response\"}");
                                    return;
                                }
                                
                                String token = jwtService.generateToken(email);

                                // Return JWT as JSON - properly escaped
                                response.setContentType("application/json");
                                String jsonResponse = String.format(
                                    "{\"success\":true,\"email\":\"%s\",\"token\":\"%s\"}",
                                    email.replace("\"", "\\\"").replace("\\", "\\\\"),
                                    token.replace("\"", "\\\"").replace("\\", "\\\\")
                                );
                                response.getWriter().write(jsonResponse);
                            } catch (Exception e) {
                                response.setStatus(500);
                                response.setContentType("application/json");
                                try {
                                    response.getWriter().write("{\"success\":false,\"message\":\"OAuth2 authentication failed\"}");
                                } catch (Exception ex) {
                                    // Log or handle the nested exception
                                }
                            }
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
