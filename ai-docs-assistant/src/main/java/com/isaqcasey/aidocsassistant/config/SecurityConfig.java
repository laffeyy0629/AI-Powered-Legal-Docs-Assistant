package com.isaqcasey.aidocsassistant.config;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Security.JWTFilter;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.OAuthService;
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

        // Specify exact headers instead of "*" for better security
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",      // Required for JWT Bearer tokens
                "Content-Type",       // Required for JSON requests/responses
                "Accept",            // Required for content negotiation
                "Origin",            // Required for CORS
                "X-Requested-With"   // Common for AJAX requests
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // DETERMINE WHICH URI SHOULD REQUIRED AUTHENTICATION
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JWTFilter jwtFilter, JWTService jwtService, OAuthService oauthService) throws Exception {
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
                            "/user/verify-email",
                            "/user/resend-verification",
                            "/user/refresh",
                            "/user/logout",
                            "/user/forgot-password",
                            "/user/reset-password",
                            "/user/forgot-username",
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
                                System.out.println("=== OAuth2 Login Success Handler Started ===");

                                // Get OAuth2 user information
                                OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

                                System.out.println("OAuth2 User Attributes: " + oauthUser.getAttributes());
                                System.out.println("Authentication Name: " + authentication.getName());
                                System.out.println("Authentication Details: " + authentication.getDetails());

                                String email = oauthUser.getAttribute("email");
                                String name = oauthUser.getAttribute("name");
                                String providerId = oauthUser.getAttribute("sub"); // Google uses 'sub' for user ID

                                System.out.println("Extracted - Email: " + email + ", Name: " + name + ", ProviderId: " + providerId);

                                if (email == null) {
                                    System.out.println("ERROR: Email is null, redirecting with error");
                                    response.sendRedirect("http://localhost:5173/auth/callback?error=no_email");
                                    return;
                                }
                                
                                // Determine provider - check the authorization request
                                String provider = "google"; // Default to google since we're using Google OAuth
                                System.out.println("Provider: " + provider);

                                // Create or update user in database
                                System.out.println("Calling OAuthService.findOrCreateOAuthUser...");
                                User user = oauthService.findOrCreateOAuthUser(email, name, provider, providerId);
                                System.out.println("User created/found: " + user.getUserName() + " (ID: " + user.getId() + ")");

                                // Generate JWT token using the username from database
                                System.out.println("Generating JWT token...");
                                String token = jwtService.generateToken(user.getUserName());
                                System.out.println("JWT token generated successfully");

                                // Redirect to frontend callback with token
                                String redirectUrl = "http://localhost:5173/auth/callback?token=" + token;
                                System.out.println("Redirecting to: " + redirectUrl);
                                response.sendRedirect(redirectUrl);
                                System.out.println("=== OAuth2 Login Success Handler Completed ===");

                            } catch (Exception e) {
                                System.err.println("=== OAuth2 Login Error ===");
                                System.err.println("Error Type: " + e.getClass().getName());
                                System.err.println("Error Message: " + e.getMessage());
                                e.printStackTrace();

                                // Redirect to frontend with error
                                try {
                                    response.sendRedirect("http://localhost:5173/auth/callback?error=auth_failed");
                                } catch (Exception ex) {
                                    System.err.println("Failed to redirect after error:");
                                    ex.printStackTrace();
                                }
                            }
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
