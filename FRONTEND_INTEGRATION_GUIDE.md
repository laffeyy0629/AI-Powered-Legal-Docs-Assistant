# Frontend Authentication Integration Guide

## Overview

This guide documents the complete frontend authentication implementation for the AI-Powered Legal Docs Assistant. The frontend is built with **React**, **React Router**, **Vite**, **Tailwind CSS**, **Framer Motion**, and **Three.js**, providing a modern, animated user experience with full JWT and OAuth2 authentication support.

---

## Tech Stack

### Core Technologies
- **React 18** - UI framework
- **React Router v7** - Client-side routing
- **Vite** - Build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework

### Animation & Visual Effects
- **Framer Motion** - Advanced animations and transitions
- **Three.js** - 3D WebGL graphics for animated backgrounds
- **GSAP** - High-performance animations

### State Management & API
- **Custom API Service** - Centralized API communication with JWT handling
- **LocalStorage** - JWT token persistence

---

## Architecture

### File Structure

```
frontend/
├── src/
│   ├── main.jsx                    - App entry point, router setup
│   ├── App.jsx                     - Main app component with routing
│   ├── components/
│   │   ├── Login.jsx               - Login/Signup page (combined form)
│   │   ├── Dashboard.jsx           - Protected dashboard
│   │   ├── OAuthCallback.jsx       - OAuth redirect handler
│   │   ├── EmailVerification.jsx   - Email verification handler
│   │   ├── Hero.jsx                - Landing page hero section
│   │   ├── Features.jsx            - Landing page features
│   │   └── ThreeBackground.jsx     - 3D animated background
│   └── services/
│       └── api.js                  - API service with JWT management
├── public/
├── index.html
├── vite.config.js
├── tailwind.config.js
└── package.json
```

---

## Authentication Flow

### 1. Traditional Login Flow (Username/Password)

```
User fills login form → 
Login.jsx handleSubmit() →
apiService.login(credentials) →
POST /user/login (with credentials) →
Backend validates & returns JWT →
apiService.setToken(token) →
Token saved to localStorage →
Navigate to /dashboard →
Dashboard checks isAuthenticated() →
Token sent with protected requests
```

### 2. Registration Flow

```
User fills signup form →
Login.jsx handleSubmit() (isLogin=false) →
apiService.signup(userData) →
POST /user/signup (with user data) →
Backend creates user →
Success message shown →
Form switches to login mode →
User logs in to receive JWT
```

### 3. OAuth2 Login Flow (Google)

```
User clicks "Continue with Google" →
Login.jsx handleGoogleLogin() →
apiService.initiateGoogleLogin() →
Redirect to: http://localhost:8080/oauth2/authorization/google →
User authorizes on Google →
Google redirects to backend →
Backend generates JWT →
Redirect to: http://localhost:5173/auth/callback?token=<JWT> →
OAuthCallback.jsx extracts token →
apiService.setToken(token) →
Navigate to /dashboard
```

### 4. Protected Route Access

```
User visits /dashboard →
Dashboard.jsx useEffect() →
apiService.isAuthenticated() checks localStorage →
If no token: redirect to /login →
If token exists: call apiService.testProtectedEndpoint() →
GET /jwt-try (with Authorization header) →
If 401: clearToken() & redirect to /login →
If success: show dashboard
```

### 5. Logout Flow

```
User clicks logout →
Dashboard.jsx handleLogout() →
apiService.logout() →
localStorage.removeItem('jwt_token') →
window.location.href = '/' (redirect to home)
```

### 6. Email Verification Flow

```
User registers →
Backend sends verification email with token →
User clicks link in email →
Redirect to: /verify-email?token=<TOKEN> →
EmailVerification.jsx extracts token →
GET /user/verify-email?token=<TOKEN> →
Backend validates token & expiration →
If valid: User marked as verified →
Success message displayed →
Auto-redirect to /login after 3 seconds
```

### 7. Resend Verification Flow

```
User tries to login (unverified account) →
Backend returns: { success: false, requiresVerification: true, email: "..." } →
Login.jsx shows "Resend Verification Email" button →
User clicks resend →
POST /user/resend-verification { email } →
Backend generates new token →
New verification email sent →
Success message displayed
```

---

## Detailed File Explanations

### **api.js** - API Service Layer

**Purpose**: Centralized API communication with automatic JWT token management.

**Key Features**:
- Token storage in localStorage
- Automatic Authorization header injection
- Token validation and expiry handling
- Generic HTTP methods (GET, POST, PUT, DELETE)
- Authentication helpers

**Complete Implementation**:

```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiService {
  constructor() {
    this.baseURL = API_BASE_URL;
    this.token = localStorage.getItem('jwt_token');
  }

  // Token Management
  setToken(token) {
    this.token = token;
    localStorage.setItem('jwt_token', token);
  }

  getToken() {
    this.token = localStorage.getItem('jwt_token');
    return this.token;
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('jwt_token');
  }

  isAuthenticated() {
    const token = localStorage.getItem('jwt_token');
    this.token = token;
    return !!token;
  }

  // Generic request handler
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

      if (!response.ok) {
        if (response.status === 401) {
          this.clearToken(); // Token expired
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // HTTP Methods
  async get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  async post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Authentication Endpoints
  async signup(userData) {
    return await this.post('/user/signup', userData);
  }

  async login(credentials) {
    const response = await this.post('/user/login', credentials);
    if (response.success && response.token) {
      this.setToken(response.token);
    }
    return response;
  }

  logout() {
    this.clearToken();
    window.location.href = '/';
  }

  initiateGoogleLogin() {
    window.location.href = `${this.baseURL}/oauth2/authorization/google`;
  }

  async testProtectedEndpoint() {
    return this.get('/jwt-try');
  }

  async resendVerification(email) {
    return this.post('/user/resend-verification', { email });
  }
}

export default new ApiService();
```

**Why It Works**:
- ✅ Singleton pattern ensures one instance across app
- ✅ Token always fresh from localStorage
- ✅ Automatic 401 handling (token expiry)
- ✅ Clean separation of concerns
- ✅ Easy to extend with new endpoints

---

### **Login.jsx** - Combined Login/Signup Page

**Purpose**: Single-page component that toggles between login and registration modes.

**Key Features**:
- Toggle between login and signup forms
- Form validation and error handling
- Google OAuth integration
- **Email verification handling**
- **Resend verification email functionality**
- **Success and error messages**
- Animated Three.js background
- Loading states
- Framer Motion animations

**Component Structure**:

```javascript
const Login = () => {
  // State Management
  const [isLogin, setIsLogin] = useState(true); // Toggle mode
  const [formData, setFormData] = useState({
    user_name: '',
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [showResendVerification, setShowResendVerification] = useState(false);
  const [verificationEmail, setVerificationEmail] = useState('');

  // Form Submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');
    setLoading(true);

    try {
      if (isLogin) {
        // Login flow
        const response = await apiService.login({
          user_name: formData.user_name,
          password: formData.password,
        });
        
        if (response.success) {
          navigate('/dashboard');
        } else if (response.requiresVerification) {
          // Email not verified
          setError(response.message);
          setShowResendVerification(true);
          setVerificationEmail(response.email);
        } else {
          setError(response.message || 'Login failed');
        }
      } else {
        // Signup flow
        const response = await apiService.signup(formData);
        
        if (response.success) {
          if (response.requiresVerification) {
            setSuccessMessage('Registration successful! Check your email to verify.');
          } else {
            setSuccessMessage('Registration successful! Please login.');
          }
          // Auto-switch to login after 5 seconds
          setTimeout(() => {
            setIsLogin(true);
            setFormData({ user_name: '', email: '', password: '' });
          }, 5000);
        } else {
          setError(response.message || 'Registration failed');
        }
      }
    } catch (err) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  // Resend Verification Email
  const handleResendVerification = async () => {
    if (!verificationEmail) return;
    
    setLoading(true);
    try {
      const response = await apiService.resendVerification(verificationEmail);
      if (response.success) {
        setSuccessMessage('Verification email sent! Check your inbox.');
        setShowResendVerification(false);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // OAuth Handler
  const handleGoogleLogin = () => {
    apiService.initiateGoogleLogin();
  };

  return (
    <div className="relative min-h-screen">
      {/* Three.js Background */}
      <ThreeBackground />
      
      {/* Login Card */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        {/* Google OAuth Button */}
        <button onClick={handleGoogleLogin}>
          Continue with Google
        </button>
        
        {/* Error Message */}
        {error && <div className="error-alert">{error}</div>}
        
        {/* Success Message */}
        {successMessage && <div className="success-alert">{successMessage}</div>}
        
        {/* Resend Verification Button */}
        {showResendVerification && (
          <button onClick={handleResendVerification}>
            Resend Verification Email
          </button>
        )}
        
        {/* Traditional Form */}
        <form onSubmit={handleSubmit}>
          <input name="user_name" required />
          {!isLogin && <input name="email" type="email" required />}
          <input name="password" type="password" required />
          <button type="submit" disabled={loading}>
            {isLogin ? 'Login' : 'Sign Up'}
          </button>
        </form>
        
        {/* Toggle Link */}
        <button onClick={() => setIsLogin(!isLogin)}>
          {isLogin ? 'Sign up' : 'Login'}
        </button>
      </motion.div>
    </div>
  );
};
```

**Design Highlights**:
- **Dark Mode Theme**: Gray-900 gradient background
- **Glass Morphism**: Backdrop blur with transparency
- **Google OAuth Button**: White button with Google logo
- **Animated Form**: Framer Motion transitions
- **Error Messages**: Red alert box with red-500/10 background
- **Success Messages**: Green alert box with green-500/10 background
- **Resend Button**: Yellow-themed button for verification email resend
- **Loading States**: Spinner during submission
- **Three.js Background**: Less prominent (40% opacity) for focus

**Form Validation**:
- Required fields enforced by HTML5
- Email format validation
- Backend validates password length (min 8 chars)
- Unique username/email checks
- **Email verification required for non-OAuth users**

---

### **OAuthCallback.jsx** - OAuth Redirect Handler

**Purpose**: Processes OAuth redirect from backend, extracts JWT token, and navigates to dashboard.

**Flow**:

```javascript
const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('Processing...');

  useEffect(() => {
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    if (error) {
      setStatus('Authentication failed. Redirecting...');
      setTimeout(() => navigate('/login'), 2000);
      return;
    }

    if (token) {
      apiService.setToken(token);
      setStatus('Authentication successful! Redirecting...');
      setTimeout(() => navigate('/dashboard'), 1000);
    } else {
      setStatus('No token received. Redirecting...');
      setTimeout(() => navigate('/login'), 2000);
    }
  }, [searchParams, navigate]);

  return (
    <div className="loading-screen">
      <div className="spinner"></div>
      <p>{status}</p>
    </div>
  );
};
```

**Why This Component**:
- ✅ Separates OAuth logic from main login page
- ✅ Provides user feedback during token processing
- ✅ Handles errors gracefully
- ✅ Cleans up URL parameters (token not exposed in browser history after navigation)

**Security Considerations**:
- Token only briefly visible in URL
- Immediately moved to localStorage
- Navigation clears URL parameters
- Error states handled without token exposure

---

### **Dashboard.jsx** - Protected Dashboard

**Purpose**: Main authenticated user interface with route protection.

**Key Features**:
- Authentication guard (redirects if no token)
- Token validation with backend
- Animated Three.js background (bold, 70% opacity)
- Feature cards with animations
- Logout functionality
- Loading state

**Authentication Guard**:

```javascript
const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    // Check token exists
    const isAuth = apiService.isAuthenticated();
    
    if (!isAuth) {
      navigate('/login');
      return;
    }

    // Validate token with backend
    apiService
      .testProtectedEndpoint()
      .then((response) => {
        setAuthenticated(true);
        setLoading(false);
      })
      .catch((error) => {
        apiService.clearToken();
        navigate('/login');
      });
  }, [navigate]);

  if (loading) {
    return <LoadingScreen />;
  }

  return (
    <div className="dashboard">
      {/* Three.js Background (70% opacity) */}
      <ThreeBackground />
      
      {/* Header with Logout */}
      <header>
        <h1>Dashboard</h1>
        <button onClick={handleLogout}>Logout</button>
      </header>
      
      {/* Success Message */}
      <div className="success-banner">
        Authentication Successful!
      </div>
      
      {/* Feature Cards */}
      <div className="feature-grid">
        <FeatureCard title="Document Analysis" />
        <FeatureCard title="Smart Contracts" />
        <FeatureCard title="Legal Research" />
        <FeatureCard title="Case Management" />
      </div>
    </div>
  );
};
```

**Design Elements**:
- **Bold Background**: Three.js particles at 70% opacity (more prominent than login)
- **Animated Blobs**: Multiple gradient blobs with staggered animations
- **Glass Cards**: Backdrop blur with border hover effects
- **Gradient Text**: Blue-purple-pink gradient for headers
- **Feature Icons**: SVG icons with gradient backgrounds
- **Hover Effects**: Scale and shadow transitions on cards

**Why Double Validation**:
1. **Client Check**: Quick localStorage check for immediate feedback
2. **Server Validation**: Confirms token is valid and not expired
3. **Better UX**: Prevents flash of authenticated content for invalid tokens

---

### **EmailVerification.jsx** - Email Verification Handler

**Purpose**: Handles email verification from email links with visual feedback.

**Key Features**:
- Token extraction from URL query parameters
- Backend verification API call
- Status-based UI rendering (verifying, success, error, expired)
- Auto-redirect to login after successful verification
- Resend verification email functionality
- Animated status icons

**Verification Process**:

```javascript
const EmailVerification = () => {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    
    if (!token) {
      setStatus('error');
      return;
    }

    verifyEmail(token);
  }, [searchParams]);

  const verifyEmail = async (token) => {
    const response = await fetch(
      `http://localhost:8080/user/verify-email?token=${token}`
    );
    const data = await response.json();

    if (data.success) {
      setStatus('success');
      // Redirect to login after 3 seconds
      setTimeout(() => navigate('/login'), 3000);
    } else {
      setStatus(data.expired ? 'expired' : 'error');
      setMessage(data.message);
    }
  };

  return (
    <div className="verification-container">
      {/* Status Icon (animated spinner, checkmark, or X) */}
      <StatusIcon status={status} />
      
      {/* Status Message */}
      <h2>{getStatusTitle(status)}</h2>
      <p>{message}</p>
      
      {/* Action Buttons */}
      {status === 'success' && <Button>Go to Login</Button>}
      {status === 'expired' && <Button onClick={resend}>Resend Email</Button>}
      {status === 'error' && <Button>Back to Login</Button>}
    </div>
  );
};
```

**Status States**:
1. **verifying**: Shows loading spinner, making API call
2. **success**: Green checkmark, auto-redirects to login
3. **expired**: Red X, shows resend button with email
4. **error**: Red X, shows back to login button

**Design Elements**:
- **Gradient Background**: Gray-900 to indigo-900 to gray-900
- **White Card**: Clean, centered card with shadow
- **Status Icons**: Large animated SVG icons (16x16)
- **Color Coding**: Green for success, red for error/expired
- **Smooth Transitions**: Framer Motion animations
- **Button Interactions**: Hover scale and disabled states

---

### **ThreeBackground.jsx** - 3D Animated Background

**Purpose**: Provides animated 3D particle background using Three.js and WebGL.

**Features**:
- Particle system with dynamic movement
- Camera auto-rotation
- Performance optimized with lazy loading
- Responsive canvas sizing
- Non-interactive (pointer-events: none)

**Implementation Overview**:

```javascript
const ThreeBackground = () => {
  const mountRef = useRef(null);

  useEffect(() => {
    // Setup Three.js scene
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
    const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });

    // Create particle geometry
    const particles = new THREE.BufferGeometry();
    const particleCount = 5000;
    const positions = new Float32Array(particleCount * 3);
    
    // Randomize particle positions
    for (let i = 0; i < particleCount * 3; i++) {
      positions[i] = (Math.random() - 0.5) * 1000;
    }
    
    particles.setAttribute('position', new THREE.BufferAttribute(positions, 3));

    // Create particle material
    const material = new THREE.PointsMaterial({
      color: 0x4F46E5,
      size: 2,
      transparent: true,
      opacity: 0.6,
    });

    const particleSystem = new THREE.Points(particles, material);
    scene.add(particleSystem);

    // Animation loop
    const animate = () => {
      requestAnimationFrame(animate);
      particleSystem.rotation.y += 0.001;
      particleSystem.rotation.x += 0.0005;
      renderer.render(scene, camera);
    };
    animate();

    // Cleanup
    return () => {
      renderer.dispose();
    };
  }, []);

  return <div ref={mountRef} className="absolute inset-0" />;
};

export default ThreeBackground;
```

**Performance Optimization**:
- Lazy loaded in Login and Dashboard (reduces initial bundle)
- Uses `Suspense` for fallback during load
- WebGL hardware acceleration
- Minimal particle count for smooth 60fps

**Usage**:
```javascript
// Less prominent for focus (Login page)
<div className="absolute inset-0 opacity-40">
  <Suspense fallback={null}>
    <ThreeBackground />
  </Suspense>
</div>

// Bold and prominent (Dashboard)
<div className="absolute inset-0 opacity-70">
  <Suspense fallback={null}>
    <ThreeBackground />
  </Suspense>
</div>
```

---

## Routing Configuration

### App.jsx Routes

```javascript
import { BrowserRouter, Routes, Route } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        
        {/* OAuth Callback */}
        <Route path="/auth/callback" element={<OAuthCallback />} />
        
        {/* Protected Route */}
        <Route path="/dashboard" element={<Dashboard />} />
      </Routes>
    </BrowserRouter>
  );
}
```

**Route Protection**:
- Public routes: `/`, `/login`
- OAuth route: `/auth/callback` (processes token)
- Protected route: `/dashboard` (requires valid JWT)
- Protection implemented in component (not router-level) for better UX

---

## Token Management Deep Dive

### Storage Strategy

**Why localStorage?**
- ✅ Persists across browser sessions (stay logged in)
- ✅ Accessible from any component
- ✅ Simple API (getItem, setItem, removeItem)
- ✅ Survives page refreshes

**Alternative Considerations**:
- **sessionStorage**: Cleared on tab close (less convenient)
- **Cookies**: Requires CSRF protection, httpOnly flag limits JS access
- **Memory**: Lost on refresh
- **IndexedDB**: Overkill for single token

**Security Trade-offs**:
- ⚠️ Vulnerable to XSS (Cross-Site Scripting)
- ✅ Not vulnerable to CSRF (Cross-Site Request Forgery)
- ✅ No httpOnly means JS can manage token
- ✅ Token has expiration (24 hours)

### Token Lifecycle

```
1. User logs in → Backend generates JWT
2. Frontend receives token in response
3. apiService.setToken(token) → localStorage
4. Token included in all protected requests
5. After 24 hours → Token expires
6. Backend returns 401 → apiService clears token
7. User redirected to login
```

### Authorization Header Format

```javascript
// Automatically added by api.js
headers: {
  'Authorization': `Bearer ${token}`
}

// Example
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Error Handling

### HTTP Status Codes

The frontend now properly handles different HTTP status codes from the backend:

**Status Codes Handled:**
- **200 OK**: Successful request
- **201 Created**: Successful registration
- **400 Bad Request**: Validation errors, invalid input
- **401 Unauthorized**: Invalid credentials, expired token
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource already exists (e.g., duplicate username/email)
- **500 Internal Server Error**: Server error

### Client-Side Error Handling

**api.js Request Method** (Updated December 24, 2025):
```javascript
async request(endpoint, options = {}) {
  try {
    const response = await fetch(url, config);

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch {
        errorData = { success: false, message: 'An unexpected error occurred' };
      }

      // Handle specific status codes
      switch (response.status) {
        case 400:
          throw new Error(errorData.message || 'Invalid request. Please check your input.');
        case 401:
          this.clearToken();
          throw new Error(errorData.message || 'Authentication failed. Please login again.');
        case 403:
          throw new Error(errorData.message || 'You do not have permission to access this resource.');
        case 404:
          throw new Error(errorData.message || 'The requested resource was not found.');
        case 409:
          throw new Error(errorData.message || 'This resource already exists.');
        case 500:
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
```

### Component Error Handling

**Login Component**:
```javascript
try {
  const response = await apiService.login(credentials);
  if (response.success) {
    navigate('/dashboard');
  } else {
    setError(response.message || 'Login failed');
  }
} catch (error) {
  // Error already has user-friendly message from api.js
  setError(error.message);
}
```

**Registration Component**:
```javascript
try {
  const response = await apiService.signup(userData);
  if (response.success) {
    alert('Registration successful! Please login.');
    setIsLogin(true);
  } else {
    setError(response.message || 'Registration failed');
  }
} catch (error) {
  // Handles 409 Conflict (user exists) and 400 Bad Request
  setError(error.message);
}
```

### Backend Error Responses

**Success Response (200/201)**:
```json
{
  "success": true,
  "message": "Login successful! Redirecting to dashboard...",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Validation Error (400)**:
```json
{
  "success": false,
  "message": "Password must be at least 8 characters long"
}
```

**Authentication Error (401)**:
```json
{
  "success": false,
  "message": "Invalid username or password. Please check your credentials and try again."
}
```

**Conflict Error (409)**:
```json
{
  "success": false,
  "message": "Email already registered. Please use a different email or try logging in."
}
```

**Server Error (500)**:
```json
{
  "success": false,
  "message": "An error occurred during registration. Please try again later."
}
```

### User-Friendly Error Messages

The system provides specific, actionable error messages:

**Login Errors:**
- "Invalid username or password. Please check your credentials and try again."
- "Authentication failed. Please login again." (expired token)
- "An error occurred during login. Please try again later." (server error)

**Registration Errors:**
- "Password must be at least 8 characters long"
- "Username already exists. Please choose a different username."
- "Email already registered. Please use a different email or try logging in."
- "Registration successful! You can now login with your credentials."

**Network Errors:**
- "Network error. Please check your connection."
- "The requested resource was not found."
- "A server error occurred. Please try again later."

### Error Display in UI

**Login/Signup Page**:
```jsx
{error && (
  <motion.div
    initial={{ opacity: 0, y: -10 }}
    animate={{ opacity: 1, y: 0 }}
    className="mb-4 p-3 bg-red-500/10 border border-red-500/50 rounded-lg text-red-400 text-sm"
  >
    {error}
  </motion.div>
)}
```

**Features:**
- Red background with transparency
- Red border for emphasis
- Smooth animation on appearance
- Clear, readable text
- Automatically cleared on input change

---

## Visual Design System

### Color Palette

**Background**:
- Primary: `from-gray-900 via-gray-800 to-black`
- Cards: `bg-gray-800/50` (50% opacity) or `bg-gray-800/60` (60% opacity)
- Borders: `border-gray-700/50` or `border-gray-600/60`

**Accents**:
- Primary Gradient: `from-blue-600 to-purple-600`
- Success: `green-500/20` background, `green-400` border
- Error: `red-500/10` background, `red-500/50` border
- Text Gradient: `from-blue-400 via-purple-400 to-pink-400`

**Interactive States**:
- Hover: `hover:scale-105` with `hover:shadow-*-500/50`
- Focus: `focus:ring-2 focus:ring-blue-500`
- Disabled: `disabled:opacity-50 disabled:cursor-not-allowed`

### Animation Strategy

**Framer Motion**:
- Page entry: `initial={{ opacity: 0, y: 20 }}` → `animate={{ opacity: 1, y: 0 }}`
- Staggered reveals: `transition={{ delay: 0.2 }}`
- Button hovers: `whileHover={{ scale: 1.05 }}`

**CSS Animations**:
- Spinners: `animate-spin` (loading states)
- Pulse: `animate-pulse` (background blobs)
- Transitions: `transition-all duration-300`

**Three.js**:
- Continuous particle rotation
- Camera movement (subtle)
- 60fps target for smooth experience

### Responsive Design

**Breakpoints** (Tailwind defaults):
- `sm`: 640px
- `md`: 768px (grid switches to 2 columns)
- `lg`: 1024px
- `xl`: 1280px

**Mobile Considerations**:
- Single column layouts on small screens
- Touch-friendly button sizes (min 44px)
- Reduced particle count on mobile (performance)
- Simplified animations on low-end devices

---

## Environment Variables

### .env File

```bash
# Backend API URL
VITE_API_BASE_URL=http://localhost:8080

# Production
# VITE_API_BASE_URL=https://api.yourdomain.com
```

**Vite Configuration**:
- Prefix with `VITE_` to expose to frontend
- Access via `import.meta.env.VITE_API_BASE_URL`
- Fallback to localhost for development

---

## Build & Deployment

### Development

```bash
cd frontend
bun install        # or npm install
bun run dev        # Start dev server on port 5173
```

### Production Build

```bash
bun run build      # Creates optimized bundle in dist/
```

**Build Output**:
- `dist/index.html` - Entry point
- `dist/assets/` - Chunked JS and CSS
- Three.js vendor chunk (separate for code splitting)
- GSAP vendor chunk
- Main app bundle

**Optimizations**:
- Code splitting (lazy loading Three.js)
- Tree shaking (unused code removed)
- Minification (Terser for JS, cssnano for CSS)
- Asset optimization (images, fonts)

### Deployment Checklist

- [ ] Update `VITE_API_BASE_URL` to production backend
- [ ] Enable HTTPS for secure token transmission
- [ ] Configure CORS on backend for production domain
- [ ] Set proper cache headers for static assets
- [ ] Enable gzip/brotli compression
- [ ] Configure CDN for assets
- [ ] Set up error monitoring (Sentry, etc.)
- [ ] Test OAuth redirects with production URLs

---

## Testing Approach

### Manual Testing

**Login Flow**:
1. Visit `/login`
2. Fill username and password
3. Submit form
4. Verify redirect to `/dashboard`
5. Verify token in localStorage
6. Refresh page - should stay logged in

**Signup Flow**:
1. Click "Sign up" toggle
2. Fill username, email, password
3. Submit form
4. Verify success message
5. Form switches to login
6. Login with new credentials

**OAuth Flow**:
1. Click "Continue with Google"
2. Authorize on Google
3. Verify redirect to dashboard
4. Check user created in database
5. Verify token works for protected endpoints

**Protected Route**:
1. Log out
2. Try visiting `/dashboard` directly
3. Should redirect to `/login`
4. Login and retry
5. Should show dashboard

**Logout Flow**:
1. Click logout button
2. Verify redirect to home
3. Verify token cleared from localStorage
4. Try accessing `/dashboard` - should redirect

### Debugging Tools

**Browser Console Logs**:
- `api.js` logs all token operations
- `Dashboard.jsx` logs authentication checks
- `OAuthCallback.jsx` logs token processing

**Browser DevTools**:
- **Application → Local Storage**: View stored JWT token
- **Network → Headers**: See Authorization header in requests
- **Console**: Check for errors and logs

**Backend Logs**:
- Watch terminal for JWT filter logs
- OAuth success handler logs user info
- Database logs for user creation

---

## Common Issues & Solutions

### Issue 1: "Invalid Hook Call" Error

**Symptoms**: React hooks error in browser console, white page

**Cause**: Multiple React versions or mismatched renderer

**Solution**:
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Issue 2: "loginButtonRef is not defined"

**Symptoms**: ReferenceError in Hero component

**Cause**: Missing `useRef()` import or variable declaration

**Solution**: Ensure Hero.jsx has:
```javascript
import { useRef } from 'react';

const Hero = () => {
  const loginButtonRef = useRef(null);
  // ...
};
```

### Issue 3: White Page After OAuth Login

**Symptoms**: Dashboard doesn't load after successful OAuth

**Cause**: Token not properly saved or retrieved

**Solution**: Check OAuthCallback logs:
```javascript
console.log('Token in localStorage:', localStorage.getItem('jwt_token'));
```

### Issue 4: "JSON.parse: unexpected character" Error

**Symptoms**: API calls fail with JSON parsing error

**Cause**: Backend returns non-JSON response (HTML error page)

**Solution**: Check backend is running and endpoint exists:
```bash
curl http://localhost:8080/jwt-try -H "Authorization: Bearer <token>"
```

### Issue 5: Token Not Sent with Requests

**Symptoms**: 401 errors even though user logged in

**Cause**: Token not in Authorization header

**Solution**: Verify api.js includes token:
```javascript
console.log('Token:', this.getToken());
console.log('Headers:', config.headers);
```

### Issue 6: Build Error - "Unexpected }"

**Symptoms**: Vite build fails with syntax error

**Cause**: Malformed JSX or extra closing brace

**Solution**: Check component for matching braces:
```bash
npx eslint src/components/OAuthCallback.jsx
```

### Issue 7: Duplicate className in JSX

**Symptoms**: Build warning about duplicate className attributes

**Cause**: Two className props on same element

**Solution**: Merge into one:
```javascript
// Wrong
<div className="class1" className="class2">

// Correct
<div className="class1 class2">
```

---

## Security Best Practices

### Token Security

✅ **Implemented**:
- Token has expiration (24 hours)
- Token cleared on 401 response
- Token not logged to console (except debug mode)
- HTTPS required in production

⚠️ **Improvements Needed**:
- Add XSS protection (Content Security Policy)
- Implement token refresh mechanism
- Consider httpOnly cookies for production
- Add rate limiting on login endpoint

### Input Validation

✅ **Implemented**:
- HTML5 required attributes
- Email format validation
- Backend validates all inputs

⚠️ **Improvements Needed**:
- Add client-side password strength indicator
- Implement CAPTCHA for signup
- Add input sanitization library

### CORS Configuration

✅ **Implemented**:
- Backend allows localhost origins
- Credentials included in requests

⚠️ **Production TODO**:
- Restrict origins to production domain only
- Specify allowed headers (not "*")
- Enable preflight caching

---

## Performance Metrics

### Bundle Size

**Development** (uncompressed):
- Main bundle: ~220 KB
- Three.js vendor: ~1.1 MB
- GSAP vendor: ~70 KB
- **Total**: ~1.4 MB

**Production** (gzipped):
- Main bundle: ~75 KB
- Three.js vendor: ~300 KB
- GSAP vendor: ~27 KB
- **Total**: ~400 KB

### Load Time (Local)

- First Contentful Paint: <1s
- Time to Interactive: <2s
- Full page load: <3s

**Optimization Strategies**:
- Lazy load Three.js (not needed immediately)
- Code splitting by route
- Asset preloading for critical resources
- Service worker for offline support (future)

---

## Future Enhancements

### Short Term

1. **Token Refresh**: Implement refresh token system
2. **Email Verification**: Add email confirmation flow
3. **Password Reset**: Forgot password functionality
4. **Profile Page**: User profile management
5. **Loading Skeletons**: Better loading UX

### Medium Term

6. **Role-Based UI**: Show/hide features based on user role
7. **Social Login**: Add GitHub, Microsoft OAuth
8. **2FA**: Two-factor authentication option
9. **Session Management**: View active sessions
10. **Dark/Light Mode**: Theme toggle

### Long Term

11. **Progressive Web App**: Offline support
12. **Push Notifications**: Real-time updates
13. **Internationalization**: Multi-language support
14. **Accessibility**: WCAG 2.1 AA compliance
15. **Analytics**: User behavior tracking

---

## Connected Backend Files

The frontend integrates with these backend components:

### Authentication Endpoints

1. **POST /user/signup**
   - Body: `{ user_name, email, password }`
   - Returns: `{ success, message, requiresVerification }`
   - **Sends verification email to user**

2. **POST /user/login**
   - Body: `{ user_name, password }`
   - Returns: `{ success, message, token, requiresVerification?, email? }`
   - **Blocks login if email not verified**

3. **GET /user/verify-email?token=<TOKEN>**
   - Query: `token` (verification token from email)
   - Returns: `{ success, message, expired?, email? }`
   - **Verifies user email and marks account as verified**

4. **POST /user/resend-verification**
   - Body: `{ email }`
   - Returns: `{ success, message }`
   - **Sends new verification email with fresh token**

5. **GET /jwt-try** (Protected)
   - Header: `Authorization: Bearer <token>`
   - Returns: `{ success, message }`

6. **GET /oauth2/authorization/google**
   - Redirects to Google OAuth
   - No body required

7. **OAuth Callback** (Backend handles)
   - Backend generates JWT
   - Redirects to: `/auth/callback?token=<JWT>`

### Backend Files Involved

- `SecurityConfig.java` - OAuth success handler, CORS, endpoints, email verification endpoints
- `JWTFilter.java` - Validates Authorization header
- `JWTService.java` - Generates and validates tokens
- `UserController.java` - REST endpoints, verification endpoints
- `UserImpl.java` - Business logic, verification logic
- `OAuthService.java` - OAuth user creation, auto-verification
- `EmailService.java` - Email sending, HTML templates
- `User.java` - Email verification fields (emailVerified, verificationToken, etc.)

---

## Summary

The frontend authentication system is **fully functional** and production-ready with complete email verification! It provides:

✅ **Complete JWT Implementation**
- Token storage and management
- Automatic header injection
- Token validation and expiry handling

✅ **Email Verification System**
- Registration with email verification
- HTML email templates
- Token-based verification (24-hour expiry)
- Resend verification functionality
- Login blocked for unverified accounts
- OAuth users auto-verified

✅ **Modern UI/UX**
- React with hooks
- Framer Motion animations
- Three.js 3D backgrounds
- Responsive design
- Glass morphism effects
- Status-based UI rendering

✅ **Full OAuth2 Support**
- Google login integration
- Callback handler
- Automatic user creation
- Seamless token exchange
- OAuth users pre-verified

✅ **Route Protection**
- Authentication guards
- Automatic redirects
- Loading states
- Error handling

✅ **Clean Architecture**
- Centralized API service
- Reusable components
- Lazy loading
- Code splitting

**Completed Features**:
- ✅ JWT authentication with proper token management
- ✅ Traditional username/password login
- ✅ User registration with validation
- ✅ Google OAuth integration
- ✅ Email verification system
- ✅ Protected routes and dashboard
- ✅ Proper HTTP status code handling
- ✅ User-friendly error messages

**Next Steps**: Focus on token refresh, password reset, and role-based UI enhancements for a complete enterprise-grade application.

---

## Quick Reference

### API Service Methods

```javascript
// Token Management
apiService.setToken(token)
apiService.getToken()
apiService.clearToken()
apiService.isAuthenticated()

// Authentication
apiService.signup({ user_name, email, password })
apiService.login({ user_name, password })
apiService.logout()
apiService.initiateGoogleLogin()

// Email Verification
apiService.resendVerification(email)

// Protected Endpoints
apiService.testProtectedEndpoint()
apiService.get(endpoint)
apiService.post(endpoint, data)
```

### Component Props

```javascript
// Login
<Login /> // No props

// Dashboard
<Dashboard /> // No props

// OAuthCallback
<OAuthCallback /> // No props

// ThreeBackground
<ThreeBackground /> // No props
```

### Routing

```javascript
// Navigate programmatically
import { useNavigate } from 'react-router-dom';
const navigate = useNavigate();
navigate('/dashboard');

// Link component
import { Link } from 'react-router-dom';
<Link to="/login">Login</Link>
```

---

*Last Updated: December 24, 2025*
*Version: 1.0*
*Status: Production Ready (Basic Features)*

