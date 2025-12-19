# Start Both Backend and Frontend Servers
# This script opens two new PowerShell windows to run backend and frontend

Write-Host "Starting AI-Powered Legal Docs Assistant..." -ForegroundColor Cyan
Write-Host ""

$projectRoot = "C:\Users\furui\IdeaProjects\AI-Powered-Legal-Docs-Assistant"

# Check if Bun is installed
Write-Host "Checking Bun installation..." -ForegroundColor Yellow
$bunPath = "$env:USERPROFILE\.bun\bin\bun.exe"
if (-not (Test-Path $bunPath)) {
    Write-Host "ERROR: Bun not found at $bunPath" -ForegroundColor Red
    Write-Host "Please install Bun first: https://bun.sh" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Bun found" -ForegroundColor Green
Write-Host ""

# Start Backend in new window
Write-Host "Starting Backend (Spring Boot)..." -ForegroundColor Yellow
$backendPath = Join-Path $projectRoot "ai-docs-assistant"
$backendCommand = "cd '$backendPath'; Write-Host 'Starting Spring Boot Backend...' -ForegroundColor Cyan; & '.\mvnw.cmd' spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCommand

Write-Host "Waiting 5 seconds for backend to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Start Frontend in new window
Write-Host "Starting Frontend (React)..." -ForegroundColor Yellow
$frontendPath = Join-Path $projectRoot "frontend"
$frontendCommand = "cd '$frontendPath'; Write-Host 'Starting React Frontend...' -ForegroundColor Cyan; & '$bunPath' run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCommand

Write-Host ""
Write-Host "Application is starting!" -ForegroundColor Green
Write-Host ""
Write-Host "Endpoints:" -ForegroundColor Cyan
Write-Host "   Backend API: http://localhost:8080" -ForegroundColor White
Write-Host "   Frontend:    http://localhost:5173" -ForegroundColor White
Write-Host ""
Write-Host "Please wait ~30 seconds for both servers to fully start..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Press any key to open the application in browser..." -ForegroundColor Green
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# Open browser
Start-Process "http://localhost:5173"

Write-Host ""
Write-Host "Done! Check the new PowerShell windows for server logs." -ForegroundColor Green
Write-Host "To stop the servers, close the PowerShell windows or press Ctrl+C in each." -ForegroundColor Yellow

