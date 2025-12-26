// API Service for communication with Spring Boot backend

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiService {
  constructor() {
    this.baseURL = API_BASE_URL;
    this.token = localStorage.getItem('jwt_token');
    this.refreshTimer = null;
    this.isRefreshing = false;
    this.refreshPromise = null;

    // Start token refresh timer if token exists
    if (this.token) {
      this.startTokenRefreshTimer();
    }
  }

  // Token management
  setToken(token) {
    this.token = token;
    localStorage.setItem('jwt_token', token);
    console.log('Token saved:', token ? 'Yes' : 'No');

    // Start/restart token refresh timer
    this.startTokenRefreshTimer();
  }

  getToken() {
    // Always get fresh token from localStorage
    this.token = localStorage.getItem('jwt_token');
    console.log('getToken:', this.token ? 'Token exists' : 'No token');
    return this.token;
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('jwt_token');
    console.log('Token cleared');

    // Stop refresh timer
    this.stopTokenRefreshTimer();
  }

  isAuthenticated() {
    // Always check localStorage for most recent token
    const token = localStorage.getItem('jwt_token');
    this.token = token;
    console.log('isAuthenticated:', !!token);
    return !!token;
  }

  // Decode JWT token to get expiration time
  decodeToken(token) {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      return JSON.parse(jsonPayload);
    } catch (error) {
      console.error('Failed to decode token:', error);
      return null;
    }
  }

  // Start automatic token refresh timer
  startTokenRefreshTimer() {
    // Clear existing timer
    this.stopTokenRefreshTimer();

    const token = this.getToken();
    if (!token) return;

    // Decode token to get expiration
    const decoded = this.decodeToken(token);
    if (!decoded || !decoded.exp) {
      console.warn('Could not decode token expiration');
      return;
    }

    // Calculate time until token expires
    const expiresAt = decoded.exp * 1000; // Convert to milliseconds
    const now = Date.now();
    const timeUntilExpiry = expiresAt - now;

    // Refresh token 2 minutes before expiry (or immediately if less than 2 minutes left)
    const refreshIn = Math.max(timeUntilExpiry - (2 * 60 * 1000), 0);

    console.log(`[TOKEN REFRESH] Timer scheduled in ${Math.round(refreshIn / 1000)} seconds`);

    this.refreshTimer = setTimeout(() => {
      console.log('[TOKEN REFRESH] Timer triggered - calling refreshAccessToken()');
      this.refreshAccessToken('timer');
    }, refreshIn);
  }

  // Stop token refresh timer
  stopTokenRefreshTimer() {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  // Refresh access token using refresh token
  async refreshAccessToken(source = 'unknown') {
    // Prevent multiple simultaneous refresh attempts - return existing promise
    if (this.isRefreshing && this.refreshPromise) {
      console.log(`[TOKEN REFRESH] Already in progress (requested by: ${source}), waiting for completion...`);
      return this.refreshPromise;
    }

    console.log(`[TOKEN REFRESH] Starting refresh (triggered by: ${source})`);
    this.isRefreshing = true;

    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      console.error('[TOKEN REFRESH] No refresh token available');
      this.isRefreshing = false;
      this.clearToken();
      window.location.href = '/login';
      return;
    }

    // Create the refresh promise
    this.refreshPromise = (async () => {
      try {
        console.log(`[TOKEN REFRESH] Making fetch call to /user/refresh`);

        // Make direct fetch call without using this.post() to avoid recursive issues
        // Do NOT include Authorization header for refresh endpoint
        const url = `${this.baseURL}/user/refresh`;
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            // NO Authorization header - refresh endpoint doesn't need access token
          },
          body: JSON.stringify({ refresh_token: refreshToken })
        });

        if (!response.ok) {
          throw new Error(`Refresh failed with status: ${response.status}`);
        }

        const data = await response.json();

        if (data.success && data.access_token) {
          console.log('[TOKEN REFRESH] Success! Updating tokens...');

          // Update access token (this will also restart the refresh timer)
          this.setToken(data.access_token);

          // Update refresh token if a new one was provided (token rotation)
          if (data.refresh_token) {
            localStorage.setItem('refresh_token', data.refresh_token);
            console.log('[TOKEN REFRESH] Refresh token rotated');
          }

          console.log(`[TOKEN REFRESH] Complete (source: ${source})`);
          return data.access_token;
        } else {
          throw new Error('Token refresh failed - invalid response');
        }
      } catch (error) {
        console.error(`[TOKEN REFRESH] Error (source: ${source}):`, error);

        // Clear tokens and redirect to login
        this.clearToken();
        localStorage.removeItem('refresh_token');
        window.location.href = '/login';
        throw error;
      } finally {
        // Reset refresh state
        console.log(`[TOKEN REFRESH] Cleaning up refresh state`);
        this.isRefreshing = false;
        this.refreshPromise = null;
      }
    })();

    return this.refreshPromise;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...(this.token && { Authorization: `Bearer ${this.token}` }),
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);

      // Handle different HTTP status codes
      if (!response.ok) {
        // Try to parse error response
        let errorData;
        try {
          errorData = await response.json();
        } catch {
          errorData = { success: false, message: 'An unexpected error occurred' };
        }

        // Handle specific status codes
        switch (response.status) {
          case 400:
            // Bad Request - validation errors
            throw new Error(errorData.message || 'Invalid request. Please check your input.');
          case 401:
            // Unauthorized - try to refresh token and retry
            if (!options._retry && endpoint !== '/user/refresh' && endpoint !== '/user/login') {
              console.log(`[401 HANDLER] Unauthorized for ${endpoint} - attempting token refresh...`);
              try {
                await this.refreshAccessToken('401-handler');
                // Retry the original request with new token
                console.log(`[401 HANDLER] Retrying original request: ${endpoint}`);
                return this.request(endpoint, { ...options, _retry: true });
              } catch (refreshError) {
                console.error('[401 HANDLER] Token refresh failed:', refreshError);
                this.clearToken();
                throw new Error(errorData.message || 'Authentication failed. Please login again.');
              }
            }
            // If already retried or refresh endpoint, just fail
            this.clearToken();
            throw new Error(errorData.message || 'Authentication failed. Please login again.');
          case 403:
            // Forbidden - insufficient permissions
            throw new Error(errorData.message || 'You do not have permission to access this resource.');
          case 404:
            // Not Found
            throw new Error(errorData.message || 'The requested resource was not found.');
          case 409:
            // Conflict - e.g., user already exists
            throw new Error(errorData.message || 'This resource already exists.');
          case 500:
            // Internal Server Error
            throw new Error(errorData.message || 'A server error occurred. Please try again later.');
          default:
            throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
        }
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // GET request
  async get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  // POST request
  async post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // PUT request
  async put(endpoint, data) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  // DELETE request
  async delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  }

  // Authentication endpoints
  async signup(userData) {
    const response = await this.post('/user/signup', userData);
    return response;
  }

  async login(credentials) {
    const response = await this.post('/user/login', credentials);
    if (response.success && response.access_token) {
      this.setToken(response.access_token);
      // Store refresh token separately
      if (response.refresh_token) {
        localStorage.setItem('refresh_token', response.refresh_token);
      }
    }
    return response;
  }

  async logout() {
    try {
      // Get tokens before clearing
      const accessToken = this.getToken();
      const refreshToken = localStorage.getItem('refresh_token');

      console.log('Logout initiated');
      console.log('Access token:', accessToken ? 'Present' : 'None');
      console.log('Refresh token:', refreshToken ? 'Present' : 'None');

      // Call backend logout endpoint to invalidate tokens
      if (accessToken || refreshToken) {
        console.log('Calling backend logout endpoint...');
        const response = await this.post('/user/logout', {
          access_token: accessToken,
          refresh_token: refreshToken
        });
        console.log('Logout response:', response);
      } else {
        console.log('No tokens to invalidate');
      }
    } catch (error) {
      console.error('Logout error:', error);
      // Continue with logout even if backend call fails
    } finally {
      // Clear tokens from localStorage
      console.log('Clearing tokens from localStorage');
      this.clearToken();
      localStorage.removeItem('refresh_token');
      // Redirect to home
      console.log('Redirecting to home page');
      window.location.href = '/';
    }
  }

  // OAuth2 - Redirects to Google OAuth
  initiateGoogleLogin() {
    window.location.href = `${this.baseURL}/oauth2/authorization/google`;
  }

  // Test protected endpoint
  async testProtectedEndpoint() {
    return this.get('/jwt-try');
  }

  // Email verification
  async resendVerification(email) {
    return this.post('/user/resend-verification', { email });
  }

  // Get user profile
  async getUserProfile() {
    return this.get('/user/profile');
  }

  // OAuth linking
  async getOAuthLinkInfo(email, provider, sessionId) {
    return this.post('/user/oauth-link-info', {
      email,
      provider,
      session_id: sessionId
    });
  }

  async confirmOAuthLink(email, provider, sessionId, consent) {
    return this.post('/user/confirm-oauth-link', {
      email,
      provider,
      session_id: sessionId,
      consent
    });
  }

  async unlinkOAuth(password, provider) {
    return this.post('/user/unlink-oauth', {
      password,
      provider
    });
  }

  // Other API endpoints
  async getStatus() {
    return this.get('/api/status');
  }

  async getHome() {
    return this.get('/');
  }
}

export default new ApiService();

