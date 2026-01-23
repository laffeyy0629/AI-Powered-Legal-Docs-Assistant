package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OAuthService
{
    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    public OAuthService(UserRepo userRepo, PasswordEncoder encoder, EmailService emailService)
    {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    /**
     * Find existing OAuth user or create new one
     * @param email OAuth user email
     * @param name OAuth user display name
     * @param provider OAuth provider (google, github, etc.)
     * @param providerId OAuth provider user ID
     * @return User entity
     */
    public User findOrCreateOAuthUser(String email, String name, String provider, String providerId)
    {
        System.out.println("=== OAuthService.findOrCreateOAuthUser ===");
        System.out.println("Email: " + email);
        System.out.println("Name: " + name);
        System.out.println("Provider: " + provider);
        System.out.println("ProviderId: " + providerId);

        // Check if user already exists by email
        return userRepo.findUserByEmail(email)
                .map(existingUser -> {
                    System.out.println("Found existing user: " + existingUser.getUserName());

                    // Check if this is a manual account (no OAuth provider set)
                    if (existingUser.getOAuthProvider() == null) {
                        System.out.println("⚠️ Manual account detected. Linking OAuth provider...");

                        // Only link OAuth if email is verified (security check)
                        if (!existingUser.isEmailVerified()) {
                            System.out.println("❌ Email not verified. Cannot link OAuth account.");
                            throw new RuntimeException("Please verify your email before linking OAuth account. Check your inbox for verification link.");
                        }

                        // Link OAuth to existing verified manual account
                        System.out.println("✅ Email verified. Linking " + provider + " to existing account...");
                        existingUser.setOAuthProvider(provider);
                        existingUser.setOAuthId(providerId);
                        existingUser = userRepo.save(existingUser);
                        System.out.println("OAuth linked successfully");

                        // 🔔 FIRST LINK ONLY: Send notification email (not on subsequent logins)
                        try {
                            emailService.sendOAuthLinkedEmail(existingUser.getEmail(), existingUser.getUserName(), provider);
                            System.out.println("✅ OAuth linked notification email sent (first link)");
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to send OAuth linked email: " + e.getMessage());
                            // Don't fail the login if email fails
                        }
                    }
                    // Check if OAuth provider changed (user switching from one OAuth to another)
                    else if (!existingUser.getOAuthProvider().equals(provider)) {
                        System.out.println("⚠️ Switching OAuth provider from " + existingUser.getOAuthProvider() + " to " + provider);
                        String oldProvider = existingUser.getOAuthProvider();
                        existingUser.setOAuthProvider(provider);
                        existingUser.setOAuthId(providerId);
                        existingUser = userRepo.save(existingUser);
                        System.out.println("OAuth provider updated");

                        // 🔔 PROVIDER SWITCH: Send notification email (security alert)
                        try {
                            emailService.sendOAuthLinkedEmail(existingUser.getEmail(), existingUser.getUserName(), provider);
                            System.out.println("✅ OAuth provider switch notification sent");
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to send OAuth switch email: " + e.getMessage());
                        }
                    }
                    // Same provider, just update provider ID if different
                    else if (!existingUser.getOAuthId().equals(providerId)) {
                        System.out.println("Updating OAuth ID for same provider (no email sent - subsequent login)");
                        existingUser.setOAuthId(providerId);
                        existingUser = userRepo.save(existingUser);
                    }
                    else {
                        // Same provider, same ID - regular login, no changes needed
                        System.out.println("✅ Regular OAuth login - no changes needed (no email sent)");
                    }

                    return existingUser;
                })
                .orElseGet(() -> {
                    System.out.println("Creating new OAuth user...");
                    // Create new user for OAuth login
                    User newUser = new User();
                    newUser.setEmail(email);

                    String username = generateUniqueUsername(name, email);
                    System.out.println("Generated username: " + username);
                    newUser.setUserName(username);

                    // OAuth users don't have password - set to NULL (industry standard)
                    // This makes it easy to detect OAuth-only accounts
                    newUser.setPassword(null);
                    newUser.setOAuthProvider(provider);
                    newUser.setOAuthId(providerId);

                    // OAuth users are automatically email verified
                    newUser.setEmailVerified(true);

                    User savedUser = userRepo.save(newUser);
                    System.out.println("New user saved with ID: " + savedUser.getId());
                    return savedUser;
                });
    }

    /**
     * Generate a unique username from name or email
     * @param name Display name from OAuth
     * @param email Email from OAuth
     * @return Unique username
     */
    private String generateUniqueUsername(String name, String email)
    {
        System.out.println("Generating username from - Name: '" + name + "', Email: '" + email + "'");

        // Start with base username from name or email
        String baseUsername;

        if (name != null && !name.isBlank()) {
            // Clean name: remove special chars, lowercase
            String cleaned = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            System.out.println("Cleaned name: '" + cleaned + "'");

            if (cleaned.isEmpty()) {
                // If name has no alphanumeric chars, use email
                cleaned = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            }

            baseUsername = cleaned.substring(0, Math.min(cleaned.length(), 12));
        } else {
            // Use email prefix if name not available
            String emailPrefix = email.split("@")[0];
            String cleaned = emailPrefix.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            baseUsername = cleaned.substring(0, Math.min(cleaned.length(), 12));
        }

        System.out.println("Base username: '" + baseUsername + "'");

        // Ensure minimum length of 8 characters
        if (baseUsername.length() < 8) {
            String suffix = UUID.randomUUID().toString().substring(0, 8 - baseUsername.length());
            baseUsername = baseUsername + suffix;
            System.out.println("Extended to min length: '" + baseUsername + "'");
        }

        // Check if username exists, if so add unique suffix
        String username = baseUsername;
        int counter = 1;

        while (userRepo.findUserByUserName(username).isPresent()) {
            // Add random suffix to make it unique
            String suffix = UUID.randomUUID().toString().substring(0, 4);
            username = baseUsername.substring(0, Math.min(baseUsername.length(), 14)) + suffix;
            counter++;

            // Safety check to prevent infinite loop
            if (counter > 100) {
                username = baseUsername + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }

        return username;
    }

    /**
     * Update existing OAuth user information
     * @param user Existing user
     * @param provider OAuth provider
     * @param providerId OAuth provider user ID
     * @return Updated user
     */
    public User updateOAuthUser(User user, String provider, String providerId)
    {
        // Update OAuth information if changed
        if (user.getOAuthProvider() == null || !user.getOAuthProvider().equals(provider)) {
            user.setOAuthProvider(provider);
        }
        if (user.getOAuthId() == null || !user.getOAuthId().equals(providerId)) {
            user.setOAuthId(providerId);
        }

        return userRepo.save(user);
    }
}

