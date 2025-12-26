package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.Model.InvalidatedToken;
import com.isaqcasey.aidocsassistant.Repo.InvalidatedTokenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final InvalidatedTokenRepo invalidatedTokenRepo;
    private final JWTService jwtService;
    private final TokenHashingService tokenHashingService;

    public TokenBlacklistService(InvalidatedTokenRepo invalidatedTokenRepo, JWTService jwtService, TokenHashingService tokenHashingService) {
        this.invalidatedTokenRepo = invalidatedTokenRepo;
        this.jwtService = jwtService;
        this.tokenHashingService = tokenHashingService;
    }

    /**
     * Invalidate an access token (add to blacklist)
     * Used during logout to prevent token reuse
     * NOTE: Hashes token before storing for security
     */
    @Transactional
    public void invalidateToken(String plainToken) {
        try {
            // Extract username and expiration from token
            String userName = jwtService.extractUserName(plainToken);
            LocalDateTime expiresAt = convertToLocalDateTime(jwtService.extractExpiration(plainToken));

            // Only add to blacklist if token is still valid
            if (!jwtService.isTokenExpired(plainToken)) {
                // Hash the token before storing
                String hashedToken = tokenHashingService.hashToken(plainToken);

                InvalidatedToken invalidatedToken = new InvalidatedToken(hashedToken, expiresAt, userName);
                invalidatedTokenRepo.save(invalidatedToken);
                log.info("Access token invalidated (hashed) for user: {}", userName);
            }
        } catch (Exception e) {
            log.error("Error invalidating token: {}", e.getMessage());
            // Don't throw exception - logout should still succeed even if blacklisting fails
        }
    }

    /**
     * Check if a token has been invalidated (is in blacklist)
     * NOTE: Hashes the plain token before checking database
     */
    public boolean isTokenInvalidated(String plainToken) {
        String hashedToken = tokenHashingService.hashToken(plainToken);
        return invalidatedTokenRepo.existsByToken(hashedToken);
    }

    /**
     * Cleanup expired invalidated tokens - runs daily at 3:30 AM
     * Runs 30 minutes after refresh token cleanup
     */
    @Scheduled(cron = "0 30 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        invalidatedTokenRepo.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired invalidated tokens");
    }

    /**
     * Convert java.util.Date to LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(java.util.Date date) {
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }
}

