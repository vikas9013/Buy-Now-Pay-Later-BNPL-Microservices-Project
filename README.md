# 💳 Buy Now, Pay Later (BNPL) & Flexible EMI - Enterprise Fintech Microservices

[![CI Build & Test](https://github.com/vikas9013/Buy-Now-Pay-Later-BNPL-Microservices-Project/actions/workflows/ci.yml/badge.svg)](https://github.com/vikas9013/Buy-Now-Pay-Later-BNPL-Microservices-Project/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17%20%2F%2021-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-brightgreen.svg)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-Gateway_%26_Eureka-blue.svg)
![Spring Security](https://img.shields.io/badge/OAuth_2.0-JWT_Secured-blue.svg)
![Payment Gateways](https://img.shields.io/badge/Payment_Gateways-Razorpay_%26_Stripe-528FF0.svg)
![Redis](https://img.shields.io/badge/Redis-Idempotency_%26_Locks-red.svg)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-Event--Driven-red.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%20%2F%2018-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A production-grade, pure backend **Buy Now, Pay Later (BNPL) & Flexible EMI** distributed system built with **Java 17/21**, **Spring Boot 3**, **Payment Gateways (Razorpay & Stripe)**, **Redis (Idempotency & Redisson Distributed Locks)**, **Spring Cloud (API Gateway & Eureka Discovery)**, **Spring Security (OAuth 2.0 / JWT)**, **Apache Kafka**, **PostgreSQL**, and **Docker**.

---

## 📌 Architectural Highlights

* **Single Entry Point (`api-gateway` @ Port 8080):** Spring Cloud API Gateway routes and load-balances all client traffic.
* **Dynamic Service Discovery (`discovery-server` @ Port 8761):** Netflix Eureka registry eliminates hardcoded service URLs.
* **Payment Gateway Integration (Razorpay & Stripe):** Supports order creation, subunit conversions (paise/cents), and cryptographic **HMAC-SHA256 signature verification** for real UPI and Card repayments.
* **Redis Idempotency Engine (`X-Idempotency-Key`):** Prevents double-charging during network retries or duplicate checkouts with sub-30ms response caching.
* **Redisson Distributed Locks (`Redlock`):** Prevents credit race conditions during high-concurrency checkout requests via `lock:credit:<userId>`.
* **Microservices Isolation:** Strict Database-per-Service pattern (`user_db` and `ledger_db`).
* **Stateless OAuth 2.0 / JWT Security:** HMAC-SHA256 token issuance and Bearer token interception filter.
* **Event-Driven Architecture (EDA):** Asynchronous message publishing & consumption via Apache Kafka (`bnpl-checkout-events`, `bnpl-repayment-events`).
* **Flexible EMI Calculation Engine:** Supports 1 to 12-month installment schedules with automated interest calculation.
* **Automated Late Fee Scheduler:** Spring `@Scheduled` CRON job for enforcing late fees on overdue accounts.
* **Automated Credit Restoration:** Synchronous and event-driven credit limit restoration upon verified installment repayment.

---

## 💳 Real Payment Gateway Architecture (Razorpay & Stripe)

The platform supports end-to-end payment gateway workflows for processing customer EMI repayments:

```
                  [ Customer / Client ]
                     /              \
    1. Create Order /                \ 3. Verify & Repay
                   v                  v
       +-----------------------------------------------+
       |         api-gateway (Port 8080)               |
       +-----------------------------------------------+
                              |
                              v
       +-----------------------------------------------+
       |        ledger-service (Port 8082)             |
       |           PaymentGatewayService               |
       +-----------------------------------------------+
           /                     \                 \
          v                       v                 v
   [ Razorpay Gateway ]    [ Stripe Gateway ]  [ User Service ]
   * Order: order_xxx      * Intent: pi_xxx    * Restore Credit
   * HMAC-SHA256 Signature * 3D-Secure Card    * Post to ledger_db
```

### 1. Razorpay UPI Repayment Workflow
1. **Initiate Order (`POST /api/ledger/payment/create-order`):**
   * Creates an order with currency subunits (e.g., ₹1,050.00 = `105000` paise).
   * Returns `orderId` (e.g. `order_aae9b23...`) and public key ID.
2. **Customer Payment:** Customer completes UPI transaction via VPA (`upi://pay`).
3. **Cryptographic Verification (`POST /api/ledger/payment/verify-and-repay`):**
   * Computes HMAC-SHA256 signature:
     $$\text{Signature} = \text{HMAC-SHA256}(\text{orderId} + \text{"\|"} + \text{paymentId}, \text{secretKey})$$
   * Verifies authenticity, marks installment as `isPaid = true`, logs gateway reference `pay_xxx`, and restores customer credit.

### 2. Stripe Card Repayment Workflow
1. **Create PaymentIntent (`POST /api/ledger/payment/create-order`):** Generates intent ID (`pi_xxx`).
2. **Authorization:** Authenticates card token (`ch_xxx` / `pm_card_visa`).
3. **Verification & Settlement:** Validates payment and restores user credit line.

---

## 🛡️ Enterprise Transaction Safety (Redis & Redisson)

1. **Idempotency Engine (`X-Idempotency-Key` Header):**
   * Clients pass an `X-Idempotency-Key: <unique-uuid>` header with `POST /api/ledger/checkout` and `POST /api/ledger/repay`.
   * Redis checks if the key was already processed. If found, it returns the cached transaction response in **<25ms** without double-deducting credit!
2. **Distributed Locking (Redisson `RLock`):**
   * Synchronizes credit balance deduction (`lock:credit:<userId>`) and checkout execution using Redis.
   * Ensures rapid parallel requests from the same user execute sequentially with zero race conditions or negative credit balances.

---

## 📅 Flexible EMI & Interest Rate Matrix

| Tenure | Repayment Plan | Interest Rate | Installments | Example (₹6,000 Purchase) |
| :--- | :--- | :--- | :--- | :--- |
| **1 Month** | Pay in 30 Days | **0% Interest** | 1 Full Payment | ₹6,000 (1x ₹6,000.00) |
| **3 Months** | Low-Cost EMI | **5% Total Interest** | 3 Monthly Installments | ₹6,300 (3x ₹2,100.00/mo) |
| **6 Months** | Standard EMI | **10% Total Interest** | 6 Monthly Installments | ₹6,600 (6x ₹1,100.00/mo) |
| **9 Months** | Extended EMI | **12% Total Interest** | 9 Monthly Installments | ₹6,720 (9x ₹746.67/mo) |
| **12 Months** | Full-Year EMI | **15% Total Interest** | 12 Monthly Installments | ₹6,900 (12x ₹575.00/mo) |

---

## 🏗️ System Architecture

```
                                [ Client / Postman ]
                                         |
                            Header: [X-Idempotency-Key]
                                         |
                                         v
                      +------------------------------------+
                      |     api-gateway (Port 8080)        |
                      +------------------------------------+
                                 /            \
                (Dynamic Route) /              \ (Dynamic Route)
                               v                v
                 +-------------------+    +-------------------+
                 |   user-service    |    |  ledger-service   |
                 |    (Port 8081)    |    |    (Port 8082)    |
                 +-------------------+    +-------------------+
                     /         \                /       \
        (Redisson RLock)      (PostgreSQL)  (Redis Cache) (PostgreSQL)
                   /             \            /           \
                  v               v          v             v
             [ REDIS:6379 ]  [ user_db ]  [ Idempotency ] [ ledger_db ]
                                       \        |
                                        v       v
                             APACHE KAFKA BROKER
                Topics: [bnpl-checkout-events] [bnpl-repayment-events]
                                       |
                                       v
                      +------------------------------------+
                      |   notification-service (Port 8083) |
                      +------------------------------------+
```

---

## 📡 Gateway API Reference (`http://localhost:8080`)

### 🔐 Authentication & Users (`lb://USER-SERVICE`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `http://localhost:8080/api/auth/register` | Register new user & issue JWT Token |
| `POST` | `http://localhost:8080/api/auth/login` | Authenticate user & issue JWT Token |
| `GET` | `http://localhost:8080/api/users/{userId}/credit` | Fetch credit account details & balance |
| `POST` | `http://localhost:8080/api/users/{userId}/credit/deduct` | Internal: Deduct credit limit |
| `POST` | `http://localhost:8080/api/users/{userId}/credit/restore` | Internal: Restore credit limit |

### 💳 Ledger, EMI & Payment Operations (`lb://LEDGER-SERVICE`)
| Method | Endpoint | Headers | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `http://localhost:8080/api/ledger/checkout` | `X-Idempotency-Key` (Optional) | Execute Idempotent BNPL / EMI checkout |
| `POST` | `http://localhost:8080/api/ledger/payment/create-order` | - | Create live Razorpay (`api.razorpay.com`) / Stripe order |
| `POST` | `http://localhost:8080/api/ledger/payment/verify-and-repay` | `X-Idempotency-Key` (Optional) | Verify signature/token & process repayment |
| `POST` | `http://localhost:8080/api/ledger/payment/webhook/razorpay` | `X-Razorpay-Signature` | Razorpay webhook callback for async reconciliation |
| `POST` | `http://localhost:8080/api/ledger/repay` | `X-Idempotency-Key` (Optional) | Direct / Gateway repayment for an installment |
| `GET` | `http://localhost:8080/api/ledger/transactions/{userId}` | - | Get full transaction history |
| `GET` | `http://localhost:8080/api/ledger/repayments/pending/{userId}` | - | Get pending EMI installment dues |

---

## 🧪 Testing Real Payment Services (Automated Test Suite)

An enterprise automated test harness is included to test realistic customer scenarios, payment gateways, and concurrency:

```powershell
# Run the complete real-world payment services test suite
pwsh -File .\test-real-payments.ps1
```

### What the Test Suite Verifies:
1. **Realistic Customer Personas:** Onboards Aarav Sharma, Priya Patel, Rohan Gupta, and Ananya Iyer with instant KYC approval and ₹10,000 credit lines.
2. **Multi-Tenure Retail Checkouts:** Executes real purchases across Swiggy (1M 0% Int), Myntra (3M 5% Int), Amazon (6M 10% Int), Croma (9M 12% Int), and Apple Store (12M 15% Int).
3. **Razorpay UPI Repayments:** Generates live orders, simulates UPI payments, validates HMAC-SHA256 signatures, and restores credit lines.
4. **Stripe Card Repayments:** Creates PaymentIntents, authorizes card tokens, processes repayments, and updates the ledger.
5. **Redis Idempotency Engine:** Validates that duplicate requests with identical `X-Idempotency-Key` return in <25ms with zero double-charging.
6. **Credit Overdraft Guard:** Verifies system blocks transactions exceeding available credit limits.
7. **Redisson Distributed Lock Stress Test:** Fires simultaneous parallel checkout requests to ensure 100% thread safety and zero balance corruption.
8. **Audit Report:** Automatically exports a complete report to `payment-test-report.md`.

---

## 🚀 Quick Start

### Prerequisites
- **Java 17 or 21**
- **Apache Maven 3.9+**
- **Docker Desktop** (for Redis, Kafka, Zookeeper, and Zipkin)
- **PostgreSQL 15+**

### Option A: Local Launch (Recommended for Development)
```powershell
# 1. Start Support Infrastructure (Redis, Kafka, Zookeeper, Zipkin)
docker compose up -d redis zookeeper kafka zipkin

# 2. Launch All Microservices in Separate Terminals
.\run-all.ps1

# 3. Run the Comprehensive Real-Data Payment Test Suite
pwsh -File .\test-real-payments.ps1
```

### Option B: Docker Compose (Full Stack)
```bash
# Build and run all 5 microservices and all backing containers
docker-compose up -d --build
```

> **Note on Service Discovery:**
> Microservices register dynamically with Netflix Eureka (`http://localhost:8761`). Wait ~15 seconds on initial boot for Eureka heartbeats and route discovery by the `api-gateway`.

---

## 📝 License & Author

Developed by **Vikas Singh Rawat** as a showcase backend project for Fintech Software Engineering roles.

Distributed under the MIT License. See `LICENSE` for more information.
