package com.isaqcasey.aidocsassistant.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService
{
    private static final Logger log = LoggerFactory.getLogger(JWTService.class);
    
    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.expiration.ms:900000}") // Default 15 minutes (industry standard for access tokens)
    private long expirationMs;

    private Key key;

    @PostConstruct
    public void init()
    {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generate access token with claims
     */
    public String generateToken(String userName)
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        return createToken(claims, userName, expirationMs);
    }

    /**
     * Generate access token with custom claims
     */
    public String generateToken(String userName, Map<String, Object> extraClaims)
    {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("type", "access");
        return createToken(claims, userName, expirationMs);
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration)
    {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extract username from token
     */
    public String extractUserName(String token)
    {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token)
    {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver)
    {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token)
    {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token)
    {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token against username
     */
    public boolean validateToken(String token, String username)
    {
        try {
            final String extractedUsername = extractUserName(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for user: {}", username);
            return false;
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token (without username check)
     */
    public boolean validateToken(String token)
    {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired");
            return false;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
