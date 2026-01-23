package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.Model.RefreshToken;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import com.isaqcasey.aidocsassistant.Service.EmailService;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.RefreshTokenService;
import com.isaqcasey.aidocsassistant.Service.TokenHashingService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserImpl implements UserService
{
    private static final Logger log = LoggerFactory.getLogger(UserImpl.class);
    private final UserRepo repo;
    private final PasswordEncoder encoder;
    private final JWTService jwt;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final TokenHashingService tokenHashingService;

    @Value("${jwt.expiration.ms:900000}")
    private long jwtExpirationMs;

    // DEPENDENCY INJECTION
    public UserImpl(UserRepo repo, PasswordEncoder encoder, JWTService jwt,
                    EmailService emailService, RefreshTokenService refreshTokenService,
                    TokenHashingService tokenHashingService)
    {
        this.repo    = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.tokenHashingService = tokenHashingService;
    }

    // INPUT VALIDATOR
    @Override
    public Map<String, Object> getInputValidationResult(BindingResult result)
    {
        Map<String, Object> response = new HashMap<>();

        if(result.hasErrors())
        {
            Map<String, Object> errors = new HashMap<>();

            result.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });

            response.put("success", false);
            response.put("errors", errors);

            return response;
        }

        response.put("success", true);

        return response;
    }

    // USER REGISTRATION
    @Override
    public Map<String, Object> store(User user)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Trim inputs
            String username = user.getUserName().trim();
            String email = user.getEmail().trim();
            String password = user.getPassword().trim();

            // Validate password length
            if(password.length() < 8)
            {
                response.put("success", false);
                response.put("message", "Password must be at least 8 characters long");
                return response;
            }

            // Check if username exists
            if(repo.findUserByUserName(username).isPresent())
            {
                response.put("success", false);
                response.put("message", "Username already exists. Please choose a different username.");
                return response;
            }

            // Check if email exists
            if(repo.findUserByEmail(email).isPresent())
            {
                response.put("success", false);
                response.put("message", "Email already registered. Please use a different email or try logging in.");
                return response;
            }

            // Generate verification token (plain)
            String plainVerificationToken = UUID.randomUUID().toString();
            String hashedVerificationToken = tokenHashingService.hashToken(plainVerificationToken);
            LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);

            // Create new user
            user.setUserName(username);
            user.setEmail(email);
            user.setPassword(encoder.encode(password));
            user.setEmailVerified(false);
            user.setVerificationToken(hashedVerificationToken); // Store hashed version
            user.setVerificationTokenExpiry(tokenExpiry);

            repo.save(user);

            // Send verification email with PLAIN token
            boolean emailSent = emailService.sendVerificationEmail(email, username, plainVerificationToken);

            if (!emailSent) {
                log.warn("Failed to send verification email to: {}", email);
                response.put("success", true);
                response.put("message", "Registration successful! However, we couldn't send the verification email. Please contact support.");
                return response;
            }

            response.put("success", true);
            response.put("message", "Registration successful! Please check your email to verify your account.");
            response.put("requiresVerification", true);

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred during registration: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred during registration. Please try again later.");

            return exception;
        }
    }

    // USER LOGIN
    public Map<String, Object> login(User user)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Trim inputs
            String username = user.getUserName().trim();
            String password = user.getPassword().trim();

            // Find user by username
            User foundUser = repo.findUserByUserName(username).orElse(null);

            if(foundUser == null)
            {
                response.put("success", false);
                response.put("message", "Invalid username or password. Please check your credentials and try again.");
                return response;
            }

            // Verify password
            if(!encoder.matches(password, foundUser.getPassword()))
            {
                response.put("success", false);
                response.put("message", "Invalid username or password. Please check your credentials and try again.");
                return response;
            }

            // Check if email is verified (only for non-OAuth users)
            if(foundUser.getOAuthProvider() == null && !foundUser.isEmailVerified())
            {
                response.put("success", false);
                response.put("message", "Please verify your email before logging in. Check your inbox for the verification link.");
                response.put("requiresVerification", true);
                response.put("email", foundUser.getEmail());
                return response;
            }

            // Generate JWT access token
            String accessToken = jwt.generateToken(foundUser.getUserName());

            // Generate refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(foundUser.getUserName());

            response.put("success", true);
            response.put("message", "Login successful! Redirecting to dashboard...");
            response.put("access_token", accessToken);
            response.put("refresh_token", refreshToken.getToken());
            response.put("token_type", "Bearer");
            response.put("expires_in", jwtExpirationMs / 1000); // Convert to seconds

            // Also include legacy 'token' field for backward compatibility
            response.put("token", accessToken);

            log.info("User logged in successfully: {}", username);
            return response;

        }
        catch(Exception error)
        {
            log.error("Error occurred during login: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred during login. Please try again later.");

            return exception;
        }
    }

    // EMAIL VERIFICATION
    @Override
    public Map<String, Object> verifyEmail(String plainToken)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Hash the token before database lookup
            String hashedToken = tokenHashingService.hashToken(plainToken);

            // Find user by hashed verification token
            User user = repo.findUserByVerificationToken(hashedToken).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "Invalid or expired verification link. Please request a new verification email.");
                return response;
            }

            // Check if token has expired
            if(user.getVerificationTokenExpiry().isBefore(LocalDateTime.now()))
            {
                // Invalidate expired token for security
                user.setVerificationToken(null);
                user.setVerificationTokenExpiry(null);
                repo.save(user);

                log.warn("Expired verification token attempted for user: {}", user.getUserName());

                response.put("success", false);
                response.put("message", "Verification link has expired. Please request a new verification email.");
                response.put("expired", true);
                response.put("email", user.getEmail());
                return response;
            }

            // Check if already verified
            if(user.isEmailVerified())
            {
                // Clear token even if already verified (security)
                if(user.getVerificationToken() != null) {
                    user.setVerificationToken(null);
                    user.setVerificationTokenExpiry(null);
                    repo.save(user);
                }

                response.put("success", true);
                response.put("message", "Email already verified. You can now log in.");
                response.put("alreadyVerified", true);
                return response;
            }

            // Verify the user and immediately invalidate token (one-time use)
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpiry(null);
            repo.save(user);

            log.info("Email verified successfully for user: {}", user.getUserName());

            response.put("success", true);
            response.put("message", "Email verified successfully! You can now log in with your credentials.");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred during email verification: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred during verification. Please try again later.");

            return exception;
        }
    }

    // RESEND VERIFICATION EMAIL
    @Override
    public Map<String, Object> resendVerificationEmail(String email)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Find user by email
            User user = repo.findUserByEmail(email.trim()).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "No account found with this email address.");
                return response;
            }

            // Check if already verified
            if(user.isEmailVerified())
            {
                response.put("success", false);
                response.put("message", "Email already verified. You can log in.");
                return response;
            }

            // Generate new verification token (plain)
            String plainVerificationToken = UUID.randomUUID().toString();
            String hashedVerificationToken = tokenHashingService.hashToken(plainVerificationToken);
            LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);

            user.setVerificationToken(hashedVerificationToken); // Store hashed
            user.setVerificationTokenExpiry(tokenExpiry);
            repo.save(user);

            // Send verification email with PLAIN token
            boolean emailSent = emailService.sendVerificationEmail(user.getEmail(), user.getUserName(), plainVerificationToken);

            if (!emailSent) {
                log.warn("Failed to resend verification email to: {}", email);
                response.put("success", false);
                response.put("message", "Failed to send verification email. Please try again later.");
                return response;
            }

            log.info("Verification email resent to: {}", email);

            response.put("success", true);
            response.put("message", "Verification email sent! Please check your inbox.");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred while resending verification email: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");

            return exception;
        }
    }

    // FORGOT PASSWORD - Send password reset email
    @Override
    public Map<String, Object> forgotPassword(String email)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Find user by email
            User user = repo.findUserByEmail(email.trim()).orElse(null);

            if(user == null)
            {
                // Don't reveal if email exists (security)
                response.put("success", true);
                response.put("message", "If an account exists with this email, a password reset link has been sent.");
                return response;
            }

            // Check if user is OAuth user (can't reset password)
            if(user.getOAuthProvider() != null && !user.getOAuthProvider().isEmpty())
            {
                response.put("success", false);
                response.put("message", "This account uses " + user.getOAuthProvider() + " login. Please use " + user.getOAuthProvider() + " to sign in.");
                return response;
            }

            // Generate password reset token (plain)
            String plainResetToken = UUID.randomUUID().toString();
            String hashedResetToken = tokenHashingService.hashToken(plainResetToken);
            LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(1); // 1 hour expiration

            user.setPasswordResetToken(hashedResetToken); // Store hashed version
            user.setPasswordResetTokenExpiry(tokenExpiry);
            repo.save(user);

            // Send password reset email with PLAIN token
            boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), user.getUserName(), plainResetToken);

            if (!emailSent) {
                log.warn("Failed to send password reset email to: {}", email);
                response.put("success", false);
                response.put("message", "Failed to send password reset email. Please try again later.");
                return response;
            }

            log.info("Password reset email sent to: {}", email);

            response.put("success", true);
            response.put("message", "If an account exists with this email, a password reset link has been sent. Please check your inbox.");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred during forgot password: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");

            return exception;
        }
    }

    // RESET PASSWORD - Reset password with token
    @Override
    public Map<String, Object> resetPassword(String plainToken, String newPassword)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Validate password
            if(newPassword == null || newPassword.trim().length() < 8)
            {
                response.put("success", false);
                response.put("message", "Password must be at least 8 characters long.");
                return response;
            }

            // Hash the token before database lookup
            String hashedToken = tokenHashingService.hashToken(plainToken);

            // Find user by hashed reset token
            User user = repo.findUserByPasswordResetToken(hashedToken).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "Invalid or expired reset link. Please request a new password reset.");
                return response;
            }

            // Check if token has expired
            if(user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now()))
            {
                // Invalidate expired token
                user.setPasswordResetToken(null);
                user.setPasswordResetTokenExpiry(null);
                repo.save(user);

                log.warn("Expired password reset token attempted for user: {}", user.getUserName());

                response.put("success", false);
                response.put("message", "Reset link has expired. Please request a new password reset.");
                response.put("expired", true);
                return response;
            }

            // Reset password and invalidate token (one-time use)
            user.setPassword(encoder.encode(newPassword.trim()));
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            repo.save(user);

            log.info("Password reset successfully for user: {}", user.getUserName());

            response.put("success", true);
            response.put("message", "Password reset successfully! You can now log in with your new password.");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred during password reset: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred during password reset. Please try again later.");

            return exception;
        }
    }

    // FORGOT USERNAME - Send username reminder email
    @Override
    public Map<String, Object> forgotUsername(String email)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Find user by email
            User user = repo.findUserByEmail(email.trim()).orElse(null);

            if(user == null)
            {
                // Don't reveal if email exists (security)
                response.put("success", true);
                response.put("message", "If an account exists with this email, a username reminder has been sent.");
                return response;
            }

            // Send username reminder email
            boolean emailSent = emailService.sendUsernameReminderEmail(user.getEmail(), user.getUserName());

            if (!emailSent) {
                log.warn("Failed to send username reminder email to: {}", email);
                response.put("success", false);
                response.put("message", "Failed to send username reminder. Please try again later.");
                return response;
            }

            log.info("Username reminder sent to: {}", email);

            response.put("success", true);
            response.put("message", "If an account exists with this email, a username reminder has been sent. Please check your inbox.");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred during forgot username: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");

            return exception;
        }
    }

    // GET USER PROFILE
    @Override
    public Map<String, Object> getUserProfile(String username)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            User user = repo.findUserByUserName(username).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            // Simple and clear: OAuth-only accounts have NULL password
            // Manual accounts always have a password (set during registration)
            boolean hasRealPassword = user.getPassword() != null && !user.getPassword().isEmpty();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", user.getUserName());
            userInfo.put("email", user.getEmail());
            userInfo.put("emailVerified", user.isEmailVerified());
            userInfo.put("oAuthProvider", user.getOAuthProvider());
            userInfo.put("createdAt", user.getCreatedAt());
            userInfo.put("hasPassword", hasRealPassword); // Simple: NULL = no password

            response.put("success", true);
            response.put("user", userInfo);

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred while fetching user profile: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();
            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");
            return exception;
        }
    }

    // GET OAUTH LINK INFO - For consent page
    @Override
    public Map<String, Object> getOAuthLinkInfo(String email, String sessionId)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            // Validate session (in production, implement proper session management)
            // For now, just verify the user exists
            User user = repo.findUserByEmail(email.trim()).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "No account found with this email");
                return response;
            }

            // Return account info for consent page
            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put("username", user.getUserName());
            accountInfo.put("email", user.getEmail());
            accountInfo.put("email_verified", user.isEmailVerified());

            response.put("success", true);
            response.put("account", accountInfo);

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred while fetching OAuth link info: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();
            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");
            return exception;
        }
    }

    // CONFIRM OAUTH LINK - User explicitly consents
    @Override
    public Map<String, Object> confirmOAuthLink(String email, String provider, String sessionId)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            User user = repo.findUserByEmail(email.trim()).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "No account found with this email");
                return response;
            }

            // Check if email is verified
            if(!user.isEmailVerified())
            {
                response.put("success", false);
                response.put("message", "Please verify your email before linking OAuth account");
                return response;
            }

            // Link OAuth provider
            user.setOAuthProvider(provider);
            // Note: oAuthId should be set from OAuth callback, using session for now
            user.setOAuthId(sessionId);
            repo.save(user);

            log.info("OAuth provider {} linked to account: {}", provider, user.getUserName());

            // Send notification email
            emailService.sendOAuthLinkedEmail(user.getEmail(), user.getUserName(), provider);

            // Generate tokens for login
            String accessToken = jwt.generateToken(user.getUserName());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserName());

            response.put("success", true);
            response.put("message", "OAuth account linked successfully!");
            response.put("access_token", accessToken);
            response.put("refresh_token", refreshToken.getToken());
            response.put("token_type", "Bearer");
            response.put("expires_in", 900); // 15 minutes

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred while confirming OAuth link: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();
            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");
            return exception;
        }
    }

    // UNLINK OAUTH
    @Override
    public Map<String, Object> unlinkOAuth(String username, String password, String provider)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            User user = repo.findUserByUserName(username).orElse(null);

            if(user == null)
            {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            // Check if user has OAuth linked
            if(user.getOAuthProvider() == null)
            {
                response.put("success", false);
                response.put("message", "No OAuth account linked");
                return response;
            }

            // SECURITY CHECK: Prevent unlinking if user has no password (OAuth-only account)
            // OAuth-only accounts have password = NULL
            if(user.getPassword() == null || user.getPassword().isEmpty())
            {
                log.warn("Attempted to unlink OAuth from OAuth-only account: {}", user.getUserName());
                response.put("success", false);
                response.put("message", "Cannot unlink OAuth account. This is your only login method. Please set a password first.");
                return response;
            }

            // Verify password before unlinking (must match existing password)
            if(!encoder.matches(password, user.getPassword()))
            {
                response.put("success", false);
                response.put("message", "Incorrect password. Please try again.");
                return response;
            }

            // Unlink OAuth
            String unlinkedProvider = user.getOAuthProvider();
            user.setOAuthProvider(null);
            user.setOAuthId(null);
            repo.save(user);

            log.info("OAuth provider {} unlinked from account: {}", unlinkedProvider, user.getUserName());

            response.put("success", true);
            response.put("message", "OAuth account unlinked successfully. You can still log in with your password.");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred while unlinking OAuth: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();
            exception.put("success", false);
            exception.put("message", "An error occurred. Please try again later.");
            return exception;
        }
    }
}



