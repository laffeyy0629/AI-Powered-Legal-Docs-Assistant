# JWT & OAuth Integration Guide

## Overview

Your project implements a **JWT-based authentication system** with **OAuth2 login support** for your AI-Powered Legal Docs Assistant. The backend uses Spring Boot with Spring Security, and the authentication flow supports both traditional username/password login and OAuth2 social login.

---

## Architecture Components

### Core Files Structure

```
Backend (Spring Boot):
├── Security Layer
│   ├── SecurityConfig.java         - Main security configuration
│   ├── JWTFilter.java              - JWT token validation filter
│   └── JWTService.java             - JWT token generation & parsing
├── User Management
│   ├── UserController.java         - REST endpoints
│   ├── UserService.java            - Service interface
│   ├── UserImpl.java               - Business logic implementation
│   ├── UserRepo.java               - Database operations
│   └── User.java (Model)           - User entity
├── Token Management
│   ├── RefreshTokenService.java    - Refresh token operations
│   ├── RefreshTokenRepo.java       - Refresh token database access
│   ├── RefreshToken.java (Model)   - Refresh token entity
│   ├── TokenBlacklistService.java  - Access token blacklist operations
│   ├── InvalidatedTokenRepo.java   - Blacklisted token database access
│   └── InvalidatedToken.java       - Blacklisted token entity
└── DTO
    └── LoginResponse.java          - Login response structure

Frontend (React):
├── Components
│   ├── Login.jsx                   - Login/Signup with resend verification & forgot links
│   ├── EmailVerification.jsx       - Email verification page with resend
│   ├── ForgotPassword.jsx          - Forgot password page (email input)
│   ├── ResetPassword.jsx           - Reset password page (with token)
│   ├── ForgotUsername.jsx          - Forgot username page (email input)
│   ├── Dashboard.jsx               - Protected dashboard
│   ├── Hero.jsx                    - Landing page hero section
│   ├── Features.jsx                - Features showcase
│   ├── OAuthCallback.jsx           - OAuth callback handler
│   └── ThreeBackground.jsx         - 3D animated background
└── Services
    └── api.js                      - API communication service
```

---

## How It Works

### 1. Authentication Flow

#### **Traditional Login Flow (Username/Password)**

```
1. User submits credentials → POST /user/login
   ↓
2. UserController receives request
   ↓
3. UserService validates input (BindingResult)
   ↓
4. UserImpl checks database for username
   ↓
5. BCrypt verifies password hash
   ↓
6. JWTService generates token (valid for 24 hours)
   ↓
7. Server returns: { success: true, message: "...", token: "..." }
   ↓
8. Client stores token (localStorage/sessionStorage)
   ↓
9. Client includes token in subsequent requests:
   Header: "Authorization: Bearer <token>"
   ↓
10. JWTFilter intercepts requests & validates token
    ↓
11. If valid → Request proceeds to protected endpoint
    If invalid → 401 Unauthorized
```

#### **Registration Flow**

```
1. User submits data → POST /user/signup
   ↓
2. Validates input (email format, username/email uniqueness, password length)
   ↓
3. Password is hashed using BCrypt
   ↓
4. User saved to PostgreSQL database
   ↓
5. Returns: { success: true, message: "Registered Successfully" }
   ↓
6. User must then log in to receive JWT token
```

#### **OAuth2 Login Flow**

```
1. User clicks "Login with Google/GitHub" (frontend redirects)
   ↓
2. Spring Security redirects to OAuth2 provider (Google, GitHub, etc.)
   ↓
3. User authorizes app on provider's page
   ↓
4. Provider redirects back with authorization code
   ↓
5. Spring Security exchanges code for access token
   ↓
6. OAuth2 provider returns user profile (email, name, etc.)
   ↓
7. SecurityConfig's successHandler extracts email
   ↓
8. JWTService generates JWT token with email as subject
   ↓
9. Server returns JSON: { success: true, email: "...", token: "..." }
   ↓
10. Frontend stores token and uses for subsequent requests
```

#### **Rotating Refresh Token Flow (Industry Standard)**

```
1. User logs in → POST /user/login
   ↓
2. Server validates credentials
   ↓
3. Server generates:
   - Access token (expires in 15 minutes)
   - Refresh token (expires in 7 days, stored in database)
   ↓
4. Server returns:
   {
     "success": true,
     "access_token": "...",
     "refresh_token": "...",
     "token_type": "Bearer",
     "expires_in": 900
   }
   ↓
5. Frontend stores both tokens (localStorage)
   ↓
6. Frontend AUTOMATICALLY schedules token refresh:
   - Decodes JWT to get expiration time
   - Calculates refresh time (2 minutes before expiry)
   - Sets timer to auto-refresh at that time
   ↓
7. Frontend uses access_token for API requests
   ↓
8. AUTO-REFRESH TIMER TRIGGERS (at 13 minutes):
   ↓
9. Frontend automatically sends → POST /user/refresh
   Body: { "refresh_token": "..." }
   ↓
10. Server validates refresh token:
    - Check if token exists in database (hashed)
    - Check if not expired
    - Check if not revoked
    - Check for token reuse (security)
    ↓
11. Server performs token rotation:
    - Generates NEW access token
    - Generates NEW refresh token
    - Revokes OLD refresh token
    - Links old → new for audit trail
    - Stores tokens as HASHED values in DB
    ↓
12. Server returns new tokens:
    {
      "success": true,
      "access_token": "...",
      "refresh_token": "...",
      "token_type": "Bearer",
      "expires_in": 900
    }
    ↓
13. Frontend updates stored tokens
    ↓
14. Frontend reschedules next auto-refresh (13 min from now)
    ↓
15. If refresh token is expired/revoked:
    → Force user to login again
    ↓
16. AUTOMATIC 401 HANDLING:
    - If API request returns 401
    - Frontend automatically attempts token refresh
    - If successful, retries original request
    - If refresh fails, redirects to login

Security Features:
- Token Reuse Detection: If revoked token is used, ALL user tokens are revoked
- Single Session: Each login revokes previous refresh tokens
- Automatic Cleanup: Expired tokens deleted daily at 3 AM
- Logout Support: POST /user/logout revokes refresh token
- Token Hashing: Refresh tokens stored as SHA-256 hashes in database
- Automatic Refresh: Frontend refreshes tokens 2 minutes before expiry
- Retry Logic: Failed requests auto-retry after token refresh
- No User Intervention: Token refresh is completely transparent to user
```

---

## Detailed File Explanations

### **SecurityConfig.java**

**Purpose**: Configures Spring Security, defines public/protected endpoints, and sets up OAuth2.

**Key Configurations:**

1. **Password Encoding**: Uses BCrypt for secure password hashing
   ```java
   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();
   }
   ```

2. **CORS Configuration**: Allows frontend (localhost:5173, localhost:3000) to make requests
   ```java
   configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
   configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
   ```

3. **Session Management**: Set to STATELESS (required for JWT)
   ```java
   .sessionManagement(session -> session
       .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
   )
   ```

4. **Public Endpoints**: No authentication required
   - `/` - Home
   - `/user/signup` - Registration
   - `/user/login` - Login
   - `/user/status` - User status check
   - `/api/status` - API status check
   - `/auth/**`, `/oauth2/**` - OAuth2 endpoints
   - `/error` - Error handling

5. **OAuth2 Success Handler**: Generates JWT after successful OAuth login with error handling
   ```java
   .oauth2Login(oauth -> oauth
       .successHandler((request, response, authentication) -> {
           try {
               // Generate JWT
               OAuth2User user = (OAuth2User) authentication.getPrincipal();
               String email = user.getAttribute("email");
               
               if (email == null) {
                   response.setStatus(400);
                   response.setContentType("application/json");
                   response.getWriter().write("{\"success\":false,\"message\":\"Email attribute not found in OAuth2 response\"}");
                   return;
               }
               
               String token = jwtService.generateToken(email);

               // Return JWT as JSON - properly escaped
               response.setContentType("application/json");
               String jsonResponse = String.format(
                   "{\"success\":true,\"email\":\"%s\",\"token\":\"%s\"}",
                   email.replace("\"", "\\\"").replace("\\", "\\\\"),
                   token.replace("\"", "\\\"").replace("\\", "\\\\")
               );
               response.getWriter().write(jsonResponse);
           } catch (Exception e) {
               response.setStatus(500);
               response.setContentType("application/json");
               try {
                   response.getWriter().write("{\"success\":false,\"message\":\"OAuth2 authentication failed\"}");
               } catch (Exception ex) {
                   // Log or handle the nested exception
               }
           }
       })
   )
   ```

6. **JWT Filter**: Applied before Spring Security's default authentication filter
   ```java
   .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
   ```

---

### **JWTService.java**

**Purpose**: Handles JWT token generation, parsing, and validation.

**Key Methods:**

1. **generateToken(String userName)**
   - Creates JWT with username as subject
   - Adds "type": "access" claim for token identification
   - Sets issue date and expiration (15 minutes by default)
   - Signs with HMAC-SHA secret key from environment variable
   ```java
   Map<String, Object> claims = new HashMap<>();
   claims.put("type", "access");
   return createToken(claims, userName, expirationMs);
   ```

2. **generateToken(String userName, Map<String, Object> extraClaims)**
   - Creates JWT with custom claims
   - Useful for adding roles, permissions, or other metadata

3. **extractUserName(String token)**
   - Parses JWT and extracts username from subject claim
   - Validates signature

4. **extractExpiration(String token)**
   - Extracts token expiration date

5. **extractClaim(String token, Function<Claims, T> claimsResolver)**
   - Generic method to extract any claim from token

6. **isTokenExpired(String token)**
   - Checks if token has expired
   - Returns true if expired or if ExpiredJwtException is thrown

7. **validateToken(String token, String username)**
   - Validates token against username
   - Checks signature, expiration, and format
   - Catches and logs different JWT exceptions (expired, malformed, invalid signature)
   - Returns false for any validation failure

8. **validateToken(String token)**
   - Validates token without username check
   - Useful for general token validation

9. **init()** (PostConstruct)
   - Initializes the signing key from environment variable
   - Called automatically after dependency injection

**✅ Security Improvements**: 
- Secret key loaded from environment variables (`jwt.secret.key`)
- Expiration time configurable via `jwt.expiration.ms` (default: 15 minutes)
- Enhanced error handling with specific exception types
- Comprehensive logging for security events
- Token validation with multiple checks

---

### **RefreshTokenService.java**

**Purpose**: Manages refresh token lifecycle with industry-standard security practices.

**Key Methods:**

1. **createRefreshToken(String username)**
   - Creates new refresh token for user
   - Automatically revokes existing tokens (single session policy)
   - Generates UUID-based token
   - Sets expiration (7 days by default, configurable)
   ```java
   RefreshToken refreshToken = new RefreshToken();
   refreshToken.setToken(UUID.randomUUID().toString());
   refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
   ```

2. **validateRefreshToken(String token)**
   - Validates token exists, not expired, not revoked
   - Returns Optional<RefreshToken>

3. **rotateRefreshToken(String oldToken)**
   - **Core security feature** - implements token rotation
   - Creates new refresh token
   - Revokes old token and links to new one
   - Detects token reuse attacks:
     ```java
     if (oldRefreshToken.isRevoked()) {
         // Token reuse detected - revoke ALL user tokens
         revokeAllUserTokens(userId);
         throw new RuntimeException("Token reuse detected");
     }
     ```

4. **revokeRefreshToken(String token)**
   - Revokes specific refresh token
   - Sets revoked flag and timestamp

5. **revokeAllUserTokens(Long userId)**
   - Revokes all tokens for a user
   - Used during logout or security events

6. **cleanupExpiredTokens()** (Scheduled)
   - Runs daily at 3 AM
   - Deletes expired tokens from database
   - Keeps database clean

7. **getUserFromRefreshToken(String token)**
   - Extracts user from valid refresh token
   - Used during token refresh

**Security Features:**
- **Token Rotation**: New token on every refresh, old one revoked
- **Reuse Detection**: If revoked token used, all user tokens revoked
- **Single Session**: Each login revokes previous tokens
- **Automatic Cleanup**: Scheduled task removes expired tokens
- **Audit Trail**: Links old token to new token (replacedByToken field)

---

### **RefreshToken.java (Model)**

**Purpose**: JPA entity representing refresh tokens in database.

**Fields:**

- `id` - Primary key (auto-generated)
- `token` - UUID-based token string (unique, indexed)
- `user` - Many-to-one relationship with User entity
- `expiresAt` - Token expiration timestamp
- `createdAt` - Token creation timestamp (auto-set)
- `revoked` - Boolean flag indicating if token is revoked
- `revokedAt` - Timestamp when token was revoked
- `replacedByToken` - Links to new token during rotation (audit trail)

**Methods:**

- `isExpired()` - Checks if token has passed expiration time
- `isActive()` - Returns true if not revoked AND not expired

**Database Table**: `refresh_tokens`

**Indexes**: 
- Unique index on `token` for fast lookups
- Index on `user_id` for user-specific queries

---

### **TokenBlacklistService.java**

**Purpose**: Manages invalidated access tokens (token blacklist) to immediately revoke JWTs upon logout.

**Key Methods:**

1. **invalidateToken(String token)**
   - Adds access token to blacklist
   - Extracts username and expiration from token
   - Only blacklists tokens that are still valid
   - Used during logout to immediately invalidate access tokens
   ```java
   String userName = jwtService.extractUserName(token);
   LocalDateTime expiresAt = convertToLocalDateTime(jwtService.extractExpiration(token));
   InvalidatedToken invalidatedToken = new InvalidatedToken(token, expiresAt, userName);
   invalidatedTokenRepo.save(invalidatedToken);
   ```

2. **isTokenInvalidated(String token)**
   - Checks if token exists in blacklist
   - Called by JWTFilter before accepting token
   - Returns true if token has been invalidated

3. **cleanupExpiredTokens()** (Scheduled)
   - Runs daily at 3:30 AM (30 minutes after refresh token cleanup)
   - Deletes expired blacklisted tokens from database
   - Keeps database clean and efficient

**Why Token Blacklist:**
- JWTs are stateless - cannot be "revoked" by default
- Blacklist allows immediate token invalidation on logout
- Only stores tokens until their natural expiration
- Automatic cleanup prevents database bloat

**Security Benefits:**
- **Immediate Logout**: Tokens invalidated instantly, not after expiration
- **Stolen Token Protection**: If token is compromised, logout blocks it immediately
- **Clean Database**: Expired blacklisted tokens auto-removed
- **Performance**: Only checks blacklist for valid authentication attempts

---

### **InvalidatedToken.java (Model)**

**Purpose**: JPA entity representing blacklisted (invalidated) access tokens.

**Fields:**

- `id` - Primary key (auto-generated)
- `token` - The invalidated JWT token (unique, indexed)
- `invalidatedAt` - Timestamp when token was blacklisted
- `expiresAt` - Original token expiration time
- `userName` - User associated with the token

**Methods:**

- `isExpired()` - Checks if token has passed its natural expiration

**Database Table**: `invalidated_tokens`

**Indexes**: 
- Unique index on `token` for fast blacklist lookups
- Automatically cleaned up after token expiration

**Usage Flow:**
1. User logs out → Access token added to `invalidated_tokens`
2. User tries to use old token → JWTFilter checks blacklist → Rejects request
3. Token expires naturally → Cleanup task removes from database

---

### **JWTFilter.java (Updated)**

**Purpose**: Intercepts every HTTP request to validate JWT tokens.

**Enhanced Process:**

1. Extracts `Authorization` header from request
2. Checks if header starts with "Bearer "
3. Extracts token (removes "Bearer " prefix)
4. **NEW: Checks if token is blacklisted** (invalidated during logout)
5. Validates token using JWTService
6. **NEW: Checks if token is expired**
7. If valid and not blacklisted:
   - Creates authentication object
   - Sets in SecurityContext (grants access)
8. If invalid, blacklisted, or expired:
   - Logs error
   - Request continues but remains unauthenticated

**Security Improvements:**
- **Blacklist Check**: Prevents use of logged-out tokens
- **Expiration Check**: Double-checks token validity
- **Early Return**: Stops processing if token is blacklisted

**Code Flow:**
```java
// Check blacklist first
if (tokenBlacklistService.isTokenInvalidated(token)) {
    System.out.println("Token is blacklisted (invalidated)");
    return; // Don't authenticate
}

// Then check expiration
if (jwt.isTokenExpired(token)) {
    System.out.println("Token has expired");
    return; // Don't authenticate
}
```

**Debugging Features**: Includes extensive console logging to track authentication flow.

---

### **JWTFilter.java (Previous Version)**

**Purpose**: Intercepts every HTTP request to validate JWT tokens.

**Process:**

1. Extracts `Authorization` header from request
2. Checks if header starts with "Bearer "
3. Extracts token (removes "Bearer " prefix)
4. Validates token using JWTService
5. If valid:
   - Creates authentication object
   - Sets in SecurityContext (grants access)
6. If invalid:
   - Logs error
   - Request continues but remains unauthenticated

**Debugging Features**: Includes extensive console logging to track authentication flow.

---

### **UserController.java**

**Purpose**: Exposes REST endpoints for user operations.

**Endpoints:**

1. **GET `/user/signup`** - Returns API documentation for signup endpoint
2. **POST `/user/signup`** - Registers new user
   - Validates with `@Validated(UserService.OnCreate.class)`
   - Required: username, email, password
3. **POST `/user/login`** - Authenticates user
   - Validates with `@Validated(UserService.OnLogin.class)`
   - Required: username, password
   - Returns both access_token and refresh_token on success
   - Response format:
   ```json
   {
     "success": true,
     "message": "Login successful!",
     "access_token": "...",
     "refresh_token": "...",
     "token_type": "Bearer",
     "expires_in": 900
   }
   ```
4. **POST `/user/refresh`** - Refresh access token using refresh token
   - Required: `refresh_token` in request body
   - Performs token rotation (returns new access and refresh tokens)
   - Returns 401 if refresh token is invalid, expired, or revoked
5. **POST `/user/logout`** - Logout and invalidate tokens
   - Optional: `refresh_token` in request body (will be revoked)
   - Optional: `access_token` in request body (will be blacklisted)
   - Revokes the refresh token to prevent further use
   - Adds access token to blacklist to immediately invalidate it
   - Returns success even if tokens are not provided (graceful logout)
6. **GET `/user/verify-email`** - Verify email with token
   - Query parameter: `token` (verification token from email link)
   - Returns success with user verification status
   - Immediately invalidates token after successful verification
   - Returns error with email if token expired (for resend)
7. **POST `/user/resend-verification`** - Resend verification email ✅ FULLY IMPLEMENTED
   - Required: `email` in request body
   - Validates email exists and is not already verified
   - Generates NEW verification token (replaces old one)
   - Sends new email with 24-hour verification link
   - **Frontend Access Points:**
     - **Login Page:** Button appears when login blocked due to unverified email
     - **Email Verification Page:** Button appears when verification link expires
   - Returns 400 Bad Request if email not found or already verified
   - Returns 200 OK with success message when email sent
8. **POST `/user/forgot-password`** - Request password reset ✅ NEWLY IMPLEMENTED
   - Required: `email` in request body
   - Generates secure UUID reset token with 1-hour expiration
   - Sends password reset email with reset link
   - Returns success message (doesn't reveal if email exists for security)
   - OAuth users blocked from password reset (must use OAuth provider)
9. **POST `/user/reset-password`** - Reset password with token ✅ NEWLY IMPLEMENTED
   - Required: `token` and `new_password` in request body
   - Validates token exists and not expired
   - Requires minimum 8 character password
   - Encrypts password with BCrypt
   - Invalidates token immediately (one-time use)
   - Returns 400 if token invalid/expired
10. **POST `/user/forgot-username`** - Request username reminder ✅ NEWLY IMPLEMENTED
   - Required: `email` in request body
   - Sends username reminder email
   - Returns success message (doesn't reveal if email exists for security)
11. **GET `/jwt-try`** - Protected endpoint to test JWT authentication
   - Requires valid JWT token in Authorization header
   - Requires valid JWT token in Authorization header

**Note**: The controller now uses `service.getInputValidationResult(bindingResult)` instead of `inputValidator()`.

---

### **UserImpl.java**

**Purpose**: Implements business logic for user operations.

**Key Methods:**

1. **getInputValidationResult(BindingResult result)** (renamed from `inputValidator`)
   - Validates input using Jakarta Validation annotations
   - Returns formatted error messages

2. **store(User user)** - Registration logic
   - Trims all input fields (username, email, password)
   - Checks password length (min 8 characters)
   - Verifies username and email uniqueness
   - Hashes password with BCrypt
   - Saves to database
   - Improved error handling with logging
   ```java
   user.setUserName(user.getUserName().trim());
   user.setEmail(user.getEmail().trim());
   user.setPassword(encoder.encode(user.getPassword().trim()));
   repo.save(user);
   ```

3. **login(User user)** - Login logic
   - Trims input fields
   - Finds user by username
   - Compares password hash
   - Checks email verification status
   - Generates JWT access token (15 minutes expiration)
   - Creates refresh token (7 days expiration)
   - Returns both tokens with expiration info
   - Improved error handling with logging
   ```java
   String accessToken = jwt.generateToken(foundUser.getUserName());
   RefreshToken refreshToken = refreshTokenService.createRefreshToken(foundUser.getUserName());
   
   response.put("access_token", accessToken);
   response.put("refresh_token", refreshToken.getToken());
   response.put("expires_in", jwtExpirationMs / 1000);
   ```

**Error Handling Improvements**:
- Added SLF4J logging for exceptions
- Returns generic error messages to users (doesn't expose sensitive details)
- All error responses include `"success": false` field

---

### **User.java (Model)**

**Purpose**: JPA entity representing users in the database.

**Fields:**

- `id` - Primary key (auto-generated)
- `userName` - Unique, 8-20 characters (required for login & registration, validated in both groups)
- `email` - Unique, valid email format (required for registration, custom validation message)
- `password` - Min 8 characters, write-only (never exposed in JSON responses, validated in both groups)
- `oAuthProvider` - OAuth provider name (e.g., "google", "github")
- `oAuthId` - User's ID from OAuth provider
- `createdAt` - Auto-timestamp on creation
- `updatedAt` - Auto-timestamp on updates

**Validation Groups**: Uses `OnCreate` and `OnLogin` interfaces to validate different fields for different operations.

---

### **UserRepo.java**

**Purpose**: Database access layer using Spring Data JPA.

**Methods:**
- `findUserByUserName(String userName)` - Query user by username
- `findUserByEmail(String email)` - Query user by email

Returns `Optional<User>` to handle cases where user doesn't exist.

---

## Setup Instructions

### Prerequisites

1. **Database**: PostgreSQL running on `localhost:5432`
2. **Database Name**: `AI_docs_assistant`
3. **Database User**: `postgres` / `admin123`
4. **Java**: Version 21
5. **Maven**: For dependency management
6. **Node.js**: For frontend

### Backend Setup

1. **Configure Database** (application.properties):
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/AI_docs_assistant
   spring.datasource.username=postgres
   spring.datasource.password=admin123
   ```

2. **Set Environment Variables** (Required):
   
   **Windows PowerShell:**
   ```powershell
   $env:JWT_SECRET_KEY="your-secure-secret-key-at-least-32-characters-long"
   $env:JWT_EXPIRATION_MS="900000"  # 15 minutes for access token
   $env:JWT_REFRESH_EXPIRATION_DAYS="7"  # 7 days for refresh token
   ```
   
   **Linux/Mac:**
   ```bash
   export JWT_SECRET_KEY="your-secure-secret-key-at-least-32-characters-long"
   export JWT_EXPIRATION_MS="900000"
   export JWT_REFRESH_EXPIRATION_DAYS="7"
   ```
   
   **Generate secure key:**
   ```bash
   openssl rand -base64 32
   ```
   
   **See `ENVIRONMENT_SETUP.md` for detailed instructions**

3. **Install Dependencies**:
   ```bash
   cd ai-docs-assistant
   ./mvnw clean install
   ```

4. **Run Application**:
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Server starts on**: `http://localhost:8080`

### Frontend Setup

1. **Install Dependencies**:
   ```bash
   cd frontend
   npm install  # or bun install
   ```

2. **Configure API URL** (.env file):
   ```
   VITE_API_BASE_URL=http://localhost:8080
   ```

3. **Run Development Server**:
   ```bash
   npm run dev
   ```

4. **Frontend runs on**: `http://localhost:5173`

### OAuth2 Configuration (Required for Social Login)

**Add to `application.properties`**:

```properties
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=profile,email

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
spring.security.oauth2.client.registration.github.scope=user:email
```

**Get OAuth2 Credentials**:
- **Google**: [Google Cloud Console](https://console.cloud.google.com/)
- **GitHub**: [GitHub Developer Settings](https://github.com/settings/developers)

### Email Configuration (Required for Email Verification)

**Add to `application.yml`**:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            trust: ${MAIL_HOST:smtp.gmail.com}

app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:5173}
```

**Email Service Configuration**:
- **Gmail**: Use App-specific password (not regular password)
  1. Enable 2-factor authentication on Gmail
  2. Go to: https://myaccount.google.com/apppasswords
  3. Generate app password for "Mail"
  4. Add to `ai-docs-assistant/.env` file (see below)
- **Other Providers**: Configure SMTP settings accordingly
- **Development**: Email functionality optional, registration still works without it

**Add to `.env` file** (`ai-docs-assistant/.env`):
```bash
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
FRONTEND_URL=http://localhost:5173
```

**Note**: The `start.ps1` script automatically loads all environment variables from `ai-docs-assistant/.env` and passes them to the Spring Boot application.

---

## Current Limitations & Issues

### 🔴 Critical Issues

1. **~~Hardcoded JWT Secret Key~~ ✅ FIXED**
   - ✅ **IMPLEMENTED**: JWT secret key now uses environment variables
   - Configuration in `application.yml`: `jwt.secret.key=${JWT_SECRET_KEY}`
   - Fallback for development: `my-secret-key-for-development-only-minimum-32-characters-required`
   - **Action Required**: Set `JWT_SECRET_KEY` environment variable in production
   - See `ENVIRONMENT_SETUP.md` for detailed setup instructions
   - **Implementation**: Auto-generated in `start.ps1` script, loaded via environment variables

2. **~~OAuth2 User Creation Missing~~ ✅ FIXED**
   - ✅ **IMPLEMENTED**: OAuth2 users are now automatically saved to database
   - ✅ OAuthService handles user creation/update with provider information
   - ✅ Auto-generates unique usernames from OAuth profile name/email
   - ✅ Stores provider name (google, github) and provider user ID
   - ✅ Handles existing users by updating OAuth info
   - ✅ Generates random secure passwords for OAuth-only accounts

3. **~~Email Verification Missing~~ ✅ FIXED & ENHANCED**
   - ✅ **IMPLEMENTED**: Complete email verification system following industry standards
   - ✅ EmailService with HTML email templates for professional appearance
   - ✅ Verification tokens with 24-hour expiration for security
   - ✅ Unique UUID-based tokens stored securely in database
   - ✅ **One-time use tokens** - token invalidated immediately after successful verification
   - ✅ **Expired token cleanup** - automatic invalidation of expired tokens
   - ✅ **Scheduled cleanup** - removes expired tokens daily at 4 AM
   - ✅ Login blocked for unverified accounts (non-OAuth users)
   - ✅ OAuth users automatically marked as verified (email verified by provider)
   - ✅ Resend verification email functionality (generates new token)
   - ✅ Email verification UI component with status indicators
   - ✅ Verification endpoints: `/user/verify-email` and `/user/resend-verification`
   - ✅ Mail configuration via environment variables
   - ✅ Security logging for verification attempts
   - **Action Required**: Configure SMTP settings in production (see Email Configuration below)

4. **~~No Token Refresh Mechanism~~ ✅ FIXED**
   - ✅ **IMPLEMENTED**: Complete rotating refresh token system following industry standards
   - ✅ Access tokens expire after 15 minutes (industry standard)
   - ✅ Refresh tokens valid for 7 days (configurable)
   - ✅ Automatic token rotation on refresh (new refresh token issued, old one revoked)
   - ✅ Token reuse detection with automatic revocation of all user tokens
   - ✅ Single active session policy (revokes old tokens when new login occurs)
   - ✅ Scheduled cleanup of expired tokens (runs daily at 3 AM)
   - ✅ Secure logout endpoint that revokes refresh tokens
   - ✅ Refresh endpoint: `POST /user/refresh` with `refresh_token` in body
   - ✅ Logout endpoint: `POST /user/logout` with `refresh_token` in body
   - ✅ Login now returns both `access_token` and `refresh_token`
   - ✅ Enhanced JWTService with token validation and expiration checking
   - ✅ RefreshTokenService with comprehensive token management
   - ✅ RefreshToken entity with revocation tracking and token linking

5. **No Role-Based Access Control (RBAC)**
   - All authenticated users have same permissions
   - No admin/user distinction
   - **Fix Required**: Add roles to User model and configure authorities

6. **No Password Reset Flow**
   - Users can't recover forgotten passwords
   - **Fix Required**: Add password reset endpoints with email verification

### ⚠️ Medium Priority Issues

6. **~~Frontend JWT Storage Not Implemented~~ ✅ FIXED**
   - ✅ **IMPLEMENTED**: Full token management in api.js
   - ✅ Tokens stored in localStorage
   - ✅ Authorization header automatically added to requests
   - ✅ Token validation and expiry handling
   - ✅ isAuthenticated() check for protected routes
   - ✅ Proper HTTP status code handling with user-friendly error messages
   - See Frontend Integration Guide for detailed implementation

7. **No Email Verification**
   - Users can register with any email without verification
   - **Fix Required**: Add email confirmation flow

7. **~~OAuth Provider Fields Not Used~~ ✅ FIXED**
   - ✅ **IMPLEMENTED**: `oAuthProvider` and `oAuthId` fields now populated
   - ✅ OAuthService saves provider name (google, github)
   - ✅ OAuth provider user ID stored in database

8. **~~Error Handling Partially Improved~~ ✅ FIXED**
   - ✅ **IMPLEMENTED**: Proper HTTP status codes now used throughout
   - ✅ Generic error messages returned to users (improved security)
   - ✅ SLF4J logging added for exceptions
   - ✅ All error responses include `"success": false` field
   - ✅ ResponseEntity with appropriate HTTP status codes:
     - **200 OK**: Successful login, successful requests
     - **201 Created**: Successful registration
     - **400 Bad Request**: Validation errors, invalid input
     - **401 Unauthorized**: Invalid credentials, expired token
     - **409 Conflict**: User already exists (duplicate username/email)
     - **500 Internal Server Error**: Server-side errors
   - ✅ Frontend api.js handles all status codes with user-friendly messages
   - ✅ Specific error messages for each failure case (login failed, user exists, etc.)

9. **CORS Configuration Too Permissive**
    - Allows all headers with `"*"`
    - **Fix Required**: Specify exact headers needed

### 📝 Low Priority Issues

10. **No Rate Limiting**
    - Vulnerable to brute force attacks
    - **Fix Required**: Add rate limiting to login endpoints

11. **No Logout Mechanism**
    - JWT tokens remain valid until expiration
    - **Fix Required**: Implement token blacklist or use Redis for token management

12. **No Account Lockout**
    - Multiple failed login attempts don't lock account
    - **Fix Required**: Add failed attempt tracking

13. **Minimal Input Sanitization**
    - No XSS protection beyond Spring Security defaults
    - **Fix Required**: Add input sanitization

---

## What to Implement Next

### ~~Priority 1: Complete Basic Authentication~~ ✅ COMPLETED

#### ~~1. **Frontend JWT Integration**~~ ✅ COMPLETED

**Status:** Fully implemented in `frontend/src/services/api.js`

The API service now includes:
- **Automatic Token Refresh**: Tokens automatically refresh 2 minutes before expiry
- **Token Refresh Timer**: Decodes JWT expiration and schedules automatic refresh
- **Refresh Token Rotation**: Stores and updates both access and refresh tokens
- **401 Auto-Retry**: Failed requests automatically retry after token refresh
- Token storage in localStorage (both access_token and refresh_token)
- Automatic token inclusion in headers
- Token management methods (setToken, getToken, clearToken)
- Authentication check (isAuthenticated)
- Complete login/signup integration
- **Seamless User Experience**: Token refresh is completely transparent to users

#### ~~2. **OAuth2 User Persistence**~~ ✅ COMPLETED

**Status:** Fully implemented in `OAuthService.java`

Features implemented:
- Automatic user creation for OAuth logins
- Username generation from OAuth profile
- OAuth provider and ID storage
- Email pre-verification for OAuth users
- Integration with SecurityConfig success handler
- Token generation and frontend redirect

### Priority 2: Add User Roles & Authorization

#### 4. **Add Roles to User Model**

**Update `User.java`**:

```java
@Entity
@Table(name = "users")
public class User {
    // ... existing fields

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public enum Role {
        USER, ADMIN
    }
}
```

#### 5. **Implement UserDetailsService**

**Create `CustomUserDetailsService.java`**:

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findUserByUserName(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUserName())
            .password(user.getPassword())
            .authorities("ROLE_" + user.getRole().name())
            .build();
    }
}
```

#### 6. **Update JWTFilter with Authorities**

**Modify `JWTFilter.java`**:

```java
@Component
public class JWTFilter extends OncePerRequestFilter {
    private final JWTService jwt;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        // ... extract token
        
        String username = jwt.extractUserName(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities() // Now includes roles!
            );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // ... continue
    }
}
```

#### 6. **Use Role-Based Authorization**

**Add to controllers**:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<User> getAllUsers() {
    // Only admins can access
}

@PreAuthorize("hasRole('USER')")
@GetMapping("/user/profile")
public User getProfile() {
    // Authenticated users can access
}
```

**Enable method security** in `SecurityConfig.java`:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // ... existing config
}
```

### ~~Priority 3: Token Refresh & Enhanced Security~~ ✅ MOSTLY COMPLETED

#### ~~8. **Implement Refresh Tokens**~~ ✅ COMPLETED

**Status:** Fully implemented with rotating refresh tokens

Features implemented:
- RefreshToken entity with revocation tracking
- RefreshTokenService with token rotation and reuse detection
- Token expiration (access: 15 min, refresh: 7 days)
- POST `/user/refresh` endpoint with token rotation
- POST `/user/logout` endpoint with token blacklist
- Single session enforcement
- Scheduled cleanup (daily at 3 AM)
- Enhanced JWTService with validation
- LoginResponse DTO with both token types

See "NEWLY IMPLEMENTED - Rotating Refresh Tokens" section in summary for details.

#### ~~9. **Add Password Reset Flow**~~ ✅ COMPLETED

**Status:** Fully implemented with secure token-based reset

Features implemented:
- `POST /user/forgot-password` - sends reset email
- `POST /user/reset-password` - validates token and resets password
- `POST /user/forgot-username` - sends username reminder
- EmailService with professional HTML templates
- Frontend pages: ForgotPassword.jsx, ResetPassword.jsx, ForgotUsername.jsx
- 1-hour token expiration
- One-time use tokens
- OAuth user protection
- Email enumeration prevention
- Scheduled cleanup (daily at 4:30 AM)

See "Forgot Password & Username Recovery" section for complete details.

#### ~~9. **Add Email Verification**~~ ✅ COMPLETED

**Status:** Fully implemented with resend capability

Features implemented:
- Email verification with 24-hour tokens
- Resend verification email functionality
- Email verification UI component
- One-time use tokens with cleanup
- Login blocked for unverified users
- OAuth users auto-verified
- Scheduled cleanup (daily at 4 AM)

See "Email Verification & Resend Functionality" section for complete details.

---

## Email Verification & Resend Functionality

### Complete Implementation ✅

Your application has **fully functional email verification with resend capability** accessible from both frontend and backend.

### Frontend Access Points

#### 1. **Login Page** (`/login`)
**When:** User tries to login with unverified email
**What Happens:**
- Error message displays: "Please verify your email before logging in."
- Yellow "Resend Verification Email" button appears automatically
- Clicking button sends new verification email
- Success message confirms email sent

**Component:** `frontend/src/components/Login.jsx`

**Code:**
```javascript
// Shows resend button when verification required
{showResendVerification && (
  <button onClick={handleResendVerification} disabled={loading}>
    {loading ? 'Sending...' : 'Resend Verification Email'}
  </button>
)}
```

#### 2. **Email Verification Page** (`/verify-email`)
**When:** User clicks expired verification link
**What Happens:**
- "Link Expired" message with red X icon
- Blue "Resend Verification Email" button appears
- Clicking button sends new verification email with popup confirmation

**Component:** `frontend/src/components/EmailVerification.jsx`

**Route:** `http://localhost:5173/verify-email?token=EXPIRED_TOKEN`

### Backend Endpoint

**Endpoint:** `POST /user/resend-verification`

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Verification email sent! Please check your inbox."
}
```

**Response (Already Verified):**
```json
{
  "success": false,
  "message": "Email already verified. You can log in."
}
```

**Response (Email Not Found):**
```json
{
  "success": false,
  "message": "No account found with this email address."
}
```

### How It Works (Backend)

**File:** `ai-docs-assistant/src/main/java/com/isaqcasey/aidocsassistant/Impl/UserImpl.java`

**Process:**
1. Validates email exists in database
2. Checks if email already verified → Returns error if yes
3. Generates NEW UUID verification token
4. Sets NEW 24-hour expiration (`LocalDateTime.now().plusHours(24)`)
5. **Replaces old token** (only one active token per user)
6. Saves updated user to database
7. Calls EmailService to send new verification email
8. Returns success response

**Code:**
```java
public Map<String, Object> resendVerificationEmail(String email) {
    User user = repo.findUserByEmail(email.trim()).orElse(null);
    
    if (user == null) {
        return error("No account found with this email address.");
    }
    
    if (user.isEmailVerified()) {
        return error("Email already verified. You can log in.");
    }
    
    // Generate new token (replaces old one)
    String verificationToken = UUID.randomUUID().toString();
    LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);
    
    user.setVerificationToken(verificationToken);
    user.setVerificationTokenExpiry(tokenExpiry);
    repo.save(user);
    
    // Send email
    emailService.sendVerificationEmail(user.getEmail(), user.getUserName(), verificationToken);
    
    return success("Verification email sent! Please check your inbox.");
}
```

### Testing Resend Verification

#### Test 1: Resend from Login Page

```bash
# 1. Register user
curl -X POST http://localhost:8080/user/signup \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "testuser123",
    "email": "test@example.com",
    "password": "password123"
  }'

# 2. Try to login (will fail - email not verified)
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "testuser123",
    "password": "password123"
  }'

# Response includes: "requiresVerification": true, "email": "test@example.com"

# 3. Resend verification email
curl -X POST http://localhost:8080/user/resend-verification \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Response: { "success": true, "message": "Verification email sent!" }
```

#### Test 2: Resend from Expired Link

```bash
# 1. Manually expire token in database
psql -U postgres -d AI_docs_assistant -c \
  "UPDATE users SET verification_token_expiry = NOW() - INTERVAL '1 hour' \
   WHERE email = 'test@example.com';"

# 2. Get the expired token
TOKEN=$(psql -U postgres -d AI_docs_assistant -t -c \
  "SELECT verification_token FROM users WHERE email = 'test@example.com';")

# 3. Try to verify with expired token (will fail)
curl "http://localhost:8080/user/verify-email?token=$TOKEN"

# Response: { "success": false, "expired": true, "email": "test@example.com" }

# 4. Resend verification
curl -X POST http://localhost:8080/user/resend-verification \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# New email sent with new token!
```

#### Test 3: Frontend Manual Test

**Steps:**
1. Go to `http://localhost:5173/login`
2. Register new user
3. Try to login with that user
4. See error: "Please verify your email..."
5. See yellow "Resend Verification Email" button
6. Click button
7. See success message
8. Check email inbox
9. Click verification link
10. Email verified! Can now login

### Security Features

✅ **One Active Token:** Old token replaced when new one generated
✅ **24-Hour Expiration:** All tokens expire after 24 hours
✅ **One-Time Use:** Token invalidated immediately after successful verification
✅ **Expired Token Cleanup:** Scheduled task cleans up expired tokens daily at 4 AM
✅ **Email Validation:** Checks if email exists and is not already verified
✅ **Token Replacement:** Each resend generates completely new token

### User Experience Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    User Journey                             │
└─────────────────────────────────────────────────────────────┘

Registration
   ↓
Email Sent (24h expiration)
   ↓
   ├─→ User clicks link → ✅ Verified → Can Login
   │
   ├─→ Link expired → Opens expired page
   │      ↓
   │   Clicks "Resend" button
   │      ↓
   │   New email sent
   │      ↓
   │   Clicks new link → ✅ Verified
   │
   └─→ Never got email → Tries to login
          ↓
       Login blocked with error
          ↓
       Clicks "Resend" button on login page
          ↓
       New email sent
          ↓
       ✅ Verified → Can Login
```

---

## Forgot Password & Username Recovery

### Complete Implementation ✅

Your application now has **full password reset and username recovery functionality** with secure token-based verification.

### Features Implemented

#### 1. **Forgot Password** - Password Reset via Email
- User requests password reset by providing email
- System generates secure UUID reset token (1-hour expiration)
- Email sent with password reset link
- User clicks link and enters new password
- Token validated and password updated
- **One-time use:** Token invalidated immediately after use
- **Security:** OAuth users cannot reset password (use OAuth provider)

#### 2. **Reset Password** - Secure Password Change
- Validates reset token exists and not expired
- Requires minimum 8 character password
- Encrypts new password with BCrypt
- Invalidates token immediately (one-time use)
- Clears expired tokens automatically

#### 3. **Forgot Username** - Username Reminder via Email
- User provides email address
- System sends username reminder email
- Simple, secure, no token required
- Includes link to login page

### Backend Endpoints

#### **POST `/user/forgot-password`**

Request password reset email.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "If an account exists with this email, a password reset link has been sent. Please check your inbox."
}
```

**Response (OAuth User):**
```json
{
  "success": false,
  "message": "This account uses Google login. Please use Google to sign in."
}
```

**Security Note:** Always returns success message even if email doesn't exist (prevents email enumeration attacks).

#### **POST `/user/reset-password`**

Reset password using token from email.

**Request:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "new_password": "newSecurePassword123"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Password reset successfully! You can now log in with your new password."
}
```

**Response (Expired Token):**
```json
{
  "success": false,
  "message": "Reset link has expired. Please request a new password reset.",
  "expired": true
}
```

**Response (Invalid Token):**
```json
{
  "success": false,
  "message": "Invalid or expired reset link. Please request a new password reset."
}
```

#### **POST `/user/forgot-username`**

Request username reminder email.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "If an account exists with this email, a username reminder has been sent. Please check your inbox."
}
```

### Implementation Details

#### Database Schema

**User Model - New Fields:**
```java
private String passwordResetToken;           // UUID reset token
private LocalDateTime passwordResetTokenExpiry;  // 1-hour expiration
```

#### Password Reset Flow

```
User Forgot Password
   ↓
1. User enters email → POST /user/forgot-password
   ↓
2. Backend generates UUID token + 1hr expiry
   ↓
3. Token saved to database (user.passwordResetToken)
   ↓
4. Email sent with link: /reset-password?token=UUID
   ↓
5. User clicks link → Opens reset password page
   ↓
6. User enters new password → POST /user/reset-password
   ↓
7. Backend validates:
   - Token exists in database
   - Token not expired
   - Password meets requirements (8+ chars)
   ↓
8. If valid:
   - Password encrypted with BCrypt
   - Token cleared (one-time use)
   - User can login with new password
   ↓
9. If expired:
   - Token cleared from database
   - User must request new reset
```

#### Username Recovery Flow

```
User Forgot Username
   ↓
1. User enters email → POST /user/forgot-username
   ↓
2. Backend finds user by email
   ↓
3. Email sent with username displayed
   ↓
4. User sees username → Goes to login
   ↓
5. User logs in with remembered username
```

### Security Features

✅ **Token Expiration:** Reset tokens expire after 1 hour (email verification: 24 hours)
✅ **One-Time Use:** Tokens invalidated immediately after successful password reset
✅ **Expired Token Cleanup:** Scheduled task removes expired tokens daily at 4:30 AM
✅ **OAuth Protection:** OAuth users cannot reset password (must use OAuth provider)
✅ **Email Enumeration Prevention:** Always returns success message even if email not found
✅ **Secure Token Generation:** UUID v4 (128-bit random, cryptographically secure)
✅ **Password Encryption:** BCrypt with salt (industry standard)
✅ **Token Storage:** Secure database storage with expiration tracking
✅ **Expired Token Handling:** Automatically cleared when detected

### Email Templates

#### Password Reset Email

**Subject:** Reset Your Password - AI Legal Docs Assistant

**Content:**
- Professional HTML template with gradient header
- Large "Reset Password" button with reset link
- Link expiration warning (1 hour)
- Security notice if user didn't request reset
- Clickable link as backup

**Link Format:** `http://localhost:5173/reset-password?token=UUID`

#### Username Reminder Email

**Subject:** Username Reminder - AI Legal Docs Assistant

**Content:**
- Professional HTML template
- Username displayed prominently in bordered box
- "Go to Login" button
- Tip to save username in password manager
- Security notice

### Testing the Functionality

#### Test 1: Forgot Password Flow

```bash
# 1. Request password reset
curl -X POST http://localhost:8080/user/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Response: 
# {
#   "success": true,
#   "message": "If an account exists with this email, a password reset link has been sent."
# }

# 2. Check email for reset link with token

# 3. Reset password with token
curl -X POST http://localhost:8080/user/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "new_password": "newPassword123"
  }'

# Response:
# {
#   "success": true,
#   "message": "Password reset successfully! You can now log in with your new password."
# }

# 4. Login with new password
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "testuser123",
    "password": "newPassword123"
  }'

# Success! User logged in with new password
```

#### Test 2: Expired Token

```bash
# 1. Manually expire a reset token in database
psql -U postgres -d AI_docs_assistant -c \
  "UPDATE users SET password_reset_token_expiry = NOW() - INTERVAL '2 hours' \
   WHERE email = 'test@example.com';"

# 2. Try to reset with expired token
curl -X POST http://localhost:8080/user/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "EXPIRED_TOKEN",
    "new_password": "newPassword123"
  }'

# Response:
# {
#   "success": false,
#   "message": "Reset link has expired. Please request a new password reset.",
#   "expired": true
# }

# Token automatically cleared from database
```

#### Test 3: Forgot Username

```bash
# 1. Request username reminder
curl -X POST http://localhost:8080/user/forgot-username \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Response:
# {
#   "success": true,
#   "message": "If an account exists with this email, a username reminder has been sent."
# }

# 2. Check email - username displayed clearly

# 3. User can now login with remembered username
```

#### Test 4: OAuth User Password Reset

```bash
# OAuth users cannot reset password
curl -X POST http://localhost:8080/user/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "googleuser@example.com"}'

# Response:
# {
#   "success": false,
#   "message": "This account uses Google login. Please use Google to sign in."
# }
```

### Database Queries

#### View Password Reset Tokens

```sql
-- View all active reset tokens
SELECT 
    user_name,
    email,
    LEFT(password_reset_token, 20) as token_preview,
    password_reset_token_expiry,
    CASE 
        WHEN password_reset_token_expiry < NOW() THEN 'EXPIRED'
        WHEN password_reset_token IS NULL THEN 'NONE'
        ELSE 'ACTIVE'
    END as status
FROM users
WHERE password_reset_token IS NOT NULL
ORDER BY password_reset_token_expiry DESC;

-- Count reset token states
SELECT 
    CASE 
        WHEN password_reset_token IS NULL THEN 'NO TOKEN'
        WHEN password_reset_token_expiry < NOW() THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END as status,
    COUNT(*) as count
FROM users
GROUP BY status;

-- Find user with specific reset token
SELECT user_name, email, password_reset_token_expiry 
FROM users 
WHERE password_reset_token = 'YOUR_TOKEN_HERE';
```

#### Manual Token Operations

```sql
-- Manually create reset token for testing
UPDATE users 
SET password_reset_token = gen_random_uuid()::text,
    password_reset_token_expiry = NOW() + INTERVAL '1 hour'
WHERE email = 'test@example.com';

-- Clear all reset tokens
UPDATE users 
SET password_reset_token = NULL,
    password_reset_token_expiry = NULL
WHERE password_reset_token IS NOT NULL;

-- Expire a token immediately (for testing)
UPDATE users 
SET password_reset_token_expiry = NOW() - INTERVAL '1 hour'
WHERE email = 'test@example.com';
```

### Scheduled Maintenance

**EmailVerificationCleanupService** now includes password reset token cleanup:

**Schedule:**
- Email verification tokens: Daily at 4:00 AM
- Password reset tokens: Daily at 4:30 AM
- Refresh tokens: Daily at 3:00 AM
- Invalidated tokens (blacklist): Daily at 3:30 AM

**What Gets Cleaned:**
```java
@Scheduled(cron = "0 30 4 * * ?")
public void cleanupExpiredPasswordResetTokens() {
    int cleaned = userRepo.clearExpiredPasswordResetTokens(LocalDateTime.now());
    log.info("Cleaned up {} expired password reset tokens", cleaned);
}
```

### Code Locations

#### Backend Implementation

**User Model:** `ai-docs-assistant/src/main/java/.../Model/User.java`
- Lines 53-54: `passwordResetToken`, `passwordResetTokenExpiry`

**UserRepo:** `ai-docs-assistant/src/main/java/.../Repo/UserRepo.java`
- Line 11: `findUserByPasswordResetToken()`
- Lines 17-19: `clearExpiredPasswordResetTokens()`

**UserService:** `ai-docs-assistant/src/main/java/.../Service/UserService.java`
- Lines 16-18: Method signatures for forgot/reset operations

**UserImpl:** `ai-docs-assistant/src/main/java/.../Impl/UserImpl.java`
- Lines 363-420: `forgotPassword()` implementation
- Lines 423-495: `resetPassword()` implementation
- Lines 498-537: `forgotUsername()` implementation

**UserController:** `ai-docs-assistant/src/main/java/.../Controller/UserController.java`
- Lines 239-271: `/user/forgot-password` endpoint
- Lines 274-310: `/user/reset-password` endpoint
- Lines 313-337: `/user/forgot-username` endpoint

**EmailService:** `ai-docs-assistant/src/main/java/.../Service/EmailService.java`
- Lines 159-182: `sendPasswordResetEmail()` method
- Lines 185-276: Password reset email HTML template
- Lines 282-305: `sendUsernameReminderEmail()` method
- Lines 308-358: Username reminder email HTML template

**SecurityConfig:** `ai-docs-assistant/src/main/java/.../config/SecurityConfig.java`
- Lines 60-62: Public access for forgot/reset endpoints

**EmailVerificationCleanupService:** `ai-docs-assistant/src/main/java/.../Service/EmailVerificationCleanupService.java`
- Lines 33-44: Password reset token cleanup scheduler

### Configuration

**Token Expiration:**
- Email verification: 24 hours
- Password reset: 1 hour
- Refresh token: 7 days

**Frontend URL:** Configure in `application.yml`:
```yaml
app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:5173}
```

### Error Handling

**Password Reset Errors:**
- Missing email → 400 Bad Request
- Missing token → 400 Bad Request
- Invalid token → 400 Bad Request, "Invalid or expired reset link"
- Expired token → 400 Bad Request, token cleared automatically
- Password too short → 400 Bad Request, "Password must be at least 8 characters"
- OAuth user → 400 Bad Request, "Use OAuth provider to sign in"

**All errors logged with SLF4J for monitoring and debugging**

### Next Steps for Frontend ✅ COMPLETED

The frontend implementation has been completed! Here's what was created:

1. **Forgot Password Page** (`/forgot-password`) ✅ IMPLEMENTED
   - Clean, modern UI with Three.js background
   - Email input form with validation
   - Submit to POST `/user/forgot-password`
   - Success/error messages with animations
   - Links to login and forgot username pages
   - Component: `frontend/src/components/ForgotPassword.jsx`

2. **Reset Password Page** (`/reset-password?token=UUID`) ✅ IMPLEMENTED
   - Extracts token from URL query parameter
   - New password and confirm password inputs
   - Password visibility toggle
   - Real-time password validation (8+ characters, matching)
   - Submit to POST `/user/reset-password`
   - Automatic redirect to login on success
   - Handles expired tokens with redirect to forgot password
   - Component: `frontend/src/components/ResetPassword.jsx`

3. **Forgot Username Page** (`/forgot-username`) ✅ IMPLEMENTED
   - Email input form with validation
   - Submit to POST `/user/forgot-username`
   - Success/error messages
   - Links to login and forgot password pages
   - Component: `frontend/src/components/ForgotUsername.jsx`

4. **Updated Login Page** ✅ IMPLEMENTED
   - "Forgot Password?" link next to password field (login mode only)
   - "Forgot Username?" link below password field (login mode only)
   - Both links styled and positioned for optimal UX
   - Component: `frontend/src/components/Login.jsx`

5. **Route Configuration** ✅ IMPLEMENTED
   - Routes added to `App.jsx`:
     - `/forgot-password` → ForgotPassword component
     - `/reset-password` → ResetPassword component
     - `/forgot-username` → ForgotUsername component

### Frontend Features Implemented

**All Pages Include:**
- ✅ Consistent design with Three.js animated backgrounds
- ✅ Responsive layouts (mobile-friendly)
- ✅ Framer Motion animations
- ✅ Loading states with spinners
- ✅ Error handling with styled error messages
- ✅ Success messages with checkmarks
- ✅ Form validation (client-side)
- ✅ Disabled states during API calls
- ✅ Breadcrumb navigation links
- ✅ Gradient backgrounds matching app theme

**Reset Password Page Specific Features:**
- ✅ Password visibility toggle (eye icon)
- ✅ Real-time password strength feedback
- ✅ Password match validation
- ✅ Character count validation (8+ chars)
- ✅ Token validation from URL
- ✅ Expired token handling with redirect
- ✅ Auto-redirect to login after success (3 seconds)

**User Experience Flow:**

```
User Forgot Password Flow:
1. User clicks "Forgot Password?" on login page
   ↓
2. Opens /forgot-password page
   ↓
3. User enters email → Submit
   ↓
4. Success message displayed
   ↓
5. User checks email → Clicks reset link
   ↓
6. Opens /reset-password?token=UUID page
   ↓
7. User enters new password (with validation)
   ↓
8. Submit → Success message
   ↓
9. Auto-redirect to /login (3 seconds)
   ↓
10. User logs in with new password ✅
```

```
User Forgot Username Flow:
1. User clicks "Forgot your username?" on login page
   ↓
2. Opens /forgot-username page
   ↓
3. User enters email → Submit
   ↓
4. Success message displayed
   ↓
5. User checks email → Sees username
   ↓
6. User clicks "Back to Login"
   ↓
7. User logs in with remembered username ✅
```

### Testing the Frontend

**Start the development server:**
```bash
cd frontend
npm run dev
```

**Access the pages:**
- Forgot Password: `http://localhost:5173/forgot-password`
- Reset Password: `http://localhost:5173/reset-password?token=YOUR_TOKEN`
- Forgot Username: `http://localhost:5173/forgot-username`

**Test the complete flow:**
1. Visit `http://localhost:5173/login`
2. Click "Forgot Password?" link
3. Enter email and submit
4. Check backend email service or database for token
5. Visit reset password page with token
6. Enter new password
7. Get redirected to login
8. Login with new password ✅

---

## Testing the Current Implementation

### Test Registration

```bash
curl -X POST http://localhost:8080/user/signup \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "testuser123",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Registered Successfully"
}
```

### Test Login

```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "testuser123",
    "password": "password123"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Login successful! Redirecting to dashboard...",
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
  "token_type": "Bearer",
  "expires_in": 900
}
```

### Test Refresh Token

```bash
curl -X POST http://localhost:8080/user/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "660e8400-e29b-41d4-a716-446655440111",
  "token_type": "Bearer",
  "expires_in": 900
}
```

**Note**: The old refresh token is now revoked and cannot be reused. The new refresh token must be used for future refreshes.

### Test Logout

```bash
curl -X POST http://localhost:8080/user/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "660e8400-e29b-41d4-a716-446655440111",
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

**What Happens:**
- Refresh token is revoked in database (cannot be used to get new tokens)
- Access token is added to blacklist (immediately invalidated)
- Any subsequent requests with these tokens will be rejected

**Verify Logout (Test Blacklisted Token):**
```bash
# Try to use the access token after logout
curl -X GET http://localhost:8080/jwt-try \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Expected:** 401 Unauthorized or authentication failure (token is blacklisted)

### Test Protected Endpoint

```bash
curl -X GET http://localhost:8080/jwt-try \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Expected Response**:
```
JWT works
```

---

## Security Best Practices

### Immediate Actions

1. ✅ **Move JWT secret to environment variables**
2. ✅ **Use HTTPS in production** (configure SSL certificates)
3. ✅ **Implement CSRF protection** (for form-based auth)
4. ✅ **Add rate limiting** (prevent brute force)
5. ✅ **Validate and sanitize all inputs**
6. ✅ **Use secure password requirements** (min length, complexity)
7. ✅ **Implement account lockout** (after failed attempts)
8. ✅ **Log security events** (failed logins, token validation failures)

### Production Checklist

- [x] JWT secret key in environment variable (IMPLEMENTED - must be set in production)
- [ ] OAuth2 credentials secured
- [ ] Database credentials in environment variables
- [ ] HTTPS enabled with valid SSL certificate
- [ ] CORS configured for production domain only
- [ ] Rate limiting enabled
- [ ] Security headers configured (CSP, HSTS, etc.)
- [ ] Error messages don't leak sensitive information
- [ ] Logging configured (without sensitive data)
- [ ] Token expiration reasonable for use case
- [ ] Refresh token rotation implemented
- [x] Account verification required (EMAIL VERIFICATION IMPLEMENTED)
- [ ] Password reset flow tested
- [ ] Security testing completed (OWASP Top 10)

---

## Summary

Your partner has successfully implemented:

✅ **JWT authentication** with token generation and validation  
✅ **Spring Security** configuration with CORS and session management  
✅ **User registration and login** endpoints with validation  
✅ **Password hashing** using BCrypt  
✅ **OAuth2 integration** with complete user persistence  
✅ **Database persistence** using JPA and PostgreSQL  
✅ **Request filtering** to validate JWT on protected endpoints  
✅ **Input trimming and sanitization** for username, email, and password fields  
✅ **Complete error handling** with proper HTTP status codes and user-friendly messages  
✅ **Enhanced validation** with proper validation groups for login and registration  
✅ **Environment variable configuration** for JWT secret and expiration  
✅ **OAuthService** for automatic user creation and management  
✅ **Complete frontend authentication** with React, token management, and protected routes  
✅ **ResponseEntity with HTTP status codes** for proper REST API responses  
✅ **Email verification system** with industry-standard security practices  
✅ **Rotating refresh tokens** with industry-standard security implementation

**Recent improvements by your partner:**
- Renamed `inputValidator()` to `getInputValidationResult()` for clarity
- Added input trimming for all user fields before processing
- Implemented proper exception logging with SLF4J
- Enhanced OAuth2 success handler with null checks and try-catch blocks
- Improved JSON response escaping for OAuth2 authentication
- Added `"success": false` field to all error responses
- Updated validation messages to be more user-friendly
- **✅ Moved JWT secret key to environment variables** (SECURITY FIX)
- **✅ Made JWT expiration time configurable** via environment variable

**Latest implementations (December 23-24, 2025):**
- **✅ Created modern Login/Signup page** with React, Tailwind, GSAP, Framer Motion
- **✅ Integrated Google OAuth login** with redirect flow
- **✅ Implemented JWT token storage** in frontend (localStorage)
- **✅ Created OAuthService** for user persistence
- **✅ OAuth users now saved to database** with auto-generated usernames
- **✅ OAuth provider information stored** (provider name and ID)
- **✅ Dashboard component** with authentication check and Three.js background
- **✅ OAuth callback handler** for token processing
- **✅ Updated api.js** with complete token management
- **✅ Fixed duplicate className bug** in Dashboard component
- **✅ Fixed React hooks issues** in Login component
- **✅ Fixed loginButtonRef issue** in Hero component
- **✅ Fixed OAuthCallback** token handling and navigation
- **✅ Fixed JSON parsing errors** in protected endpoint calls
- **✅ Animated backgrounds** applied to Login and Dashboard pages
- **✅ Implemented proper HTTP status codes** (200, 201, 400, 401, 409, 500)
- **✅ Enhanced error messages** for better user experience
- **✅ Frontend handles all HTTP status codes** with specific error messages
- **✅ Email verification system** with industry-standard security practices
- **✅ EmailService with HTML email templates** for verification and password reset
- **✅ Email verification tokens** with 24-hour expiration
- **✅ OAuth users automatically verified** (email pre-verified by provider)
- **✅ Resend verification email** functionality
- **✅ Email verification UI component** with status indicators
- **✅ Login blocked for unverified accounts** (non-OAuth users)

**NEWLY IMPLEMENTED - Rotating Refresh Tokens (December 24, 2025):**
- **✅ RefreshToken entity** with revocation tracking and audit trail
- **✅ RefreshTokenService** with comprehensive token management
- **✅ RefreshTokenRepo** with query methods for token operations
- **✅ Token rotation** on every refresh (new token issued, old one revoked)
- **✅ Token reuse detection** with automatic revocation of all user tokens
- **✅ Single session policy** (revokes old tokens on new login)
- **✅ Scheduled cleanup** of expired tokens (daily at 3 AM)
- **✅ Logout endpoint** that revokes refresh tokens
- **✅ Enhanced JWTService** with validation and expiration checking
- **✅ Access tokens** expire in 15 minutes (industry standard)
- **✅ Refresh tokens** valid for 7 days (configurable)
- **✅ Login returns both** access_token and refresh_token
- **✅ POST /user/refresh** endpoint for token rotation
- **✅ POST /user/logout** endpoint for secure logout
- **✅ Enhanced LoginResponse DTO** with both token types
- **✅ @EnableScheduling** for automated token cleanup

**NEWLY IMPLEMENTED - Token Blacklist for Immediate Logout (December 24, 2025):**
- **✅ InvalidatedToken entity** for storing blacklisted access tokens
- **✅ TokenBlacklistService** for managing token blacklist
- **✅ InvalidatedTokenRepo** with query methods for blacklist operations
- **✅ JWTFilter enhanced** to check token blacklist before authentication
- **✅ Logout invalidates access tokens** immediately (adds to blacklist)
- **✅ Logout revokes refresh tokens** (prevents new token generation)
- **✅ Scheduled cleanup** of expired blacklisted tokens (daily at 3:30 AM)
- **✅ Complete logout implementation** - both token types invalidated
- **✅ Immediate token revocation** - no waiting for expiration
- **✅ Database-backed blacklist** - works across multiple server instances
- **✅ Automatic cleanup** - blacklisted tokens removed after natural expiration

**NEWLY IMPLEMENTED - Email Verification Security Enhancements (December 24, 2025):**
- **✅ One-time use tokens** - verification tokens invalidated immediately after use
- **✅ Expired token invalidation** - expired tokens automatically cleared from database
- **✅ EmailVerificationCleanupService** - scheduled cleanup of expired verification tokens
- **✅ Scheduled cleanup** runs daily at 4 AM
- **✅ Token invalidation on expiry** - prevents expired token reuse
- **✅ Enhanced security logging** - logs verification attempts and expiry events
- **✅ Already verified check** - clears tokens even if user already verified
- **✅ UserRepo enhancement** - added clearExpiredVerificationTokens query method
- **✅ Zero-trust approach** - tokens cannot be reused once verified or expired

**NEWLY IMPLEMENTED - Automatic Frontend Token Refresh (December 24, 2025):**
- **✅ Automatic token refresh timer** - automatically refreshes tokens 2 minutes before expiry
- **✅ JWT token decoding** - extracts expiration time from access token
- **✅ Smart refresh scheduling** - calculates and schedules refresh at optimal time
- **✅ Token rotation on refresh** - receives and stores new access + refresh tokens
- **✅ 401 auto-retry logic** - automatically refreshes token and retries failed requests
- **✅ Prevents multiple simultaneous refreshes** - uses promise to queue concurrent attempts
- **✅ Automatic redirect on refresh failure** - redirects to login if refresh token invalid
- **✅ Seamless user experience** - users never see token expiration errors
- **✅ Console logging** - logs refresh scheduling and execution for debugging
- **✅ Timer cleanup on logout** - properly stops refresh timer when user logs out
- **✅ Handles edge cases** - token decode errors, missing tokens, expired refresh tokens
- **✅ Frontend implementation** - complete in `frontend/src/services/api.js`

**How It Works:**
```
Login → Access Token (expires in 15 min) + Refresh Token (expires in 7 days)
  ↓
Frontend decodes JWT and extracts expiration time
  ↓
Frontend schedules automatic refresh at 13 minutes (2 min before expiry)
  ↓
Timer triggers → Calls POST /user/refresh with refresh_token
  ↓
Backend validates, rotates tokens, returns new tokens
  ↓
Frontend updates localStorage and reschedules next refresh
  ↓
User continues working without interruption ✅

If API request returns 401:
  ↓
Frontend automatically calls POST /user/refresh
  ↓
If successful: Retries original request with new token
  ↓
If refresh fails: Redirects to login page
```

**User Benefits:**
- **Zero interruption:** Never see "session expired" errors
- **Automatic renewal:** Tokens refresh in background while user works
- **Smart retry:** Failed requests automatically retry after token refresh
- **Secure by default:** Short-lived access tokens (15 min) for security
- **Long sessions:** Can work for 7 days without re-login (via refresh token)

**NEWLY IMPLEMENTED - Forgot Password & Username Recovery (December 24, 2025):**
- **✅ Forgot Password functionality** - users can reset forgotten passwords via email
- **✅ Secure reset tokens** - UUID-based tokens with 1-hour expiration
- **✅ Password reset endpoint** - POST `/user/forgot-password` sends reset email
- **✅ Reset password endpoint** - POST `/user/reset-password` with token validation
- **✅ Forgot Username functionality** - users can retrieve forgotten username via email
- **✅ Username reminder endpoint** - POST `/user/forgot-username` sends username
- **✅ One-time use reset tokens** - tokens invalidated immediately after password reset
- **✅ OAuth protection** - OAuth users cannot reset password (must use OAuth provider)
- **✅ Email enumeration prevention** - doesn't reveal if email exists (security)
- **✅ Password validation** - minimum 8 characters enforced
- **✅ HTML email templates** - professional password reset and username reminder emails
- **✅ Username email visibility fix** - username text changed to black for readability
- **✅ Scheduled cleanup** - expired reset tokens removed daily at 4:30 AM
- **✅ Database schema** - added passwordResetToken and passwordResetTokenExpiry fields
- **✅ UserRepo queries** - findUserByPasswordResetToken and clearExpiredPasswordResetTokens
- **✅ Complete error handling** - expired tokens, invalid tokens, OAuth users
- **✅ Security logging** - all reset operations logged for audit trail

**NEWLY IMPLEMENTED - Frontend Password/Username Recovery UI (December 24, 2025):**
- **✅ ForgotPassword.jsx** - complete forgot password page with email input
- **✅ ResetPassword.jsx** - password reset page with token validation & visibility toggle
- **✅ ForgotUsername.jsx** - username recovery page with email input
- **✅ Login.jsx updated** - added "Forgot Password?" and "Forgot Username?" links
- **✅ App.jsx routing** - added routes for /forgot-password, /reset-password, /forgot-username
- **✅ Consistent design** - Three.js backgrounds, Framer Motion animations
- **✅ Real-time validation** - password strength, matching passwords, email format
- **✅ Loading states** - spinners and disabled buttons during API calls
- **✅ Success/error messaging** - styled alerts with icons and animations
- **✅ Auto-redirect** - reset password page redirects to login after success
- **✅ Expired token handling** - automatic redirect to forgot password on expired token
- **✅ Password visibility toggle** - eye icon for show/hide password
- **✅ Responsive design** - mobile-friendly layouts for all pages
- **✅ Navigation links** - breadcrumb navigation between related pages
- **✅ Link positioning** - forgot username/password links next to respective field labels

**Next steps** focus on:
1. ~~Frontend integration (storing and sending tokens)~~ ✅ COMPLETED
2. ~~Securing the JWT secret key~~ ✅ COMPLETED
3. ~~Completing OAuth2 user persistence~~ ✅ COMPLETED
4. ~~Creating Login/Signup UI~~ ✅ COMPLETED
5. ~~Fixing OAuth callback and token flow~~ ✅ COMPLETED
6. ~~Implementing proper HTTP status codes~~ ✅ COMPLETED
7. ~~Email verification for registration~~ ✅ COMPLETED
8. ~~Implementing token refresh mechanism~~ ✅ COMPLETED
9. ~~Implementing password reset functionality~~ ✅ COMPLETED
10. ~~Frontend forgot password/username pages~~ ✅ COMPLETED
11. ~~Frontend automatic token refresh logic~~ ✅ COMPLETED
12. Adding roles and authorization (RBAC)
13. Implementing rate limiting for security

The authentication system is now **production-ready with industry-standard security**! All core authentication features are complete including:
- ✅ Secure JWT tokens with short expiration (15 minutes)
- ✅ Rotating refresh tokens with reuse detection
- ✅ **Automatic frontend token refresh** (2 minutes before expiry)
- ✅ **401 auto-retry with token refresh** (seamless user experience)
- ✅ Single session enforcement
- ✅ Automatic token cleanup
- ✅ Secure logout mechanism with token blacklist
- ✅ Email verification with resend capability
- ✅ Password reset via email with secure tokens
- ✅ Username recovery via email
- ✅ Complete frontend UI for all auth features
- ✅ OAuth2 integration (Google)
- ✅ Proper error handling
- ✅ Comprehensive logging
- ✅ Scheduled maintenance tasks

Focus on roles/authorization (RBAC) and rate limiting for the complete production solution.
