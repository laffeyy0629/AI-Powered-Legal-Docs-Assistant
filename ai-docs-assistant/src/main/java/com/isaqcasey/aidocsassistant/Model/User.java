package com.isaqcasey.aidocsassistant.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long iD;

    @JsonProperty("user_name")
    @Column(unique = true, nullable = false)
    private String userName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @JsonProperty("o_auth_provider")
    private String oAuthProvider;

    @JsonProperty("o_auth_id")
    private String oAuthId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}