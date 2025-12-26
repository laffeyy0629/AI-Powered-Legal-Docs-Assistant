import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Split Three.js and related libraries into a separate chunk
          'three-vendor': ['three', '@react-three/fiber', '@react-three/drei', 'maath'],
          // Split GSAP into its own chunk
          'gsap-vendor': ['gsap'],
          // React and React-DOM in their own chunk
          'react-vendor': ['react', 'react-dom']
        }
      }
    },
    // Increase chunk size warning limit to 1000 kB since we're using legitimate large libraries
    chunkSizeWarningLimit: 1000,
    // Use esbuild for minification (faster and no extra dependencies needed)
    minify: 'esbuild'
  }
})
