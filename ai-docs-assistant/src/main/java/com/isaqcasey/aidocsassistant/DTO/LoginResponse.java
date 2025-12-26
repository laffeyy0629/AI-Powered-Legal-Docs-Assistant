package com.isaqcasey.aidocsassistant.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse
{
    private final boolean success;
    private final String message;

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("refresh_token")
    private final String refreshToken;

    @JsonProperty("token_type")
    private final String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private final long expiresIn; // in seconds

    public LoginResponse(boolean success, String message, String accessToken, String refreshToken, long expiresIn)
    {
        this.success = success;
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    // Constructor for error responses
    public LoginResponse(boolean success, String message)
    {
        this.success = success;
        this.message = message;
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresIn = 0;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public String getMessage()
    {
        return message;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public String getTokenType()
    {
        return tokenType;
    }

    public long getExpiresIn()
    {
        return expiresIn;
    }
}
