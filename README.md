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
│   │   ├── services/           # API service layer
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
│   │   │   │       ├── Service/
│   │   │   │       ├── Model/
│   │   │   │       ├── Repo/
│   │   │   │       └── config/
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   ├── pom.xml
│   └── mvnw
│
├── SETUP_GUIDE.md             # Complete setup guide
├── QUICKSTART.md              # Quick start guide
└── start.ps1                  # Script to start both servers
```

## Quick Start

### Prerequisites

- **Java 21**
- **Maven** (included via Maven Wrapper)
- **Bun** (JavaScript runtime)
- **PostgreSQL 16** running on port 5432
- Database: `AI_docs_assistant` Create ka muna schema with this name saka mo run and build.

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

### Backend
- **Java 21** - Latest LTS
- **Spring Boot 4.0** - Framework
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database access
- **PostgreSQL** - Database
- **OAuth2** - Authentication protocol
- **BCrypt** - Password encryption
- **Maven** - Build tool

## Documentation
- [**frontend/README.md**](./frontend/README.md) - Frontend-specific documentation

## API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/` | API information | ❌ |
| GET | `/api/status` | Check API health | ❌ |
| GET | `/user/signup` | Signup instructions | ❌ |
| POST | `/user/signup` | Register new user | ❌ |

## Default Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend | 5173 | http://localhost:5173 |
| Backend | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | localhost:5432 |