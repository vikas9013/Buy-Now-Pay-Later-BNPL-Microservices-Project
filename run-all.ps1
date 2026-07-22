# PowerShell script to start both BNPL microservices concurrently
$scriptPath = $PSScriptRoot

Write-Host "Starting User Service (Port 8081)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$scriptPath\user-service'; mvn spring-boot:run"

Write-Host "Starting Ledger Service (Port 8082)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$scriptPath\ledger-service'; mvn spring-boot:run"

Write-Host "Both BNPL microservices are launching concurrently in separate windows!" -ForegroundColor Cyan
