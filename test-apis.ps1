$ErrorActionPreference = "Stop"

$rand = Get-Random
$email = "testuser$rand@example.com"
$password = "password123"

Write-Host "1. Testing /api/auth/register..."
$regBody = @{
    fullName = "Test User $rand"
    email = $email
    password = $password
    phoneNumber = "1234$rand"
} | ConvertTo-Json

$regResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
$userId = $regResponse.userId
Write-Host "Registration successful! User ID: $userId"
Write-Host ""

Write-Host "2. Testing /api/auth/login..."
$loginBody = @{
    email = $email
    password = $password
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResponse.token
Write-Host "Login successful! Token received."
Write-Host ""

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Write-Host "3. Testing GET /api/users/{userId}/credit..."
$creditResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/users/$userId/credit" -Method Get -Headers $headers
Write-Host "Credit check successful! Available limit: $($creditResponse.availableLimit)"
Write-Host ""

Write-Host "4. Testing POST /api/ledger/checkout..."
$checkoutHeaders = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
    "X-Idempotency-Key" = [guid]::NewGuid().ToString()
}
$checkoutBody = @{
    userId = $userId
    amount = 500.00
    merchantName = "Amazon"
    tenureMonths = 3
} | ConvertTo-Json

$checkoutResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/checkout" -Method Post -Headers $checkoutHeaders -Body $checkoutBody
Write-Host "Checkout successful! Status: $($checkoutResponse.status)"
Write-Host ""

# Wait a moment for async events to process
Start-Sleep -Seconds 2

Write-Host "5. Testing GET /api/ledger/transactions/{userId}..."
$txResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/transactions/$userId" -Method Get -Headers $headers
Write-Host "Transactions retrieved! Found: $($txResponse.Count) transaction(s)"
Write-Host ""

Write-Host "6. Testing GET /api/ledger/repayments/pending/{userId}..."
$pendingResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/repayments/pending/$userId" -Method Get -Headers $headers
Write-Host "Pending repayments retrieved! Found: $($pendingResponse.Count) pending installment(s)"
Write-Host ""

if ($pendingResponse.Count -gt 0) {
    Write-Host "7. Testing POST /api/ledger/repay..."
    $firstPending = $pendingResponse[0]
    $repayBody = @{
        repaymentId = $firstPending.id
        amount = $firstPending.amountDue
    } | ConvertTo-Json
    
    $repayHeaders = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
        "X-Idempotency-Key" = [guid]::NewGuid().ToString()
    }

    $repayResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/repay" -Method Post -Headers $repayHeaders -Body $repayBody
    Write-Host "Repayment successful! Status: $($repayResponse.status)"
} else {
    Write-Host "Skipping repayment test because no pending installments were found."
}
Write-Host ""
Write-Host "All API tests completed successfully!"
