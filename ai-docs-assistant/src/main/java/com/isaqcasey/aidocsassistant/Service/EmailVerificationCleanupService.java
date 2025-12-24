package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EmailVerificationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationCleanupService.class);

    private final UserRepo userRepo;

    public EmailVerificationCleanupService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Cleanup expired verification tokens - runs daily at 4 AM
     * Removes expired tokens from unverified accounts for security
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanupExpiredVerificationTokens() {
        try {
            int cleaned = userRepo.clearExpiredVerificationTokens(LocalDateTime.now());
            if (cleaned > 0) {
                log.info("Cleaned up {} expired email verification tokens", cleaned);
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired verification tokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup expired password reset tokens - runs daily at 4:30 AM
     * Removes expired reset tokens for security
     */
    @Scheduled(cron = "0 30 4 * * ?")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        try {
            int cleaned = userRepo.clearExpiredPasswordResetTokens(LocalDateTime.now());
            if (cleaned > 0) {
                log.info("Cleaned up {} expired password reset tokens", cleaned);
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired password reset tokens: {}", e.getMessage(), e);
        }
    }
}

