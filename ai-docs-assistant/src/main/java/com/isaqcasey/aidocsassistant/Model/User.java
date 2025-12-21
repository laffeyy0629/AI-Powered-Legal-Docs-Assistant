package com.isaqcasey.aidocsassistant.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.isaqcasey.aidocsassistant.Service.UserService;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    private long id;

    @JsonProperty("user_name")
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required", groups = {UserService.OnLogin.class,  UserService.OnCreate.class})
    @Size(min = 8, max = 20, message = "Username must be between 8 and 20 characters")
    private String userName;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required", groups = {UserService.OnCreate.class})
    @Email(message = "Email not found")
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    @NotBlank(message = "Password is required", groups = {UserService.OnLogin.class,  UserService.OnCreate.class})
    @Size(min = 8, message = "Password must be at least 8 characters")
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