# ==============================================================================
# BNPL Microservices - 1-Click Diagnostics & Health Check Suite
# ==============================================================================
param(
    [switch]$SkipSmokeTest = $false
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " 💳  BNPL MICROSERVICES - SYSTEM HEALTH & VERIFICATION SUITE           " -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " Local Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
Write-Host ""

$results = [System.Collections.Generic.List[PSCustomObject]]::new()

function Test-TcpPort {
    param($HostName, $Port, $TimeoutMs = 1500)
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $connect = $tcpClient.BeginConnect($HostName, $Port, $null, $null)
        $success = $connect.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        if ($success -and $tcpClient.Connected) {
            $tcpClient.EndConnect($connect)
            $tcpClient.Close()
            return $true
        }
        $tcpClient.Close()
        return $false
    } catch {
        return $false
    }
}

function Test-HttpEndpoint {
    param($Url, $TimeoutSec = 3)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec $TimeoutSec -UseBasicParsing -ErrorAction Stop
        $sw.Stop()
        return @{
            Success = $true
            StatusCode = [int]$response.StatusCode
            LatencyMs = [int]$sw.ElapsedMilliseconds
            Content = $response.Content
        }
    } catch {
        $sw.Stop()
        $statusCode = 0
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        return @{
            Success = ($statusCode -gt 0 -and $statusCode -lt 500)
            StatusCode = $statusCode
            LatencyMs = [int]$sw.ElapsedMilliseconds
            Error = $_.Exception.Message
        }
    }
}

# ------------------------------------------------------------------------------
# 1. Backing Infrastructure Checks
# ------------------------------------------------------------------------------
Write-Host "🔍 [STEP 1/4] Checking Core Infrastructure..." -ForegroundColor Cyan

# PostgreSQL (Port 5432)
$pgPort = Test-TcpPort -HostName "localhost" -Port 5432
if ($pgPort) {
    Write-Host "  ✅ PostgreSQL (Port 5432)        : ONLINE" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "PostgreSQL"; Port = 5432; Status = "ONLINE"; Notes = "Port open & responsive" })
} else {
    Write-Host "  ❌ PostgreSQL (Port 5432)        : OFFLINE (Check local PostgreSQL service or Docker)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "PostgreSQL"; Port = 5432; Status = "OFFLINE"; Notes = "Connection refused" })
}

# Redis (Port 6379)
$redisPort = Test-TcpPort -HostName "localhost" -Port 6379
if ($redisPort) {
    Write-Host "  ✅ Redis (Port 6379)             : ONLINE" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "Redis"; Port = 6379; Status = "ONLINE"; Notes = "Idempotency & Locks ready" })
} else {
    Write-Host "  ❌ Redis (Port 6379)             : OFFLINE (Run: docker start redis)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "Redis"; Port = 6379; Status = "OFFLINE"; Notes = "Connection refused" })
}

# Kafka (Port 9092)
$kafkaPort = Test-TcpPort -HostName "localhost" -Port 9092
if ($kafkaPort) {
    Write-Host "  ✅ Apache Kafka (Port 9092)      : ONLINE" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "Apache Kafka"; Port = 9092; Status = "ONLINE"; Notes = "Event broker ready" })
} else {
    Write-Host "  ❌ Apache Kafka (Port 9092)      : OFFLINE (Run: docker restart zookeeper; docker start kafka)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "Apache Kafka"; Port = 9092; Status = "OFFLINE"; Notes = "Connection refused" })
}

# Zipkin (Port 9411)
$zipkinPort = Test-TcpPort -HostName "localhost" -Port 9411
if ($zipkinPort) {
    Write-Host "  ✅ Zipkin Tracing (Port 9411)    : ONLINE" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "Zipkin Tracing"; Port = 9411; Status = "ONLINE"; Notes = "Distributed tracing ready" })
} else {
    Write-Host "  ⚠️ Zipkin Tracing (Port 9411)    : OFFLINE (Optional tracing container)" -ForegroundColor Yellow
    $results.Add([PSCustomObject]@{ Service = "Zipkin Tracing"; Port = 9411; Status = "OFFLINE"; Notes = "Optional" })
}

Write-Host ""

# ------------------------------------------------------------------------------
# 2. Microservices Status Checks
# ------------------------------------------------------------------------------
Write-Host "🚀 [STEP 2/4] Checking Microservices Ports & Endpoints..." -ForegroundColor Cyan

# Discovery Server (Eureka @ Port 8761)
$eurekaRes = Test-HttpEndpoint -Url "http://localhost:8761"
if ($eurekaRes.Success -or $eurekaRes.StatusCode -eq 200) {
    Write-Host "  ✅ discovery-server (Port 8761)  : ONLINE ($($eurekaRes.LatencyMs)ms)" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "discovery-server"; Port = 8761; Status = "ONLINE"; Notes = "$($eurekaRes.LatencyMs)ms latency" })
} else {
    Write-Host "  ❌ discovery-server (Port 8761)  : DOWN (Not reachable)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "discovery-server"; Port = 8761; Status = "DOWN"; Notes = "Not responding on :8761" })
}

# API Gateway (Port 8080)
$gatewayRes = Test-HttpEndpoint -Url "http://localhost:8080/actuator/health"
if (-not $gatewayRes.Success) {
    # Fallback to checking root or tcp
    $gatewayRes = Test-HttpEndpoint -Url "http://localhost:8080"
}
if ($gatewayRes.Success -or $gatewayRes.StatusCode -in 200, 404) {
    Write-Host "  ✅ api-gateway (Port 8080)       : ONLINE ($($gatewayRes.LatencyMs)ms)" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "api-gateway"; Port = 8080; Status = "ONLINE"; Notes = "Routing entrypoint active" })
} else {
    Write-Host "  ❌ api-gateway (Port 8080)       : DOWN (Not reachable)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "api-gateway"; Port = 8080; Status = "DOWN"; Notes = "Not responding on :8080" })
}

# User Service (Port 8081)
$userRes = Test-HttpEndpoint -Url "http://localhost:8081/actuator/health"
if (-not $userRes.Success) {
    $userRes = Test-HttpEndpoint -Url "http://localhost:8081"
}
if ($userRes.Success -or $userRes.StatusCode -in 200, 404, 401, 403) {
    Write-Host "  ✅ user-service (Port 8081)      : ONLINE ($($userRes.LatencyMs)ms)" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "user-service"; Port = 8081; Status = "ONLINE"; Notes = "Auth & Credit service active" })
} else {
    Write-Host "  ❌ user-service (Port 8081)      : DOWN (Not reachable)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "user-service"; Port = 8081; Status = "DOWN"; Notes = "Not responding on :8081" })
}

# Ledger Service (Port 8082)
$ledgerRes = Test-HttpEndpoint -Url "http://localhost:8082/actuator/health"
if (-not $ledgerRes.Success) {
    $ledgerRes = Test-HttpEndpoint -Url "http://localhost:8082"
}
if ($ledgerRes.Success -or $ledgerRes.StatusCode -in 200, 404, 401, 403) {
    Write-Host "  ✅ ledger-service (Port 8082)    : ONLINE ($($ledgerRes.LatencyMs)ms)" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "ledger-service"; Port = 8082; Status = "ONLINE"; Notes = "EMI & Ledger engine active" })
} else {
    Write-Host "  ❌ ledger-service (Port 8082)    : DOWN (Not reachable)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "ledger-service"; Port = 8082; Status = "DOWN"; Notes = "Not responding on :8082" })
}

# Notification Service (Port 8083)
$notifyPort = Test-TcpPort -HostName "localhost" -Port 8083
if ($notifyPort) {
    Write-Host "  ✅ notification-service (Port 8083): ONLINE" -ForegroundColor Green
    $results.Add([PSCustomObject]@{ Service = "notification-service"; Port = 8083; Status = "ONLINE"; Notes = "Kafka consumer active" })
} else {
    Write-Host "  ❌ notification-service (Port 8083): DOWN (Not reachable)" -ForegroundColor Red
    $results.Add([PSCustomObject]@{ Service = "notification-service"; Port = 8083; Status = "DOWN"; Notes = "Not responding on :8083" })
}

Write-Host ""

# ------------------------------------------------------------------------------
# 3. Eureka Registry Discovery Check
# ------------------------------------------------------------------------------
Write-Host "📡 [STEP 3/4] Inspecting Netflix Eureka Service Registry..." -ForegroundColor Cyan
if ($eurekaRes.Success) {
    try {
        $headers = @{ "Accept" = "application/json" }
        $registry = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers $headers -TimeoutSec 3 -ErrorAction Stop
        $apps = $registry.applications.application
        if ($apps) {
            $appNames = @($apps | ForEach-Object { $_.name }) -join ", "
            Write-Host "  Registered Applications: $appNames" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️ Eureka is running, but no client services have registered yet. (Allow 10-15s for heartbeats)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ℹ️ Eureka running. (Registry query pending registration heartbeat)" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️ Skipped: Eureka Discovery Server is not running." -ForegroundColor Yellow
}

Write-Host ""

# ------------------------------------------------------------------------------
# 4. End-to-End Gateway Functional Smoke Test
# ------------------------------------------------------------------------------
if (-not $SkipSmokeTest) {
    Write-Host "🧪 [STEP 4/4] Running Live Gateway End-to-End Smoke Test..." -ForegroundColor Cyan

    $canRunSmoke = ($gatewayRes.Success -or $gatewayRes.StatusCode -in 200, 404) -and ($userRes.Success)

    if ($canRunSmoke) {
        try {
            $rand = Get-Random -Minimum 10000 -Maximum 99999
            $testEmail = "healthcheck$rand@example.com"
            $testPass = "Pass123!$rand"

            # 4.1 Register Test User (with brief warmup retry for Eureka route synchronization)
            Write-Host "  1. Testing POST /api/auth/register..." -NoNewline
            $regBody = @{
                fullName = "Health Check User $rand"
                email = $testEmail
                password = $testPass
                phoneNumber = "+9198765$rand"
            } | ConvertTo-Json

            $regRes = $null
            for ($attempt = 1; $attempt -le 4; $attempt++) {
                try {
                    $regRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" -TimeoutSec 5 -ErrorAction Stop
                    break
                } catch {
                    if ($attempt -lt 4 -and $_.Exception.Message -match "503") {
                        Start-Sleep -Seconds 3
                    } else {
                        throw $_
                    }
                }
            }
            $userId = $regRes.userId
            $token = $regRes.token
            Write-Host " SUCCESS (UserId: $userId)" -ForegroundColor Green

            $authHeaders = @{
                "Authorization" = "Bearer $token"
                "Content-Type" = "application/json"
            }

            # 4.2 Fetch Credit Line
            Write-Host "  2. Testing GET /api/users/{userId}/credit..." -NoNewline
            $creditRes = Invoke-RestMethod -Uri "http://localhost:8080/api/users/$userId/credit" -Method Get -Headers $authHeaders -TimeoutSec 5
            Write-Host " SUCCESS (Available: ₹$($creditRes.availableCredit))" -ForegroundColor Green

            # 4.3 Execute BNPL Checkout
            Write-Host "  3. Testing POST /api/ledger/checkout (Idempotent)..." -NoNewline
            $checkoutHeaders = @{
                "Authorization" = "Bearer $token"
                "Content-Type" = "application/json"
                "X-Idempotency-Key" = [guid]::NewGuid().ToString()
            }
            $checkoutBody = @{
                userId = $userId
                amount = 1000.00
                merchantName = "Diagnostics Test Merchant"
                tenureMonths = 3
            } | ConvertTo-Json

            $checkoutRes = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/checkout" -Method Post -Headers $checkoutHeaders -Body $checkoutBody -TimeoutSec 5
            Write-Host " SUCCESS (Tx ID: $($checkoutRes.transactionId), Installment: ₹$($checkoutRes.monthlyInstallment))" -ForegroundColor Green

            # 4.4 Verify Repayments
            Write-Host "  4. Testing GET /api/ledger/repayments/pending/{userId}..." -NoNewline
            $pendingRes = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/repayments/pending/$userId" -Method Get -Headers $authHeaders -TimeoutSec 5
            Write-Host " SUCCESS (Pending Installments: $($pendingRes.Count))" -ForegroundColor Green

            # 4.5 Execute Repayment
            if ($pendingRes.Count -gt 0) {
                Write-Host "  5. Testing POST /api/ledger/repay..." -NoNewline
                $firstInst = $pendingRes[0]
                $repayHeaders = @{
                    "Authorization" = "Bearer $token"
                    "Content-Type" = "application/json"
                    "X-Idempotency-Key" = [guid]::NewGuid().ToString()
                }
                $repayBody = @{
                    repaymentId = $firstInst.id
                    amount = $firstInst.amountDue
                } | ConvertTo-Json

                $repayRes = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/repay" -Method Post -Headers $repayHeaders -Body $repayBody -TimeoutSec 5
                Write-Host " SUCCESS (Paid: $($repayRes.isPaid))" -ForegroundColor Green
            }

            # 4.6 Spring AI GenAI Financial Advisor
            Write-Host "  6. Testing GET /api/ledger/ai/financial-insights/{userId}..." -NoNewline
            $aiRes = Invoke-RestMethod -Uri "http://localhost:8080/api/ledger/ai/financial-insights/$userId" -Method Get -Headers $authHeaders -TimeoutSec 5
            Write-Host " SUCCESS (Score: $($aiRes.estimatedCreditScore) - $($aiRes.creditScoreCategory) | Engine: $($aiRes.engineModel))" -ForegroundColor Green

            # 4.7 Credit Limit Upgrade
            Write-Host "  7. Testing POST /api/users/{userId}/credit/upgrade..." -NoNewline
            $upgradeBody = @{
                newCreditLimit = 25000.00
                reason = "Annual salary increment and promotion"
            } | ConvertTo-Json
            $upgradeRes = Invoke-RestMethod -Uri "http://localhost:8080/api/users/$userId/credit/upgrade" -Method Post -Headers $authHeaders -Body $upgradeBody -TimeoutSec 5
            Write-Host " SUCCESS (New Limit: ₹$($upgradeRes.totalCreditLimit), Available: ₹$($upgradeRes.availableCredit))" -ForegroundColor Green

            Write-Host "  ✨ End-to-end smoke test completed with 100% SUCCESS!" -ForegroundColor Green
        } catch {
            Write-Host " FAILED" -ForegroundColor Red
            Write-Host "  ⚠️ Smoke test error: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ℹ️ Microservices are currently not running. Launch them first using .\run-all.ps1" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " 📊  STATUS SUMMARY SCORECARD                                          " -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Cyan
$results | Format-Table -Property Service, Port, Status, Notes -AutoSize

$allServicesUp = ($results | Where-Object { $_.Status -in "DOWN", "OFFLINE" -and $_.Service -ne "Zipkin Tracing" }).Count -eq 0

if ($allServicesUp) {
    Write-Host "🎉 ALL SYSTEMS OPERATIONAL! Everything is working correctly." -ForegroundColor Green
} else {
    Write-Host "💡 TIPS TO BRING REMAINING SERVICES ONLINE:" -ForegroundColor Yellow
    Write-Host "  1. If containers are offline: run 'docker compose up -d redis zookeeper kafka zipkin'" -ForegroundColor Gray
    Write-Host "  2. To start all 5 microservices: run '.\run-all.ps1'" -ForegroundColor Gray
    Write-Host "  3. To run Maven unit/integration tests: run 'mvn test'" -ForegroundColor Gray
}
Write-Host ""
