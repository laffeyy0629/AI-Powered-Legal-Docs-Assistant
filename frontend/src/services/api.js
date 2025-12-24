// API Service for communication with Spring Boot backend

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiService {
  constructor() {
    this.baseURL = API_BASE_URL;
    this.token = localStorage.getItem('jwt_token');
  }

  // Token management
  setToken(token) {
    this.token = token;
    localStorage.setItem('jwt_token', token);
    console.log('Token saved:', token ? 'Yes' : 'No');
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
  }

  isAuthenticated() {
    // Always check localStorage for most recent token
    const token = localStorage.getItem('jwt_token');
    this.token = token;
    console.log('isAuthenticated:', !!token);
    return !!token;
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
            // Unauthorized - invalid credentials or expired token
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

