package com.isaqcasey.aidocsassistant.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
public class InvalidatedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "invalidated_at", nullable = false)
    private LocalDateTime invalidatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "user_name")
    private String userName;

    public InvalidatedToken() {
        this.invalidatedAt = LocalDateTime.now();
    }

    public InvalidatedToken(String token, LocalDateTime expiresAt, String userName) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.userName = userName;
        this.invalidatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

