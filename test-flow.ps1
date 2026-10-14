# Setup variables
$GatewayUrl = "http://localhost:8080"
$ContentType = "application/json"
$IdempotencyKey = [guid]::NewGuid().ToString()

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING BNPL END-TO-END TEST" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Register a New User & Obtain JWT Token
Write-Host "`n[1] Registering a new User & Generating JWT..." -ForegroundColor Yellow
$RegisterPayload = @{
    fullName = "Vikas"
    email = "vikas-$(Get-Date -Format 'yyyyMMddHHmmss')@example.com"
    phoneNumber = "+919968$((Get-Random -Minimum 100000 -Maximum 999999))"
    password = "securePassword123"
} | ConvertTo-Json

$Headers = @{ "Content-Type" = $ContentType }

try {
    $UserResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/register" -Method Post -Body $RegisterPayload -ContentType $ContentType
    Write-Host "✅ User Registered Successfully!" -ForegroundColor Green
    $UserId = $UserResponse.userId
    $Token = $UserResponse.token
    Write-Host "User ID: $UserId"
    $Headers["Authorization"] = "Bearer $Token"
} catch {
    Write-Host "❌ Failed to register user." -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

# 2. Check Available Credit
Write-Host "`n[2] Checking Available Credit..." -ForegroundColor Yellow
try {
    $CreditResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/users/$UserId/credit" -Method Get -Headers $Headers
    Write-Host "✅ Credit Fetched Successfully!" -ForegroundColor Green
    Write-Host "Available Credit: ₹$($CreditResponse.availableCredit) / ₹$($CreditResponse.totalCreditLimit)"
} catch {
    Write-Host "❌ Failed to fetch credit." -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 3. Perform a Checkout via Ledger Service
Write-Host "`n[3] Performing a BNPL Checkout (Amount: ₹3000)..." -ForegroundColor Yellow
$CheckoutHeaders = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = $ContentType
    "X-Idempotency-Key" = $IdempotencyKey
}
$CheckoutPayload = @{
    userId = $UserId
    amount = 3000.00
    merchantName = "MERCHANT-AMAZON-001"
    tenureMonths = 3
} | ConvertTo-Json

try {
    $CheckoutResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/ledger/checkout" -Method Post -Body $CheckoutPayload -ContentType $ContentType -Headers $CheckoutHeaders
    Write-Host "✅ Checkout Successful!" -ForegroundColor Green
    Write-Host "Transaction ID: $($CheckoutResponse.transactionId)"
    Write-Host "Monthly Installment: ₹$($CheckoutResponse.monthlyInstallment) (Tenure: $($CheckoutResponse.tenureMonths) months)"
} catch {
    Write-Host "❌ Failed to execute checkout." -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "🎉 END-TO-END TEST COMPLETE!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
