# PowerShell Auto-Launch Script for BNPL Microservices & Spring Cloud Infrastructure
Write-Host "🚀 Launching BNPL Microservices & Spring Cloud Infrastructure..." -ForegroundColor Green

$basePath = "C:\Users\vikas\Documents\antigravity\bnpl-microservices"

# 1. Launch Netflix Eureka Discovery Server (Port 8761)
Write-Host "1. Starting Eureka Discovery Server on Port 8761..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$basePath\discovery-server'; Write-Host 'Starting discovery-server (Port 8761)...' -ForegroundColor Cyan; mvn spring-boot:run"

# Wait 10 seconds for Eureka Server to fully initialize before clients attempt registration
Write-Host "⏳ Waiting 10 seconds for Eureka Server initialization..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# 2. Launch Spring Cloud API Gateway (Port 8080)
Write-Host "2. Starting Spring Cloud API Gateway on Port 8080..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$basePath\api-gateway'; Write-Host 'Starting api-gateway (Port 8080)...' -ForegroundColor Cyan; mvn spring-boot:run"

# 3. Launch User Service (Port 8081)
Write-Host "3. Starting User Service on Port 8081..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$basePath\user-service'; Write-Host 'Starting user-service (Port 8081)...' -ForegroundColor Cyan; mvn spring-boot:run"

# 4. Launch Ledger Service (Port 8082)
Write-Host "4. Starting Ledger Service on Port 8082..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$basePath\ledger-service'; Write-Host 'Starting ledger-service (Port 8082)...' -ForegroundColor Cyan; mvn spring-boot:run"

# 5. Launch Notification Service (Port 8083)
Write-Host "5. Starting Notification Service on Port 8083..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$basePath\notification-service'; Write-Host 'Starting notification-service (Port 8083)...' -ForegroundColor Cyan; mvn spring-boot:run"

Write-Host "🎉 All 5 Microservices launched in separate terminals!" -ForegroundColor Green
Write-Host "👉 Eureka Dashboard: http://localhost:8761 (Wait 15s for initial heartbeats)" -ForegroundColor Cyan
Write-Host "👉 API Gateway Entrypoint: http://localhost:8080" -ForegroundColor Cyan
