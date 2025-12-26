package com.isaqcasey.aidocsassistant.Repo;

import com.isaqcasey.aidocsassistant.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long>
{
    Optional<User> findUserByUserName(String userName);
    Optional<User> findUserByEmail(String email);
    Optional<User> findUserByVerificationToken(String verificationToken);
    Optional<User> findUserByPasswordResetToken(String passwordResetToken);

    @Modifying
    @Query("UPDATE User u SET u.verificationToken = null, u.verificationTokenExpiry = null WHERE u.verificationTokenExpiry < :now AND u.emailVerified = false")
    int clearExpiredVerificationTokens(LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.passwordResetToken = null, u.passwordResetTokenExpiry = null WHERE u.passwordResetTokenExpiry < :now")
    int clearExpiredPasswordResetTokens(LocalDateTime now);
}
