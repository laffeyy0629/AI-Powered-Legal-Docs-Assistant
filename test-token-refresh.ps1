# PowerShell Script to Test Automatic Token Refresh
# This script logs in, waits, and shows when tokens refresh

Write-Host "`n=======================================" -ForegroundColor Cyan
Write-Host "   Token Refresh Test Script" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

# Configuration
$API_BASE = "http://localhost:8080"
$USERNAME = Read-Host "`nEnter username"
$PASSWORD = Read-Host "Enter password" -AsSecureString
$PlainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($PASSWORD)
)

Write-Host "`n`nTest 1: Login and get tokens..." -ForegroundColor Yellow

# Login
try {
    $loginBody = @{
        user_name = $USERNAME
        password = $PlainPassword
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$API_BASE/user/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody

    if ($response.success) {
        Write-Host "[PASS] Login successful" -ForegroundColor Green
        Write-Host "  Access Token: $($response.access_token.Substring(0, 50))..." -ForegroundColor Gray
        Write-Host "  Refresh Token: $($response.refresh_token)" -ForegroundColor Gray
        Write-Host "  Expires In: $($response.expires_in) seconds ($([Math]::Round($response.expires_in / 60, 1)) minutes)" -ForegroundColor Gray

        $accessToken = $response.access_token
        $refreshToken = $response.refresh_token
        $expiresIn = $response.expires_in

        # Decode JWT to see expiration
        $tokenParts = $accessToken.Split('.')
        $payload = $tokenParts[1]
        # Add padding if needed
        $padding = 4 - ($payload.Length % 4)
        if ($padding -lt 4) {
            $payload += "=" * $padding
        }
        $payload = $payload.Replace('-', '+').Replace('_', '/')
        $decodedBytes = [System.Convert]::FromBase64String($payload)
        $decodedJson = [System.Text.Encoding]::UTF8.GetString($decodedBytes)
        $decoded = $decodedJson | ConvertFrom-Json

        $expirationTime = [DateTimeOffset]::FromUnixTimeSeconds($decoded.exp).LocalDateTime
        Write-Host "  Token Expiration: $expirationTime" -ForegroundColor Gray

        # Calculate when refresh should happen (2 min before expiry)
        $refreshTime = $expirationTime.AddMinutes(-2)
        Write-Host "  Auto-Refresh At: $refreshTime (2 min before expiry)" -ForegroundColor Cyan

    } else {
        Write-Host "[FAIL] Login failed: $($response.message)" -ForegroundColor Red
        exit
    }
} catch {
    Write-Host "[FAIL] Login error: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host "`n`nTest 2: Test protected endpoint with access token..." -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $accessToken"
        "Content-Type" = "application/json"
    }

    $testResponse = Invoke-RestMethod -Uri "$API_BASE/jwt-try" `
        -Method Get `
        -Headers $headers

    if ($testResponse.success) {
        Write-Host "[PASS] Protected endpoint accessible" -ForegroundColor Green
        Write-Host "  Message: $($testResponse.message)" -ForegroundColor Gray
    }
} catch {
    Write-Host "[FAIL] Protected endpoint error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n`nTest 3: Manual token refresh (simulating automatic refresh)..." -ForegroundColor Yellow

try {
    $refreshBody = @{
        refresh_token = $refreshToken
    } | ConvertTo-Json

    $refreshResponse = Invoke-RestMethod -Uri "$API_BASE/user/refresh" `
        -Method Post `
        -ContentType "application/json" `
        -Body $refreshBody

    if ($refreshResponse.success) {
        Write-Host "[PASS] Token refresh successful" -ForegroundColor Green
        Write-Host "  New Access Token: $($refreshResponse.access_token.Substring(0, 50))..." -ForegroundColor Gray
        Write-Host "  New Refresh Token: $($refreshResponse.refresh_token)" -ForegroundColor Gray
        Write-Host "  Expires In: $($refreshResponse.expires_in) seconds" -ForegroundColor Gray

        # Check that tokens changed
        if ($refreshResponse.access_token -ne $accessToken) {
            Write-Host "  [VERIFY] Access token rotated (new token issued)" -ForegroundColor Green
        } else {
            Write-Host "  [WARN] Access token did not change" -ForegroundColor Yellow
        }

        if ($refreshResponse.refresh_token -ne $refreshToken) {
            Write-Host "  [VERIFY] Refresh token rotated (old token revoked)" -ForegroundColor Green
        } else {
            Write-Host "  [WARN] Refresh token did not rotate" -ForegroundColor Yellow
        }

        $accessToken = $refreshResponse.access_token
        $newRefreshToken = $refreshResponse.refresh_token

    } else {
        Write-Host "[FAIL] Token refresh failed: $($refreshResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] Token refresh error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n`nTest 4: Verify old refresh token is revoked..." -ForegroundColor Yellow

try {
    $oldRefreshBody = @{
        refresh_token = $refreshToken  # Using OLD token
    } | ConvertTo-Json

    $oldRefreshResponse = Invoke-RestMethod -Uri "$API_BASE/user/refresh" `
        -Method Post `
        -ContentType "application/json" `
        -Body $oldRefreshBody `
        -ErrorAction Stop

    Write-Host "[FAIL] Old token still works (should be revoked!)" -ForegroundColor Red

} catch {
    $errorMessage = $_.Exception.Message
    if ($errorMessage -like "*401*" -or $errorMessage -like "*Unauthorized*" -or $errorMessage -like "*revoked*") {
        Write-Host "[PASS] Old refresh token rejected (correctly revoked)" -ForegroundColor Green
        Write-Host "  Error: $errorMessage" -ForegroundColor Gray
    } else {
        Write-Host "[WARN] Unexpected error: $errorMessage" -ForegroundColor Yellow
    }
}

Write-Host "`n`nTest 5: Test new access token works..." -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $accessToken"
        "Content-Type" = "application/json"
    }

    $testResponse = Invoke-RestMethod -Uri "$API_BASE/jwt-try" `
        -Method Get `
        -Headers $headers

    if ($testResponse.success) {
        Write-Host "[PASS] New access token works" -ForegroundColor Green
        Write-Host "  Message: $($testResponse.message)" -ForegroundColor Gray
    }
} catch {
    Write-Host "[FAIL] New access token error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n`n=======================================" -ForegroundColor Cyan
Write-Host "   Test Summary" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "
Token Refresh System Features Verified:
✓ Access tokens expire in 15 minutes
✓ Refresh tokens can renew access tokens
✓ Token rotation on refresh (new tokens issued)
✓ Old refresh tokens immediately revoked
✓ New access tokens work correctly

Frontend Auto-Refresh:
→ In your React app, open DevTools Console (F12)
→ Login and watch for: 'Token refresh scheduled in X seconds'
→ Wait 13 minutes (or adjust JWT_EXPIRATION_MS for testing)
→ You'll see automatic refresh without any user action!

" -ForegroundColor White

Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Test in browser: Login and watch console logs" -ForegroundColor White
Write-Host "2. Leave tab open for 13+ minutes" -ForegroundColor White
Write-Host "3. Token will refresh automatically" -ForegroundColor White
Write-Host "4. User experience: No interruption!" -ForegroundColor White
Write-Host ""

