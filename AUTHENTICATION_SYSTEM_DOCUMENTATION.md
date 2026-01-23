# Authentication System - Code Flow Documentation

## Table of Contents
1. [Manual Registration Flow](#manual-registration-flow)
2. [OAuth Registration Flow](#oauth-registration-flow)
3. [Manual Login Flow](#manual-login-flow)
4. [OAuth Login Flow](#oauth-login-flow)
5. [Linking OAuth to Existing Manual Account](#linking-oauth-to-existing-manual-account)
6. [Unlinking OAuth Account](#unlinking-oauth-account)
7. [Email Verification Flow](#email-verification-flow)
8. [Forgot Password Flow](#forgot-password-flow)
9. [Forgot Username Flow](#forgot-username-flow)
10. [Logout Flow](#logout-flow)
11. [Token Refresh Flow](#token-refresh-flow)
12. [JWT Authentication Filter](#jwt-authentication-filter)

---

## Manual Registration Flow

### Entry Point
**Frontend:** `Login.jsx` → `handleSignup()`
**Backend:** `UserController.java` → `POST /user/signup`

### Code Flow

```
Client
  ↓
POST /user/signup
{
  user_name: "testuser123",
  email: "test@example.com", 
  password: "password123"
}
  ↓
UserController.store(User user)
  ↓
Input validation (@Validated annotation)
  - Username: 8-20 characters
  - Email: Valid email format
  - Password: Min 8 characters
  ↓
UserImpl.store(User user)
  ↓
Check username exists?
  → YES: Return error "Username already exists"
  → NO: Continue
  ↓
Check email exists?
  → YES: Return error "Email already registered"
  → NO: Continue
  ↓
Generate verification token
  - plainToken = UUID.randomUUID()
  - hashedToken = SHA-256(plainToken)
  ↓
Create user entity
  - userName = username
  - email = email
  - password = BCrypt.hash(password)
  - emailVerified = false
  - verificationToken = hashedToken
  - verificationTokenExpiry = now + 24 hours
  ↓
Save to database
  ↓
Send verification email
  - EmailService.sendVerificationEmail()
  - Email contains: plainToken (not hashed)
  - Link: http://frontend/verify-email?token=plainToken
  ↓
Return response
{
  success: true,
  message: "Registration successful! Please verify your email.",
  user: { username, email, emailVerified: false }
}
```

### Key Classes
- **Controller:** `UserController.java` - HTTP endpoint handler
- **Service:** `UserImpl.java` - Business logic implementation
- **Repository:** `UserRepo.java` - Database operations
- **Email:** `EmailService.java` - Email sending
- **Security:** `TokenHashingService.java` - Token hashing (SHA-256)

### Security Features
- Password hashed with BCrypt (cost factor: 10)
- Verification token hashed with SHA-256 before database storage
- Plain token sent in email (one-time use)
- Token expires after 24 hours

---

## OAuth Registration Flow

### Entry Point
**Frontend:** `Login.jsx` → Click "Login with Google"
**Backend:** `SecurityConfig.java` → OAuth2 flow

### Code Flow

```
Client clicks "Login with Google"
  ↓
Redirected to Google OAuth consent screen
  ↓
User grants permission
  ↓
Google redirects back with authorization code
  ↓
Spring Security exchanges code for access token
  ↓
SecurityConfig.successHandler()
  ↓
Extract OAuth user info
  - email: user.getAttribute("email")
  - name: user.getAttribute("name")
  - providerId: user.getAttribute("sub")
  - provider: "google"
  ↓
OAuthService.findOrCreateOAuthUser()
  ↓
Check if user exists by email?
  ↓
NO: Create new OAuth user
  ↓
Generate unique username
  - Base: sanitize(name) or email prefix
  - Check uniqueness in database
  - Add random suffix if duplicate
  ↓
Create user entity
  - userName = generated username
  - email = OAuth email
  - password = NULL (OAuth-only account)
  - emailVerified = true (auto-verified)
  - oAuthProvider = "google"
  - oAuthId = providerId
  ↓
Save to database
  ↓
Generate JWT tokens
  - accessToken = JWT.create(username, exp: 15 min)
  - refreshToken = UUID.randomUUID()
  - Store hashed refreshToken in database
  ↓
Redirect to frontend callback
  - URL: /auth/callback?token=accessToken
  ↓
Frontend (OAuthCallback.jsx)
  - Extract token from URL
  - Store in localStorage
  - Store refreshToken
  - Redirect to dashboard
```

### Key Classes
- **Config:** `SecurityConfig.java` - OAuth2 configuration
- **Service:** `OAuthService.java` - OAuth user creation
- **Service:** `JWTService.java` - JWT token generation
- **Service:** `RefreshTokenService.java` - Refresh token management

### Security Features
- OAuth users have NULL password (cannot login with password)
- Email auto-verified (trusted OAuth provider)
- Refresh tokens hashed with SHA-256 in database
- JWT includes type claim ("access" or "refresh")

---

## Manual Login Flow

### Entry Point
**Frontend:** `Login.jsx` → `handleLogin()`
**Backend:** `UserController.java` → `POST /user/login`

### Code Flow

```
Client
  ↓
POST /user/login
{
  user_name: "testuser123",
  password: "password123"
}
  ↓
UserController.login(User user)
  ↓
Input validation
  ↓
UserImpl.login(User user)
  ↓
Find user by username
  → NOT FOUND: Return error
  ↓
Check email verified?
  → NO: Return error "Please verify your email"
  → YES: Continue
  ↓
Verify password
  - BCrypt.matches(inputPassword, storedHash)
  → NO MATCH: Return error "Invalid credentials"
  → MATCH: Continue
  ↓
Revoke all existing refresh tokens
  - Set revoked = true for user's tokens
  - Enforces single active session
  ↓
Generate new tokens
  - accessToken = JWT.create(username, exp: 15 min)
  - refreshToken = UUID.randomUUID()
  ↓
Hash and store refresh token
  - hashedToken = SHA-256(refreshToken)
  - Store in refresh_tokens table
  - expiresAt = now + 7 days
  ↓
Return response
{
  success: true,
  access_token: "eyJhbGc...",
  refresh_token: "uuid-plain-token",
  token_type: "Bearer",
  expires_in: 900
}
  ↓
Frontend stores tokens
  - localStorage.setItem('access_token', token)
  - localStorage.setItem('refresh_token', refreshToken)
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `UserImpl.java`
- **Service:** `JWTService.java`
- **Service:** `RefreshTokenService.java`
- **Encoder:** Spring Security `PasswordEncoder`

### Security Features
- Email verification required before login
- BCrypt password verification (slow hash, prevents brute force)
- Single active session per user (old refresh tokens revoked)
- Refresh tokens hashed in database
- Access token expires in 15 minutes
- Refresh token expires in 7 days

---

## OAuth Login Flow

### Entry Point
**Frontend:** `Login.jsx` → Click "Login with Google"
**Backend:** `SecurityConfig.java` → OAuth2 flow

### Code Flow

```
Client clicks "Login with Google"
  ↓
OAuth consent screen (if not already authorized)
  ↓
Google redirects back with code
  ↓
SecurityConfig.successHandler()
  ↓
OAuthService.findOrCreateOAuthUser()
  ↓
Find user by email
  ↓
YES: User exists
  ↓
Check OAuth provider status
  ↓
CASE 1: User has NO OAuth linked (manual account)
  ↓
  Check email verified?
    → NO: Throw error "Please verify email first"
    → YES: Continue
  ↓
  Link OAuth to account
    - oAuthProvider = "google"
    - oAuthId = providerId
    - Save to database
  ↓
  Send notification email
    - "Google account linked"
    - Lists both login methods
    - Security warning
  ↓
CASE 2: User has SAME OAuth provider
  ↓
  Update OAuth ID if changed
  - No email sent (regular login)
  ↓
CASE 3: User has DIFFERENT OAuth provider
  ↓
  Switch provider
    - oAuthProvider = newProvider
    - oAuthId = newProviderId
  ↓
  Send notification email (security alert)
  ↓
Generate JWT tokens
  ↓
Redirect to /auth/callback?token=accessToken
```

### Key Classes
- **Config:** `SecurityConfig.java`
- **Service:** `OAuthService.java`
- **Service:** `EmailService.java`
- **Service:** `JWTService.java`

### Security Features
- Email verification required before OAuth linking
- Notification email sent on first link (not subsequent logins)
- Notification email sent on provider switch (security alert)
- No email spam on regular logins

---

## Linking OAuth to Existing Manual Account

### Entry Point
**Frontend:** Manual registration → OAuth login attempt
**Backend:** `OAuthService.java` → Auto-detection

### Code Flow

```
User has manual account (email verified)
  ↓
User clicks "Login with Google"
  ↓
OAuth flow completes
  ↓
OAuthService.findOrCreateOAuthUser()
  ↓
Find user by email: FOUND
  ↓
Check: oAuthProvider == null?
  → YES: Manual account detected
  ↓
Security check: email verified?
  → NO: Throw error
  → YES: Proceed
  ↓
Link OAuth to account
  - user.setOAuthProvider("google")
  - user.setOAuthId(providerId)
  - userRepo.save(user)
  ↓
Send notification email
  - Subject: "Google Account Linked"
  - Body: Explains both login methods now available
  - Includes security warning
  - Link to dashboard
  ↓
Generate tokens and login
  ↓
User can now login with:
  - Username + Password (manual)
  - Google OAuth (linked)
```

### Database Changes

**Before:**
```sql
username: "testuser123"
email: "test@example.com"
password: "$2a$10$hash..."
o_auth_provider: NULL
o_auth_id: NULL
```

**After:**
```sql
username: "testuser123"
email: "test@example.com"
password: "$2a$10$hash..."  -- Still works!
o_auth_provider: "google"
o_auth_id: "109239677934351440957"
```

### Key Classes
- **Service:** `OAuthService.java` - Auto-linking logic
- **Service:** `EmailService.java` - Notification email
- **Repository:** `UserRepo.java`

### Security Features
- Email must be verified before linking
- Notification email sent immediately
- Password remains functional (dual authentication)
- Only one OAuth provider per account at a time

---

## Unlinking OAuth Account

### Entry Point
**Frontend:** `AccountSettings.jsx` → Unlink button
**Backend:** `UserController.java` → `POST /user/unlink-oauth`

### Code Flow

```
User navigates to Settings page
  ↓
Frontend calls GET /user/profile
  ↓
Backend returns user info
{
  username: "testuser",
  email: "test@example.com",
  oAuthProvider: "google",
  hasPassword: true/false  -- Key field!
}
  ↓
Frontend checks hasPassword
  ↓
hasPassword == false?
  → YES: Disable unlink button
  → Show warning: "This is your only login method"
  → NO: Enable unlink button
  ↓
User clicks "Unlink"
  ↓
Modal appears requesting password
  ↓
User enters password
  ↓
POST /user/unlink-oauth
{
  password: "user_password",
  provider: "google"
}
  ↓
UserController.unlinkOAuth()
  ↓
Get username from SecurityContext (JWT)
  ↓
UserImpl.unlinkOAuth(username, password, provider)
  ↓
Find user by username
  ↓
Check: oAuthProvider != null?
  → NO: Return error "No OAuth linked"
  ↓
Security check: password != null?
  → NO: Return error "Cannot unlink - only login method"
  → YES: Continue
  ↓
Verify password matches
  - BCrypt.matches(inputPassword, storedHash)
  → NO MATCH: Return error "Incorrect password"
  → MATCH: Continue
  ↓
Unlink OAuth
  - user.setOAuthProvider(null)
  - user.setOAuthId(null)
  - userRepo.save(user)
  ↓
Log event
  - "OAuth provider {provider} unlinked from {username}"
  ↓
Return success
{
  success: true,
  message: "OAuth unlinked. You can still login with password."
}
```

### Frontend Logic

```javascript
// AccountSettings.jsx
const isOAuthOnly = () => {
  return userInfo?.oAuthProvider && !userInfo?.hasPassword;
};

// Unlink button
<button
  onClick={() => setShowUnlinkConfirm(true)}
  disabled={isOAuthOnly()}
  title={isOAuthOnly() ? "Cannot unlink - only login method" : "Unlink OAuth"}
>
  Unlink
</button>

// Warning banner (if OAuth-only)
{isOAuthOnly() && (
  <div className="warning">
    Cannot unlink OAuth: This is your only login method.
    Please set a password first.
  </div>
)}
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `UserImpl.java`
- **Frontend:** `AccountSettings.jsx`

### Security Features
- Password verification required
- Cannot unlink if no password exists (prevents lockout)
- Frontend disables button for OAuth-only accounts
- Backend validates password and auth method
- User can still login with password after unlinking

---

## Email Verification Flow

### Entry Point
**Email:** User clicks verification link
**Frontend:** `EmailVerification.jsx`
**Backend:** `UserController.java` → `POST /user/verify-email`

### Code Flow

```
User receives verification email
  ↓
Email contains link:
  - http://frontend/verify-email?token=plain-uuid-token
  ↓
User clicks link
  ↓
Frontend (EmailVerification.jsx)
  - Extract token from URL params
  - Automatically call API
  ↓
POST /user/verify-email
{
  token: "plain-uuid-token"
}
  ↓
UserController.verifyEmail(token)
  ↓
UserImpl.verifyEmail(token)
  ↓
Hash the incoming token
  - hashedToken = SHA-256(plainToken)
  ↓
Find user by hashed verification token
  - userRepo.findByVerificationToken(hashedToken)
  ↓
User found?
  → NO: Return error "Invalid or expired token"
  → YES: Continue
  ↓
Check token expiration
  - tokenExpiry < now?
  → YES: Expired
    - Clear token from database
    - Return error "Token expired"
  → NO: Valid, continue
  ↓
Mark email as verified
  - user.setEmailVerified(true)
  - user.setVerificationToken(null)
  - user.setVerificationTokenExpiry(null)
  - userRepo.save(user)
  ↓
Log success
  - "Email verified for user: {username}"
  ↓
Return response
{
  success: true,
  message: "Email verified successfully!",
  user: { username, email, emailVerified: true }
}
  ↓
Frontend redirects to login page
```

### Resend Verification Flow

```
User requests resend
  ↓
POST /user/resend-verification
{
  email: "test@example.com"
}
  ↓
Find user by email
  ↓
Check already verified?
  → YES: Return "Email already verified"
  → NO: Continue
  ↓
Generate new token
  - plainToken = UUID.randomUUID()
  - hashedToken = SHA-256(plainToken)
  ↓
Update user
  - verificationToken = hashedToken
  - verificationTokenExpiry = now + 24 hours
  ↓
Send verification email
  - Contains new plainToken
  ↓
Return success
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `UserImpl.java`
- **Service:** `EmailService.java`
- **Service:** `TokenHashingService.java`

### Security Features
- Tokens hashed with SHA-256 in database
- Plain token sent in email (one-time use)
- Token expires after 24 hours
- Token cleared after successful verification
- Cannot verify already-verified email

---

## Forgot Password Flow

### Entry Point
**Frontend:** `ForgotPassword.jsx`
**Backend:** `UserController.java` → `POST /user/forgot-password`

### Code Flow

```
User clicks "Forgot Password"
  ↓
Frontend shows form
  ↓
POST /user/forgot-password
{
  email: "test@example.com"
}
  ↓
UserController.forgotPassword(email)
  ↓
UserImpl.forgotPassword(email)
  ↓
Find user by email
  → NOT FOUND: Return generic success (security)
  → FOUND: Continue
  ↓
Check if OAuth-only account
  - user.oAuthProvider != null && user.password == null?
  → YES: Return error "Use {provider} to sign in"
  → NO: Continue
  ↓
Generate reset token
  - plainToken = UUID.randomUUID()
  - hashedToken = SHA-256(plainToken)
  - tokenExpiry = now + 1 hour
  ↓
Update user
  - user.setPasswordResetToken(hashedToken)
  - user.setPasswordResetTokenExpiry(tokenExpiry)
  - userRepo.save(user)
  ↓
Send password reset email
  - EmailService.sendPasswordResetEmail()
  - Email contains plainToken
  - Link: /reset-password?token=plainToken
  ↓
Return generic success
{
  success: true,
  message: "If account exists, reset link sent."
}
  ↓
User receives email
  ↓
User clicks link
  ↓
Frontend (ResetPassword.jsx)
  - Extract token from URL
  - Show new password form
  ↓
POST /user/reset-password
{
  token: "plain-token",
  newPassword: "newPassword123"
}
  ↓
UserImpl.resetPassword(token, newPassword)
  ↓
Hash incoming token
  - hashedToken = SHA-256(plainToken)
  ↓
Find user by hashed reset token
  → NOT FOUND: Return error "Invalid token"
  ↓
Check token expiration
  → EXPIRED: Clear token, return error
  → VALID: Continue
  ↓
Update password and clear token
  - user.setPassword(BCrypt.hash(newPassword))
  - user.setPasswordResetToken(null)
  - user.setPasswordResetTokenExpiry(null)
  - userRepo.save(user)
  ↓
Log success
  ↓
Return success
{
  success: true,
  message: "Password reset successfully!"
}
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `UserImpl.java`
- **Service:** `EmailService.java`
- **Service:** `TokenHashingService.java`
- **Frontend:** `ForgotPassword.jsx`, `ResetPassword.jsx`

### Security Features
- Generic success message (don't reveal if email exists)
- OAuth-only accounts cannot reset password
- Token hashed with SHA-256 in database
- Token expires after 1 hour
- One-time use (cleared after successful reset)
- New password hashed with BCrypt

---

## Forgot Username Flow

### Entry Point
**Frontend:** `ForgotUsername.jsx`
**Backend:** `UserController.java` → `POST /user/forgot-username`

### Code Flow

```
User clicks "Forgot Username"
  ↓
Frontend shows email form
  ↓
POST /user/forgot-username
{
  email: "test@example.com"
}
  ↓
UserController.forgotUsername(email)
  ↓
UserImpl.forgotUsername(email)
  ↓
Find user by email
  → NOT FOUND: Return generic success (security)
  → FOUND: Continue
  ↓
Send username reminder email
  - EmailService.sendUsernameReminderEmail()
  - Email contains username directly
  - No token needed (just information)
  ↓
Return generic success
{
  success: true,
  message: "If account exists, username sent."
}
```

### Key Difference from Password Reset

**No token system needed:**
- Username is not sensitive like password
- Email goes to verified email address
- Just informational (no action required)
- Simpler and more secure (fewer tokens)

### Email Template

```html
Subject: Username Reminder

Hello,

Your username is: testuser123

You can login at: http://app.com/login

If you didn't request this, ignore this email.
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `UserImpl.java`
- **Service:** `EmailService.java`

### Security Features
- Generic success message (don't reveal if email exists)
- No token needed (just sends username)
- Email must be on file (verified email address)
- Simple and secure

---

## Logout Flow

### Entry Point
**Frontend:** `Dashboard.jsx` → Logout button
**Backend:** `UserController.java` → `POST /user/logout`

### Code Flow

```
User clicks "Logout" button
  ↓
Frontend gathers tokens
  - accessToken = localStorage.getItem('access_token')
  - refreshToken = localStorage.getItem('refresh_token')
  ↓
POST /user/logout
{
  access_token: "eyJhbGc...",
  refresh_token: "uuid-token"
}
  ↓
UserController.logout()
  ↓
Process refresh token
  ↓
RefreshTokenService.revokeToken(plainToken)
  ↓
Hash the plain token
  - hashedToken = SHA-256(plainToken)
  ↓
Find refresh token by hash
  ↓
Found?
  → YES: Mark as revoked
    - token.setRevoked(true)
    - token.setRevokedAt(now)
    - refreshTokenRepo.save(token)
  ↓
Process access token (blacklist)
  ↓
TokenBlacklistService.invalidateToken(accessToken)
  ↓
Hash the JWT token
  - hashedToken = SHA-256(accessToken)
  ↓
Add to blacklist
  - Create InvalidatedToken entity
  - token = hashedToken
  - expiresAt = JWT expiration time
  - invalidatedAt = now
  - userName = extracted from JWT
  - Save to invalidated_tokens table
  ↓
Return success
{
  success: true,
  message: "Logged out successfully"
}
  ↓
Frontend clears tokens
  - localStorage.removeItem('access_token')
  - localStorage.removeItem('refresh_token')
  - Redirect to home page
```

### Token Invalidation Details

**Refresh Token:**
- Stored in `refresh_tokens` table
- Marked as revoked (soft delete)
- Can track revocation history
- Cannot be reused

**Access Token:**
- Added to `invalidated_tokens` table
- JWT still valid until expiration
- Blacklist checked on every request
- Auto-cleaned after expiration

### JWT Filter Check

```java
// JWTFilter.java
protected void doFilterInternal(request, response, filterChain) {
  String token = extractToken(request);
  
  // Check if token is blacklisted
  if (tokenBlacklistService.isTokenInvalidated(token)) {
    // Reject request
    response.setStatus(401);
    return;
  }
  
  // Validate and authenticate
  String username = jwtService.extractUsername(token);
  // Set authentication in SecurityContext
  filterChain.doFilter(request, response);
}
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `RefreshTokenService.java`
- **Service:** `TokenBlacklistService.java`
- **Service:** `JWTService.java`

### Security Features
- Both access and refresh tokens invalidated
- Refresh token marked as revoked in database
- Access token added to blacklist
- Hashed tokens stored in database
- Cannot reuse invalidated tokens
- Immediate effect (no grace period)

---

## Token Refresh Flow

### Entry Point
**Frontend:** `api.js` → Automatic on 401 error
**Backend:** `UserController.java` → `POST /user/refresh`

### Code Flow

```
Client makes authenticated request
  ↓
Access token expired
  ↓
Server returns 401 Unauthorized
  ↓
Frontend (api.js) intercepts error
  ↓
Automatic refresh attempt
  ↓
POST /user/refresh
{
  refresh_token: "plain-uuid-token"
}
  ↓
UserController.refreshToken(token)
  ↓
RefreshTokenService.rotateRefreshToken(plainToken)
  ↓
Hash incoming token
  - hashedToken = SHA-256(plainToken)
  ↓
Find refresh token by hash
  → NOT FOUND: Return error
  ↓
Validate refresh token
  ↓
Check expired?
  → YES: Return error "Token expired"
  ↓
Check revoked?
  → YES: Security breach detected!
    - Revoke ALL user tokens
    - Return error "Token reuse detected"
  ↓
Token is valid, rotate it
  ↓
Generate new refresh token
  - newPlainToken = UUID.randomUUID()
  - newHashedToken = SHA-256(newPlainToken)
  ↓
Create new refresh token entity
  - token = newHashedToken
  - expiresAt = now + 7 days
  - user = oldToken.user
  - Save to database
  ↓
Revoke old token
  - oldToken.setRevoked(true)
  - oldToken.setRevokedAt(now)
  - oldToken.setReplacedByToken(newHashedToken)
  - Save to database
  ↓
Flush and detach (prevent auto-update)
  - entityManager.flush()
  - entityManager.detach(newRefreshToken)
  ↓
Set plain token for client
  - newRefreshToken.setToken(newPlainToken)
  ↓
Generate new access token
  - accessToken = JWT.create(username, exp: 15 min)
  ↓
Return new tokens
{
  access_token: "new-jwt",
  refresh_token: "new-plain-uuid",
  token_type: "Bearer",
  expires_in: 900
}
  ↓
Frontend stores new tokens
  ↓
Retry original request with new access token
```

### Refresh Token Rotation

**Why rotate:**
- Enhanced security (stolen tokens expire)
- Detects token reuse (security breach)
- Limits token lifetime

**Database tracking:**
```sql
-- Old token
revoked: true
revoked_at: 2025-12-24 10:00:00
replaced_by_token: "hash-of-new-token"

-- New token
revoked: false
created_at: 2025-12-24 10:00:00
expires_at: 2025-12-31 10:00:00
```

### Key Classes
- **Controller:** `UserController.java`
- **Service:** `RefreshTokenService.java`
- **Service:** `JWTService.java`
- **Repository:** `RefreshTokenRepo.java`

### Security Features
- Refresh token rotation (new token every refresh)
- Old token immediately revoked
- Detects token reuse (revokes all user tokens)
- Tokens hashed in database
- 7-day refresh token lifetime
- 15-minute access token lifetime

---

## JWT Authentication Filter

### Entry Point
Every HTTP request passes through this filter
**Class:** `JWTFilter.java`

### Code Flow

```
HTTP Request arrives
  ↓
JWTFilter.doFilterInternal()
  ↓
Extract Authorization header
  - header = request.getHeader("Authorization")
  ↓
Header present and starts with "Bearer "?
  → NO: Skip filter, continue chain
  → YES: Extract token
  ↓
token = header.substring(7)  // Remove "Bearer "
  ↓
Check if token is blacklisted
  ↓
TokenBlacklistService.isTokenInvalidated(token)
  ↓
Hash token and check database
  - hashedToken = SHA-256(token)
  - query: SELECT FROM invalidated_tokens WHERE token = hashedToken
  ↓
Token blacklisted?
  → YES: Reject request (401)
  → NO: Continue
  ↓
Validate JWT token
  ↓
JWTService.validateToken(token)
  - Verify signature (HMAC-SHA256)
  - Check expiration
  - Check type claim == "access"
  ↓
Valid?
  → NO: Continue chain (no auth set)
  → YES: Extract username
  ↓
username = JWTService.extractUsername(token)
  ↓
Create authentication token
  - authToken = new UsernamePasswordAuthenticationToken(
      username, null, authorities
    )
  ↓
Set in SecurityContext
  - SecurityContextHolder.getContext().setAuthentication(authToken)
  ↓
Log authentication
  - "Authentication set for: {username}"
  ↓
Continue filter chain
  - filterChain.doFilter(request, response)
```

### Token Validation Details

```java
// JWTService.java
public boolean validateToken(String token) {
  try {
    Jwts.parserBuilder()
      .setSigningKey(secretKey)
      .build()
      .parseClaimsJws(token);
    
    // Check type claim
    String type = extractClaim(token, claims -> claims.get("type"));
    if (!"access".equals(type)) {
      return false;
    }
    
    return true;
  } catch (ExpiredJwtException e) {
    return false;
  } catch (Exception e) {
    return false;
  }
}
```

### Protected Endpoints

**Public (no JWT required):**
```
/user/signup
/user/login
/user/verify-email
/user/forgot-password
/user/reset-password
/auth/**
/oauth2/**
```

**Protected (JWT required):**
```
/user/profile
/user/unlink-oauth
/user/logout
/jwt-try
/dashboard
```

### Key Classes
- **Filter:** `JWTFilter.java` - Main authentication filter
- **Service:** `JWTService.java` - JWT operations
- **Service:** `TokenBlacklistService.java` - Blacklist check
- **Config:** `SecurityConfig.java` - Security configuration

### Security Features
- Every request authenticated
- JWT signature verification (HMAC-SHA256)
- Token expiration checked
- Blacklist checked (logout enforcement)
- Type claim validation (access vs refresh)
- SecurityContext populated for downstream use

---

## Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY,
  user_name VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255),  -- NULL for OAuth-only accounts
  email_verified BOOLEAN DEFAULT FALSE,
  
  -- OAuth fields
  o_auth_provider VARCHAR(50),
  o_auth_id VARCHAR(255),
  
  -- Email verification
  verification_token VARCHAR(44),  -- SHA-256 hash (Base64)
  verification_token_expiry TIMESTAMP,
  
  -- Password reset
  password_reset_token VARCHAR(44),  -- SHA-256 hash (Base64)
  password_reset_token_expiry TIMESTAMP,
  
  -- Timestamps
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Refresh Tokens Table
```sql
CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(44) NOT NULL,  -- SHA-256 hash (Base64)
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN DEFAULT FALSE,
  revoked_at TIMESTAMP,
  replaced_by_token VARCHAR(44),  -- SHA-256 hash of new token
  created_at TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Invalidated Tokens Table
```sql
CREATE TABLE invalidated_tokens (
  id BIGINT PRIMARY KEY,
  token VARCHAR(44) NOT NULL,  -- SHA-256 hash (Base64) of JWT
  user_name VARCHAR(255) NOT NULL,
  invalidated_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL
);
```

---

## Token Security Summary

### JWT Token Lifetimes

**Access Token (JWT): 15 minutes**
- Short-lived for security
- Contains user authentication
- Stored client-side only (localStorage)
- Expires quickly to limit exposure if stolen
- Automatically refreshed using refresh token

**Why 15 minutes?**
- Industry standard (OWASP recommendation)
- Balances security vs user experience
- If token is stolen, attacker has limited time window
- Frequent rotation reduces risk
- Used by: Google, GitHub, Auth0, AWS

**Refresh Token: 7 days**
- Longer-lived for convenience
- Used only to get new access tokens
- Stored client-side + database (hashed)
- Rotates on every use (new token generated)
- Revoked on logout or security breach

**Why 7 days?**
- Industry standard for web applications
- Users don't need to login every 15 minutes
- Long enough for convenience
- Short enough for security
- Can be revoked immediately if compromised

**Token Lifecycle:**
```
Day 0: Login
  - Access token: Valid for 15 min
  - Refresh token: Valid for 7 days
  ↓
Minute 15: Access token expires
  - Frontend auto-refreshes
  - New access token: Valid for 15 min
  - New refresh token: Valid for 7 days (old one revoked)
  ↓
Repeat every 15 minutes...
  ↓
Day 7: Refresh token expires
  - User must login again
```

**Configuration:**
```yaml
# application.yml
jwt:
  expiration:
    ms: 900000  # 15 minutes = 900000 milliseconds
  refresh:
    expiration:
      days: 7  # 7 days

# .env
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_DAYS=7
```

**Alternative Configurations (by use case):**

| Use Case | Access Token | Refresh Token | Notes |
|----------|--------------|---------------|-------|
| **High Security** | 5 min | 1 day | Banking, healthcare |
| **Standard (Recommended)** | 15 min | 7 days | Most web apps |
| **Convenience** | 30 min | 30 days | Internal tools |
| **Mobile Apps** | 1 hour | 90 days | Less frequent refresh |

**To Change Token Duration:**
1. Edit `.env` file in `ai-docs-assistant/` directory
2. Change `JWT_EXPIRATION_MS` value (in milliseconds)
3. Restart backend server
4. All new logins use new duration

**Common Durations in Milliseconds:**
- 5 minutes = 300000
- 15 minutes = 900000 (recommended)
- 30 minutes = 1800000
- 1 hour = 3600000
- 24 hours = 86400000 (NOT recommended for access tokens)

### Token Types and Storage

| Token Type | Storage Location | Format | Lifetime |
|------------|-----------------|---------|----------|
| Access Token (JWT) | Client only | JWT (signed) | 15 minutes |
| Refresh Token | Client + Database (hashed) | UUID | 7 days |
| Verification Token | Database (hashed) | UUID | 24 hours |
| Password Reset Token | Database (hashed) | UUID | 1 hour |

### Hashing Implementation

**Algorithm:** SHA-256 + Base64 encoding

```java
// TokenHashingService.java
public String hashToken(String plainToken) {
  MessageDigest digest = MessageDigest.getInstance("SHA-256");
  byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
  return Base64.getEncoder().encodeToString(hash);
}
```

**Why SHA-256:**
- One-way hash (cannot reverse)
- Fast computation
- 256-bit security (44 chars in Base64)
- Industry standard

**What gets hashed:**
- Refresh tokens
- Email verification tokens
- Password reset tokens
- Blacklisted JWT tokens

**What doesn't get hashed:**
- Passwords (use BCrypt instead)
- JWT tokens (signed, not hashed)
- OAuth provider IDs

### Security Properties

**Refresh Tokens:**
- Plain token: Client only (localStorage)
- Hashed token: Database only
- Cannot steal from database (hash is useless without plain)
- Rotation on every use (limits exposure)

**Email Verification:**
- Plain token: Email link
- Hashed token: Database
- One-time use (cleared after verification)
- Time-limited (24 hours)

**Password Reset:**
- Plain token: Email link
- Hashed token: Database
- One-time use (cleared after reset)
- Time-limited (1 hour)

---

## API Endpoints Summary

### Public Endpoints (No Authentication)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /user/signup | Manual registration |
| POST | /user/login | Manual login |
| POST | /user/verify-email | Email verification |
| POST | /user/resend-verification | Resend verification email |
| POST | /user/forgot-password | Request password reset |
| POST | /user/reset-password | Reset password with token |
| POST | /user/forgot-username | Request username reminder |
| GET | /auth/callback | OAuth callback (frontend) |
| GET | /oauth2/authorization/google | Initiate OAuth flow |

### Protected Endpoints (JWT Required)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /user/profile | Get user profile |
| POST | /user/logout | Logout and invalidate tokens |
| POST | /user/refresh | Refresh access token |
| POST | /user/unlink-oauth | Unlink OAuth account |
| POST | /user/oauth-link-info | Get account info for consent |
| POST | /user/confirm-oauth-link | Confirm OAuth linking |

---

## Key Security Principles

1. **Defense in Depth**
   - Multiple layers of validation
   - Frontend + Backend checks
   - Database constraints

2. **Principle of Least Privilege**
   - Tokens expire quickly (15 min access, 7 day refresh)
   - Single active session per user
   - Minimal token payload

3. **Secure Token Storage**
   - All database tokens hashed (SHA-256)
   - Plain tokens never stored
   - One-way hashing (cannot reverse)

4. **Password Security**
   - BCrypt with cost factor 10
   - Slow hashing (prevents brute force)
   - Salted automatically

5. **Token Rotation**
   - Refresh tokens rotated on use
   - Old tokens immediately revoked
   - Detects token reuse (security breach)

6. **Email Verification**
   - Required before password login
   - OAuth accounts auto-verified
   - Prevents fake registrations

7. **Account Protection**
   - Cannot unlink OAuth if no password
   - Prevents account lockout
   - Dual authentication supported

8. **Logout Enforcement**
   - Access tokens blacklisted
   - Refresh tokens revoked
   - Immediate effect

---

## Configuration Files

### application.properties
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/AI_docs_assistant
spring.datasource.username=postgres
spring.datasource.password=admin123

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT (see application.yml)
# Email (see application.yml)
```

### application.yml
```yaml
jwt:
  secret:
    key: ${JWT_SECRET_KEY:my-secret-key-for-development}
  expiration:
    ms: ${JWT_EXPIRATION_MS:900000}  # 15 minutes (900000 ms)
  refresh:
    expiration:
      days: ${JWT_REFRESH_EXPIRATION_DAYS:7}  # 7 days

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:5173}
```

### .env (Backend Configuration)
```dotenv
# JWT Configuration
JWT_SECRET_KEY=your-secret-key-here
JWT_EXPIRATION_MS=900000  # 15 minutes (industry standard)
JWT_REFRESH_EXPIRATION_DAYS=7  # 7 days (industry standard)

# OAuth2
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Frontend
FRONTEND_URL=http://localhost:5173
```

---

## Frontend Integration

### API Service (api.js)

```javascript
class ApiService {
  constructor() {
    this.baseURL = 'http://localhost:8080';
    this.token = localStorage.getItem('access_token');
  }

  async request(endpoint, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    try {
      const response = await fetch(`${this.baseURL}${endpoint}`, {
        ...options,
        headers,
      });

      if (response.status === 401) {
        // Token expired, try refresh
        const refreshed = await this.refreshToken();
        if (refreshed) {
          // Retry original request
          headers['Authorization'] = `Bearer ${this.token}`;
          return fetch(`${this.baseURL}${endpoint}`, {
            ...options,
            headers,
          });
        } else {
          // Refresh failed, redirect to login
          window.location.href = '/login';
        }
      }

      return response.json();
    } catch (error) {
      throw error;
    }
  }

  async refreshToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) return false;

    try {
      const response = await this.post('/user/refresh', {
        refresh_token: refreshToken,
      });

      if (response.access_token) {
        this.setToken(response.access_token);
        localStorage.setItem('refresh_token', response.refresh_token);
        return true;
      }
    } catch (error) {
      return false;
    }
  }

  setToken(token) {
    this.token = token;
    localStorage.setItem('access_token', token);
  }

  async logout() {
    const accessToken = this.getToken();
    const refreshToken = localStorage.getItem('refresh_token');

    if (accessToken || refreshToken) {
      await this.post('/user/logout', {
        access_token: accessToken,
        refresh_token: refreshToken,
      });
    }

    this.clearToken();
    localStorage.removeItem('refresh_token');
    window.location.href = '/';
  }
}
```

### Key Frontend Components

| Component | Route | Purpose |
|-----------|-------|---------|
| Login.jsx | /login | Login and registration forms |
| Dashboard.jsx | /dashboard | Protected dashboard |
| OAuthCallback.jsx | /auth/callback | OAuth token handling |
| EmailVerification.jsx | /verify-email | Email verification |
| ForgotPassword.jsx | /forgot-password | Request password reset |
| ResetPassword.jsx | /reset-password | Reset password form |
| ForgotUsername.jsx | /forgot-username | Request username |
| AccountSettings.jsx | /settings | OAuth management |
| OAuthLinkConsent.jsx | /oauth-link-consent | OAuth consent page |

---

## Error Handling

### Common Error Responses

**Invalid Credentials:**
```json
{
  "success": false,
  "message": "Invalid username or password"
}
```

**Email Not Verified:**
```json
{
  "success": false,
  "message": "Please verify your email before logging in"
}
```

**Token Expired:**
```json
{
  "success": false,
  "message": "Token has expired. Please request a new one."
}
```

**OAuth-Only Account:**
```json
{
  "success": false,
  "message": "This account uses google login. Please use google to sign in."
}
```

**Cannot Unlink OAuth:**
```json
{
  "success": false,
  "message": "Cannot unlink OAuth account. This is your only login method."
}
```

---

## Testing Scenarios

### Manual Registration to Login
```
1. POST /user/signup (create account)
2. Check email for verification link
3. Click link → POST /user/verify-email (verify email)
4. POST /user/login (login with credentials)
5. Store tokens in localStorage
6. Make authenticated requests
```

### OAuth Registration to Login
```
1. Click "Login with Google"
2. OAuth consent screen
3. Redirect to /auth/callback?token=jwt
4. Store token in localStorage
5. Make authenticated requests
```

### Manual Account + OAuth Linking
```
1. POST /user/signup (manual)
2. POST /user/verify-email (verify)
3. Click "Login with Google"
4. System auto-links OAuth
5. Receive notification email
6. Can now login with both methods
```

### Forgot Password Flow
```
1. POST /user/forgot-password (request reset)
2. Check email for reset link
3. Click link → /reset-password?token=uuid
4. POST /user/reset-password (new password)
5. POST /user/login (login with new password)
```

---

## Scheduled Tasks

### Token Cleanup

```java
// RefreshTokenService.java
@Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
public void cleanupExpiredTokens() {
  LocalDateTime now = LocalDateTime.now();
  
  // Delete expired refresh tokens
  refreshTokenRepo.deleteByExpiresAtBefore(now);
  
  // Delete expired invalidated tokens
  invalidatedTokenRepo.deleteByExpiresAtBefore(now);
}
```

**What gets cleaned:**
- Expired refresh tokens (older than 7 days)
- Expired invalidated tokens (older than JWT expiration)
- Expired verification tokens (older than 24 hours)
- Expired password reset tokens (older than 1 hour)

---

## Conclusion

This authentication system implements industry-standard security practices:

- JWT-based stateless authentication
- Refresh token rotation
- Token hashing in database (SHA-256)
- Password hashing (BCrypt)
- Email verification
- OAuth2 integration
- Secure logout with token blacklisting
- Account linking and unlinking
- Password reset with time-limited tokens
- Protection against token reuse
- Single active session enforcement

All sensitive tokens are hashed before database storage, ensuring that even in the event of a database breach, tokens cannot be used to compromise user accounts.

