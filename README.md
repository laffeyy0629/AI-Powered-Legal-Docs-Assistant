# AI-Powered Legal Docs Assistant

A modern, full-stack application with decoupled frontend and backend architecture for intelligent legal document processing and analysis.

## Architecture

This project uses an **API-First / Decoupled Architecture**:

```
┌─────────────────────────────┐
│   Frontend (React/Vite)     │  ← Separate codebase
│   Port: 5173                │
│   Tech: React, Tailwind,    │
│         Three.js, GSAP      │
└─────────────┬───────────────┘
              │
              │ REST API (JSON)
              ↓
┌─────────────────────────────┐
│   Backend (Spring Boot)     │  ← Separate codebase
│   Port: 8080                │
│   Tech: Java 21, Spring,    │
│         PostgreSQL          │
└─────────────┬───────────────┘
              │
              ↓
┌─────────────────────────────┐
│   Database (PostgreSQL)     │
│   Port: 5432                │
└─────────────────────────────┘
```

## Project Structure

```
AI-Powered-Legal-Docs-Assistant/
├── frontend/                    # React frontend application
│   ├── src/
│   │   ├── components/         # React components
│   │   │   ├── Login.jsx              # Login/Signup with forgot links
│   │   │   ├── Dashboard.jsx          # Protected dashboard
│   │   │   ├── Hero.jsx               # Landing page hero
│   │   │   ├── Features.jsx           # Features showcase
│   │   │   ├── EmailVerification.jsx  # Email verification page
│   │   │   ├── ForgotPassword.jsx     # Forgot password page
│   │   │   ├── ResetPassword.jsx      # Reset password page
│   │   │   ├── ForgotUsername.jsx     # Forgot username page
│   │   │   ├── OAuthCallback.jsx      # OAuth callback handler
│   │   │   └── ThreeBackground.jsx    # 3D animated background
│   │   ├── services/           # API service layer
│   │   │   └── api.js                 # API communication
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   ├── tailwind.config.js
│   └── README.md              # Frontend documentation
│
├── ai-docs-assistant/         # Spring Boot backend application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/isaqcasey/aidocsassistant/
│   │   │   │       ├── Controller/
│   │   │   │       │   ├── UserController.java
│   │   │   │       │   └── HomeController.java
│   │   │   │       ├── Service/
│   │   │   │       │   ├── UserService.java
│   │   │   │       │   ├── JWTService.java
│   │   │   │       │   ├── RefreshTokenService.java
│   │   │   │       │   ├── TokenBlacklistService.java
│   │   │   │       │   ├── EmailService.java
│   │   │   │       │   └── OAuthService.java
│   │   │   │       ├── Model/
│   │   │   │       │   ├── User.java
│   │   │   │       │   ├── RefreshToken.java
│   │   │   │       │   └── InvalidatedToken.java
│   │   │   │       ├── Repo/
│   │   │   │       │   ├── UserRepo.java
│   │   │   │       │   ├── RefreshTokenRepo.java
│   │   │   │       │   └── InvalidatedTokenRepo.java
│   │   │   │       ├── Security/
│   │   │   │       │   └── JWTFilter.java
│   │   │   │       ├── config/
│   │   │   │       │   └── SecurityConfig.java
│   │   │   │       └── Impl/
│   │   │   │           └── UserImpl.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   ├── pom.xml
│   └── mvnw
│
├── JWT-OAuth-Integration-Guide.md  # Complete auth documentation
├── ENVIRONMENT_SETUP.md            # Environment setup guide
├── QUICKSTART.md                   # Quick start guide
├── README.md                       # This file
└── start.ps1                       # Script to start both servers
```

## Quick Start

### Prerequisites

- **Java 21**
- **Maven** (included via Maven Wrapper)
- **Bun** (JavaScript runtime)
- **PostgreSQL 16** running on port 5432
- Database: `AI_docs_assistant` Create ka muna schema with this name saka mo run and build.

### Setup

**JWT Configuration (Backend):**
The `.env` file in `ai-docs-assistant/.env` contains your JWT secret key.
- ✅ Already configured with a secure random key
- ✅ Automatically loaded by `start.ps1`
- ✅ Protected by `.gitignore` (won't be committed)

**To run the application:**
```powershell
.\start.ps1
```

This will start both backend (port 8080) and frontend (port 5173) with JWT properly configured.

## Features

### 🔐 Authentication & Security
- **JWT Authentication** - Secure token-based authentication
  - Access tokens (15-minute expiration)
  - Rotating refresh tokens (7-day expiration)
  - Token blacklist for immediate logout
- **OAuth2 Login** - Google authentication integration
- **Email Verification** - Account verification with secure tokens
- **Password Reset** - Secure password recovery via email
- **Username Recovery** - Username reminder via email
- **Session Management** - Single session enforcement with token rotation
- **Security Features**:
  - BCrypt password hashing
  - One-time use tokens
  - Token reuse detection
  - Automatic token cleanup
  - Email enumeration prevention
  - OAuth user protection
  - Tokens are hashed via SHA-256

### 📧 Email Services
- **Professional HTML Templates** - Branded email designs
- **Verification Emails** - 24-hour token expiration
- **Password Reset Emails** - 1-hour token expiration
- **Username Reminder Emails** - Account recovery support
- **Scheduled Maintenance** - Automatic cleanup of expired tokens

### 🎨 User Interface
- **Modern Design** - Three.js animated backgrounds
- **Responsive Layout** - Mobile-friendly design
- **8 Complete Pages**:
  - Landing page (Hero + Features)
  - Login/Signup (unified form)
  - Dashboard (protected)
  - Email Verification
  - Forgot Password
  - Reset Password
  - Forgot Username
  - OAuth Callback Handler
- **Real-time Validation** - Instant feedback on form inputs
- **Loading States** - Professional spinners and disabled states
- **Animations** - Framer Motion transitions and effects

### 🔄 Token Management
- **Rotating Refresh Tokens** - Enhanced security with token rotation
- **Token Reuse Detection** - Automatic revocation on suspicious activity
- **Scheduled Cleanup** - Daily maintenance tasks:
  - Refresh tokens: 3:00 AM
  - Blacklisted tokens: 3:30 AM
  - Email verification tokens: 4:00 AM
  - Password reset tokens: 4:30 AM

## Tech Stack

### Frontend
- **React 19** - Modern UI library
- **Vite** - Lightning-fast build tool
- **Bun** - Fast JavaScript runtime
- **Tailwind CSS** - Utility-first CSS
- **Three.js** - 3D graphics
- **@react-three/fiber** - React renderer for Three.js
- **@react-three/drei** - Three.js helpers
- **GSAP** - Professional animations
- **Framer Motion** - Animation library

### Backend
- **Java 21** - Latest LTS
- **Spring Boot 4.0** - Framework
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database access
- **PostgreSQL** - Database
- **OAuth2** - Social authentication (Google)
- **JWT** - Stateless authentication tokens
- **BCrypt** - Password encryption
- **JavaMail** - Email service
- **Maven** - Build tool

## Documentation

- [**ENVIRONMENT_SETUP.md**](./ENVIRONMENT_SETUP.md) - Environment variables setup guide
- [**JWT-OAuth-Integration-Guide.md**](./JWT-OAuth-Integration-Guide.md) - Complete authentication system documentation
  - JWT authentication implementation
  - OAuth2 integration guide
  - Rotating refresh tokens
  - Email verification system
  - Password reset functionality
  - Username recovery
  - Token blacklist implementation
  - Security best practices
- [**frontend/README.md**](./frontend/README.md) - Frontend-specific documentation
- [**QUICKSTART.md**](./QUICKSTART.md) - Quick start guide for developers

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | API information |
| GET | `/api/status` | Check API health |
| GET | `/user/signup` | Signup instructions |
| POST | `/user/signup` | Register new user |
| POST | `/user/login` | Login with credentials |
| GET | `/user/verify-email` | Verify email with token |
| POST | `/user/resend-verification` | Resend verification email |
| POST | `/user/forgot-password` | Request password reset |
| POST | `/user/reset-password` | Reset password with token |
| POST | `/user/forgot-username` | Request username reminder |
| POST | `/user/refresh` | Refresh access token |
| POST | `/user/logout` | Logout and invalidate tokens |
| GET | `/oauth2/authorization/google` | Initiate Google OAuth login |

### Protected Endpoints (Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/jwt-try` | Test JWT authentication |
| GET | `/dashboard` | Access protected dashboard |

## Default Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend | 5173 | http://localhost:5173 |
| Backend | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | localhost:5432 |