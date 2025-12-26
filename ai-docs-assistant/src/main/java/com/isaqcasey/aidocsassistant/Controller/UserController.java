package com.isaqcasey.aidocsassistant.Controller;

import com.isaqcasey.aidocsassistant.Model.RefreshToken;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.RefreshTokenService;
import com.isaqcasey.aidocsassistant.Service.TokenBlacklistService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController
{
    private final UserService service;
    private final RefreshTokenService refreshTokenService;
    private final JWTService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration.ms:900000}")
    private long jwtExpirationMs;

    // DEPENDENCY INJECTION
    public UserController(UserService service, RefreshTokenService refreshTokenService,
                          JWTService jwtService, TokenBlacklistService tokenBlacklistService)
    {
        this.service = service;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // DISPLAY SIGNUP PAGE/INFO
    // Yea so nilagay ko lang to para may signup endpoint info pag ginet mo yung /user/signup
    @GetMapping("/user/signup")
    public Map<String, Object> signupPage()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User Signup API");
        response.put("method", "POST");
        response.put("endpoint", "/user/signup");
        response.put("required_fields", Map.of(
            "user_name", "Your username (unique)",
            "email", "Your email address (unique)",
            "password", "Your password (will be encrypted)"
        ));
        response.put("example", Map.of(
            "user_name", "john_doe",
            "email", "john@example.com",
            "password", "securepassword123"
        ));
        return response;
    }

    // USE FOR USER REGISTRATION
    @PostMapping("/user/signup")
    public ResponseEntity<Map<String, Object>> store(@Validated(UserService.OnCreate.class) @RequestBody User user, BindingResult bindingResult)
    {
        Map<String, Object> errors = service.getInputValidationResult(bindingResult);

        if(! errors.get("success").equals(true))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors); // 400 Bad Request

        Map<String, Object> result = service.store(user);

        if(! result.get("success").equals(true)) {
            // Check if it's a conflict (user already exists) or bad request (password too short)
            String message = (String) result.get("message");
            if(message != null && message.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result); // 409 Conflict
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result); // 400 Bad Request
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result); // 201 Created
    }

    // VALIDATE USER CREDENTIALS
    @PostMapping("/user/login")
    public ResponseEntity<Map<String, Object>> login(@Validated(UserService.OnLogin.class) @RequestBody User user, BindingResult bindingResult)
    {
        Map<String, Object> errors = service.getInputValidationResult(bindingResult);

        if(! errors.get("success").equals(true))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors); // 400 Bad Request

        Map<String, Object> result = service.login(user);

        if(! result.get("success").equals(true)) {
            // Check if it's authentication failure (invalid credentials)
            String message = (String) result.get("message");
            if(message != null && (message.contains("not found") || message.contains("Incorrect password"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result); // 401 Unauthorized
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result); // 500 Internal Server Error
        }

        return ResponseEntity.ok(result); // 200 OK
    }

    // AUTHENTICATED USER PURPOSES
    @GetMapping("/jwt-try")
    public ResponseEntity<Map<String, Object>> secure()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "JWT works");
        return ResponseEntity.ok(response); // 200 OK
    }

    // EMAIL VERIFICATION
    @GetMapping("/user/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token)
    {
        Map<String, Object> result = service.verifyEmail(token);

        if(! result.get("success").equals(true)) {
            String message = (String) result.get("message");
            if(message != null && message.contains("expired")) {
                return ResponseEntity.status(HttpStatus.GONE).body(result); // 410 Gone
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result); // 400 Bad Request
        }

        return ResponseEntity.ok(result); // 200 OK
    }

    // RESEND VERIFICATION EMAIL
    @PostMapping("/user/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestBody Map<String, String> request)
    {
        String email = request.get("email");

        if(email == null || email.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> result = service.resendVerificationEmail(email);

        if(! result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result); // 400 Bad Request
        }

        return ResponseEntity.ok(result); // 200 OK
    }

    // REFRESH TOKEN - Get new access token using refresh token
    @PostMapping("/user/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request)
    {
        try {
            String refreshToken = request.get("refresh_token");

            if(refreshToken == null || refreshToken.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Refresh token is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Rotate refresh token (get new refresh token and revoke old one)
            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

            // Generate new access token
            String newAccessToken = jwtService.generateToken(newRefreshToken.getUser().getUserName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token refreshed successfully");
            response.put("access_token", newAccessToken);
            response.put("refresh_token", newRefreshToken.getToken());
            response.put("token_type", "Bearer");
            response.put("expires_in", jwtExpirationMs / 1000); // Convert to seconds

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while refreshing token");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // LOGOUT - Revoke refresh token and invalidate access token
    @PostMapping("/user/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> request)
    {
        try {
            String refreshToken = request.get("refresh_token");
            String accessToken = request.get("access_token");

            System.out.println("=== Logout Request Received ===");
            System.out.println("Refresh Token: " + (refreshToken != null ? "Present" : "Null"));
            System.out.println("Access Token: " + (accessToken != null ? "Present" : "Null"));

            // Revoke refresh token if provided
            if(refreshToken != null && !refreshToken.trim().isEmpty()) {
                System.out.println("Revoking refresh token...");
                refreshTokenService.revokeRefreshToken(refreshToken);
                System.out.println("Refresh token revoked successfully");
            }

            // Invalidate access token (add to blacklist) if provided
            if(accessToken != null && !accessToken.trim().isEmpty()) {
                System.out.println("Invalidating access token (adding to blacklist)...");
                tokenBlacklistService.invalidateToken(accessToken);
                System.out.println("Access token added to blacklist successfully");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");
            System.out.println("Logout completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred during logout");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // FORGOT PASSWORD - Send password reset email
    @PostMapping("/user/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request)
    {
        String email = request.get("email");

        if(email == null || email.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> result = service.forgotPassword(email);

        if(! result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    // RESET PASSWORD - Reset password with token
    @PostMapping("/user/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request)
    {
        String token = request.get("token");
        String newPassword = request.get("new_password");

        if(token == null || token.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Reset token is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if(newPassword == null || newPassword.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "New password is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> result = service.resetPassword(token, newPassword);

        if(! result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    // FORGOT USERNAME - Send username reminder email
    @PostMapping("/user/forgot-username")
    public ResponseEntity<Map<String, Object>> forgotUsername(@RequestBody Map<String, String> request)
    {
        String email = request.get("email");

        if(email == null || email.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> result = service.forgotUsername(email);

        if(! result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    // GET USER PROFILE - For account settings page
    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile()
    {
        try {
            // Get username from SecurityContext (works with JWT)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = authentication.getName();

            if (username == null || username.equals("anonymousUser")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Map<String, Object> result = service.getUserProfile(username);

            if(! result.get("success").equals(true)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // OAUTH LINK INFO - Get account info for consent page
    @PostMapping("/user/oauth-link-info")
    public ResponseEntity<Map<String, Object>> getOAuthLinkInfo(@RequestBody Map<String, String> request)
    {
        String email = request.get("email");
        String sessionId = request.get("session_id");

        if(email == null || sessionId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Missing required parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> result = service.getOAuthLinkInfo(email, sessionId);

        if(! result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    // CONFIRM OAUTH LINK - User explicitly consents to link OAuth
    @PostMapping("/user/confirm-oauth-link")
    public ResponseEntity<Map<String, Object>> confirmOAuthLink(@RequestBody Map<String, Object> request)
    {
        String email = (String) request.get("email");
        String provider = (String) request.get("provider");
        String sessionId = (String) request.get("session_id");
        Boolean consent = (Boolean) request.get("consent");

        if(email == null || provider == null || sessionId == null || consent == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Missing required parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if(!consent) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "User declined to link OAuth account");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> result = service.confirmOAuthLink(email, provider, sessionId);

        if(! result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    // UNLINK OAUTH - Remove OAuth provider from account
    @PostMapping("/user/unlink-oauth")
    public ResponseEntity<Map<String, Object>> unlinkOAuth(@RequestBody Map<String, String> request)
    {
        try {
            // Get username from SecurityContext (works with JWT)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = authentication.getName();

            if (username == null || username.equals("anonymousUser")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String password = request.get("password");
            String provider = request.get("provider");

            if(password == null || password.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Password is required to confirm unlinking");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Map<String, Object> result = service.unlinkOAuth(username, password, provider);

            if(! result.get("success").equals(true)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
