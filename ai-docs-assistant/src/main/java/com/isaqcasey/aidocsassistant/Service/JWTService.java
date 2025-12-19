package com.isaqcasey.aidocsassistant.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JWTService
{
    private final Key key = Keys.hmacShaKeyFor("my-secret-key-12345678901234567890".getBytes());

    public String generateToken(String userName)
    {
        return Jwts.builder()
                .subject(userName)
                .signWith(key)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .compact();
    }

    public String extractUserName(String token)
    {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
