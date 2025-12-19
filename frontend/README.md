# AI-Powered Legal Docs Assistant - Frontend

A modern, decoupled frontend built with React, Tailwind CSS, Three.js, and GSAP.

## 🚀 Tech Stack

- **React 19** - UI library
- **Vite** - Build tool
- **Bun** - Fast JavaScript runtime and package manager
- **Tailwind CSS** - Utility-first CSS framework
- **Three.js** - 3D graphics library
- **@react-three/fiber** - React renderer for Three.js
- **@react-three/drei** - Useful helpers for react-three-fiber
- **maath/random** - Random utilities for 3D
- **GSAP** - Professional-grade animation library

## 📁 Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── Hero.jsx              # Landing page hero section
│   │   ├── Features.jsx          # Features showcase
│   │   └── ThreeBackground.jsx   # 3D background with Three.js
│   ├── App.jsx                   # Main application component
│   ├── main.jsx                  # Application entry point
│   └── index.css                 # Global styles with Tailwind
├── public/                       # Static assets
├── tailwind.config.js            # Tailwind configuration
├── postcss.config.js             # PostCSS configuration
├── vite.config.js                # Vite configuration
├── package.json                  # Dependencies
└── bun.lock                      # Bun lock file
```

## 📦 Available Scripts

```bash
# Development
bun run dev          # Start dev server

# Build
bun run build        # Build for production
bun run preview      # Preview production build

# ALL IN ONE COMMAND
.\start.ps1          # Yo case, eto nalang script na i-run mo sa root folder, doon sa labas lang ng frontend folder.
                     # This shit runs both backend and frontend servers.
```