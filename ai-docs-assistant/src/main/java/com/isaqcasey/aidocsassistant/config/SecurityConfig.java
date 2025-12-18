package com.isaqcasey.aidocsassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig
{

    // HASH PASSWORD
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    // DETERMINE WHICH URI SHOULD REQUIRED AUTHENTICATION
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http.csrf(csrf -> csrf.disable()) // disable CSRF correctly
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/user/signup").permitAll() // public endpoints
                    .anyRequest().authenticated() // everything else requires auth
            );

        return http.build();
    }
}
