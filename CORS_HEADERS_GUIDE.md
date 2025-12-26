# CORS Headers Guide - When to Update

## Current Configuration ✅

**File:** `ai-docs-assistant/src/main/java/com/isaqcasey/aidocsassistant/config/SecurityConfig.java`

```java
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",      // JWT Bearer tokens
    "Content-Type",       // JSON/form data
    "Accept",            // Response format
    "Origin",            // CORS requirement
    "X-Requested-With"   // AJAX identification
));
```

---

## When to Update (Add More Headers)

### ✅ You DON'T Need to Update For:

These common features work with current headers:

| Feature | Why It Works |
|---------|-------------|
| **JWT Authentication** | Uses `Authorization` header ✅ |
| **OAuth2 Login** | Uses `Origin` and standard headers ✅ |
| **REST API calls** | Uses `Content-Type` and `Accept` ✅ |
| **Form submissions** | Uses `Content-Type: application/json` ✅ |
| **File downloads** | Uses `Accept` header ✅ |
| **Basic file uploads** | Uses `Content-Type: multipart/form-data` ✅ |
| **AJAX requests** | Uses `X-Requested-With` ✅ |
| **Password reset** | Standard POST with JSON ✅ |
| **Email verification** | Standard GET/POST ✅ |
| **User profile updates** | Uses `Content-Type` and `Authorization` ✅ |

### ⚠️ You NEED to Update For:

Add these headers if implementing these features:

#### 1. **Advanced File Upload with Metadata**

```java
// Add these headers:
"Content-Disposition",  // File name and metadata
"Content-Length",       // File size
"X-File-Name"          // Custom file name header (if using)
```

**Example scenario:**
- Drag-and-drop file uploads
- Resume uploads
- Chunked file uploads
- Custom file metadata

#### 2. **API Versioning via Headers**

```java
// Add this header:
"X-API-Version"  // API version specification
```

**Example scenario:**
- Multiple API versions (v1, v2)
- Gradual API migration
- A/B testing different API versions

#### 3. **API Keys or Custom Authentication**

```java
// Add these headers:
"X-API-Key",      // API key authentication
"X-Client-ID",    // Client identification
"X-Client-Secret" // Client secret (if needed)
```

**Example scenario:**
- Third-party API integrations
- Service-to-service authentication
- Mobile app API keys

#### 4. **Custom Caching Control**

```java
// Add these headers:
"Cache-Control",  // Client cache directives
"ETag",          // Resource versioning
"If-None-Match"  // Conditional requests
```

**Example scenario:**
- Aggressive caching strategies
- Offline-first web apps
- Progressive Web Apps (PWA)

#### 5. **GraphQL**

```java
// Add these headers:
"GraphQL-Preflight",  // GraphQL preflight
"X-GraphQL-Token"     // GraphQL-specific auth (if separate from JWT)
```

**Example scenario:**
- Migrating from REST to GraphQL
- Hybrid REST/GraphQL architecture

#### 6. **WebSocket Connections**

```java
// Add these headers:
"Sec-WebSocket-Protocol",    // WebSocket subprotocol
"Sec-WebSocket-Extensions",  // WebSocket extensions
"Upgrade",                   // Protocol upgrade
"Connection"                 // Connection management
```

**Example scenario:**
- Real-time chat
- Live notifications
- Collaborative editing

#### 7. **Custom Request Tracking**

```java
// Add these headers:
"X-Request-ID",     // Request tracing
"X-Correlation-ID", // Cross-service correlation
"X-Session-ID"      // Session tracking
```

**Example scenario:**
- Distributed tracing
- Logging correlation
- Debugging production issues

#### 8. **Content Security**

```java
// Add these headers:
"X-CSRF-Token",           // CSRF protection
"X-Frame-Options",        // Clickjacking protection
"X-Content-Type-Options"  // MIME sniffing protection
```

**Example scenario:**
- Enhanced CSRF protection
- Iframe embedding controls
- Additional security hardening

---

## How to Update

### Step 1: Identify the Need

Ask yourself:
1. Does my new feature send custom HTTP headers?
2. Does the browser reject my requests with CORS errors?
3. Do I see "Header X is not allowed by Access-Control-Allow-Headers" in console?

If yes to any → You need to add headers!

### Step 2: Update SecurityConfig.java

**Find this section:**
```java
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    "Accept",
    "Origin",
    "X-Requested-With"
));
```

**Add your new headers:**
```java
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    "Accept",
    "Origin",
    "X-Requested-With",
    // NEW HEADERS for your feature
    "X-API-Version",      // Example: API versioning
    "Content-Disposition" // Example: File metadata
));
```

### Step 3: Restart Backend

```bash
# Stop backend (Ctrl+C)
# Rebuild
cd ai-docs-assistant
mvn clean install
# Restart
mvn spring-boot:run
```

### Step 4: Test

```javascript
// Frontend test
fetch('http://localhost:8080/api/endpoint', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json',
    'X-API-Version': 'v2'  // Your new header
  },
  body: JSON.stringify(data)
});
```

Check browser console for CORS errors. If none → Success! ✅

---

## Common Mistakes to Avoid

### ❌ Don't Do This:

```java
// DON'T go back to wildcard!
configuration.setAllowedHeaders(Arrays.asList("*"));
```

**Why:** Reopens the security issue we just fixed.

### ❌ Don't Do This:

```java
// DON'T add headers "just in case"
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    // ... 50 headers you don't actually use
));
```

**Why:** Unnecessary headers = larger attack surface.

### ✅ Do This Instead:

```java
// Only add headers you ACTUALLY use
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",     // ✅ Using for JWT
    "Content-Type",      // ✅ Using for JSON
    "Accept",           // ✅ Using for responses
    "Origin",           // ✅ Required for CORS
    "X-Requested-With", // ✅ Using for AJAX
    "X-API-Version"     // ✅ Just added for API versioning
));
```

---

## Testing Your CORS Configuration

### Test 1: Verify Current Headers Work

```bash
# In browser console (after logging in)
fetch('http://localhost:8080/jwt-try', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('jwt_token'),
    'Content-Type': 'application/json'
  }
})
.then(r => r.json())
.then(d => console.log('✅ Current headers work:', d))
.catch(e => console.error('❌ CORS error:', e));
```

### Test 2: Verify Unauthorized Header is Blocked

```bash
# This SHOULD fail
fetch('http://localhost:8080/jwt-try', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('jwt_token'),
    'X-Custom-Unauthorized-Header': 'test'  // Not in allowed list
  }
})
.then(r => console.log('❌ Should have been blocked!'))
.catch(e => console.log('✅ Correctly blocked:', e));
```

Expected: Browser blocks it with CORS error before request reaches server.

### Test 3: After Adding New Header

```bash
# After adding "X-API-Version" to allowed headers
fetch('http://localhost:8080/api/endpoint', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('jwt_token'),
    'X-API-Version': 'v2'  // Newly allowed header
  }
})
.then(r => r.json())
.then(d => console.log('✅ New header works:', d))
.catch(e => console.error('❌ Still blocked:', e));
```

---

## Quick Reference

### Currently Allowed (Default Setup)

✅ Authorization
✅ Content-Type
✅ Accept
✅ Origin
✅ X-Requested-With

### Commonly Added Later

- Content-Disposition (file uploads)
- X-API-Version (API versioning)
- X-Request-ID (request tracking)
- Cache-Control (caching strategies)

### Never Allow

❌ `"*"` (wildcard - too permissive)
❌ Headers you don't actually use
❌ Security-sensitive headers without proper validation

---

## Summary

**Bottom Line:**
- ✅ Current headers are sufficient for 90% of web apps
- ✅ Only add headers when you implement features that need them
- ✅ Test after adding to ensure they work
- ✅ Keep the list minimal for security

**When in doubt:**
1. Try your feature
2. Check browser console for CORS errors
3. If error mentions specific header → Add that header
4. If no error → You're good!

**Last updated:** December 24, 2025

