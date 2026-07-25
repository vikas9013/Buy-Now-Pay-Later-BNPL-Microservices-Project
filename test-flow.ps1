# Setup variables
$GatewayUrl = "http://localhost:8080"
$ContentType = "application/json"
$IdempotencyKey = [guid]::NewGuid().ToString()

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING BNPL END-TO-END TEST" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Onboard a New User
Write-Host "`n[1] Onboarding a new User..." -ForegroundColor Yellow
$OnboardPayload = @{
    name = "Vikas"
    email = "vikas-$(Get-Date -Format 'yyyyMMddHHmmss')@example.com"
    phone = "+919968640964"
    password = "securePassword123"
} | ConvertTo-Json

try {
    $UserResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/users/onboard" -Method Post -Body $OnboardPayload -ContentType $ContentType
    Write-Host "✅ User Onboarded Successfully!" -ForegroundColor Green
    $UserId = $UserResponse.id
    Write-Host "User ID: $UserId"
} catch {
    Write-Host "❌ Failed to onboard user. Database might already have this user." -ForegroundColor Red
    Write-Host $_.Exception.Message
    Write-Host "Trying to fetch user ID 1 instead..."
    $UserId = 1
}

# 2. Check Available Credit
Write-Host "`n[2] Checking Available Credit..." -ForegroundColor Yellow
try {
    $CreditResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/users/$UserId/credit" -Method Get
    Write-Host "✅ Credit Fetched Successfully!" -ForegroundColor Green
    Write-Host "Available Credit: ₹$CreditResponse"
} catch {
    Write-Host "❌ Failed to fetch credit." -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 3. Perform a Checkout via Ledger Service
Write-Host "`n[3] Performing a BNPL Checkout (Amount: ₹3000)..." -ForegroundColor Yellow
$CheckoutPayload = @{
    userId = $UserId
    amount = 3000.00
    merchantName = "MERCHANT-AMAZON-001"
} | ConvertTo-Json

try {
    $CheckoutResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/ledger/checkout" -Method Post -Body $CheckoutPayload -ContentType $ContentType -Headers @{ "X-Idempotency-Key" = $IdempotencyKey }
    Write-Host "✅ Checkout Successful!" -ForegroundColor Green
    Write-Host "Transaction ID: $($CheckoutResponse.transactionId)"
    Write-Host "Remaining Credit: ₹$($CheckoutResponse.remainingCredit)"
} catch {
    Write-Host "❌ Failed to execute checkout." -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "🎉 END-TO-END TEST COMPLETE!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
