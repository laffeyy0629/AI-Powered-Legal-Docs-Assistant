@echo off
REM Build script for frontend - works without bun
echo Building frontend application...
node .\node_modules\vite\bin\vite.js build
echo.
echo Build complete! Output is in the 'dist' folder.

