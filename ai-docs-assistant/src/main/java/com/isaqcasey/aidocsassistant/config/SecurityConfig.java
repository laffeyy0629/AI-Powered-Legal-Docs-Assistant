package com.isaqcasey.aidocsassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource())) // enable CORS
            .csrf(csrf -> csrf.disable()) // disable CSRF correctly
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/user/signup", "/api/status", "/api/**", "/error").permitAll() // public endpoints
                    .anyRequest().authenticated() // everything else requires auth
            )
            .formLogin(form -> form.permitAll()) // enable form-based login
            .httpBasic(basic -> basic.disable()); // disable basic auth popup

        return http.build();
    }
}
