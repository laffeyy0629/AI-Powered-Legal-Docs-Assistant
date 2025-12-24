package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.Model.RefreshToken;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.RefreshTokenRepo;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepo refreshTokenRepo;
    private final UserRepo userRepo;
    private final TokenHashingService tokenHashingService;
    private final EntityManager entityManager;

    @Value("${jwt.refresh.expiration.days:7}")
    private int refreshTokenExpirationDays;

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo, UserRepo userRepo,
                               TokenHashingService tokenHashingService, EntityManager entityManager) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.userRepo = userRepo;
        this.tokenHashingService = tokenHashingService;
        this.entityManager = entityManager;
    }

    /**
     * Creates a new refresh token for a user
     * Automatically revokes any existing active tokens for the user (single device policy)
     * NOTE: Returns the PLAIN token to send to client, but stores HASHED version in DB
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepo.findUserByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Revoke all existing tokens for this user (enforce single active session)
        revokeAllUserTokens(user.getId());

        // Generate plain token (UUID)
        String plainToken = UUID.randomUUID().toString();

        // Hash the token for secure storage
        String hashedToken = tokenHashingService.hashToken(plainToken);

        // Create new refresh token with HASHED token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(hashedToken); // Store hashed version
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));

        refreshToken = refreshTokenRepo.save(refreshToken);

        // Flush to ensure DB write happens
        entityManager.flush();

        // CRITICAL: Detach the entity from persistence context
        // This prevents JPA from auto-updating when we set the plain token
        entityManager.detach(refreshToken);

        // NOW set the plain token for return to client
        // This will NOT be persisted because entity is detached
        refreshToken.setToken(plainToken);

        log.info("Created new refresh token for user: {}", username);
        return refreshToken;
    }

    /**
     * Validates a refresh token and returns it if valid
     * Checks: token exists, not expired, not revoked
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepo.findByToken(token)
                .filter(rt -> !rt.isExpired())
                .filter(rt -> !rt.isRevoked());
    }

    /**
     * Rotates a refresh token - creates new token and revokes old one
     * This is the industry standard for refresh token rotation
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken) {
        RefreshToken oldRefreshToken = refreshTokenRepo.findByToken(oldToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (oldRefreshToken.isExpired()) {
            throw new RuntimeException("Refresh token expired");
        }

        if (oldRefreshToken.isRevoked()) {
            // Token reuse detected - possible security breach
            // Revoke all tokens for this user as precaution
            log.warn("Revoked token reuse detected for user: {}. Revoking all tokens.",
                    oldRefreshToken.getUser().getUserName());
            revokeAllUserTokens(oldRefreshToken.getUser().getId());
            throw new RuntimeException("Token reuse detected. All tokens revoked for security.");
        }

        // Generate new plain token
        String newPlainToken = UUID.randomUUID().toString();
        String newHashedToken = tokenHashingService.hashToken(newPlainToken);

        // Create new token with HASHED version
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(oldRefreshToken.getUser());
        newRefreshToken.setToken(newHashedToken); // Store hashed
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        newRefreshToken = refreshTokenRepo.save(newRefreshToken);

        // Revoke old token and link to new one (store hash of new token)
        oldRefreshToken.setRevoked(true);
        oldRefreshToken.setRevokedAt(LocalDateTime.now());
        oldRefreshToken.setReplacedByToken(newHashedToken); // Store hash
        refreshTokenRepo.save(oldRefreshToken);

        // Flush to ensure DB writes happen
        entityManager.flush();

        // CRITICAL: Detach entity before setting plain token
        entityManager.detach(newRefreshToken);

        // Set plain token for return to client (will NOT be persisted)
        newRefreshToken.setToken(newPlainToken);

        log.info("Rotated refresh token for user: {}", oldRefreshToken.getUser().getUserName());
        return newRefreshToken;
    }

    /**
     * Revokes a specific refresh token
     * NOTE: Hashes plain token before database lookup
     */
    @Transactional
    public void revokeRefreshToken(String plainToken) {
        String hashedToken = tokenHashingService.hashToken(plainToken);

        refreshTokenRepo.findByToken(hashedToken).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepo.save(rt);
            log.info("Revoked refresh token for user: {}", rt.getUser().getUserName());
        });
    }

    /**
     * Revokes all refresh tokens for a user
     * Used during logout or security events
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepo.revokeAllUserTokens(userId, LocalDateTime.now());
        log.info("Revoked all refresh tokens for user ID: {}", userId);
    }

    /**
     * Cleanup expired tokens - runs daily at 3 AM
     * This is a scheduled task to keep the database clean
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepo.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired refresh tokens");
    }

    /**
     * Get user from refresh token
     * NOTE: Hashes plain token before database lookup
     */
    public Optional<User> getUserFromRefreshToken(String plainToken) {
        String hashedToken = tokenHashingService.hashToken(plainToken);

        return refreshTokenRepo.findByToken(hashedToken)
                .filter(rt -> !rt.isExpired())
                .filter(rt -> !rt.isRevoked())
                .map(RefreshToken::getUser);
    }
}

