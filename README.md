# 💳 Buy Now, Pay Later (BNPL) & Flexible EMI - Enterprise Fintech Microservices

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-brightgreen.svg)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-Gateway_%26_Eureka-blue.svg)
![Spring Security](https://img.shields.io/badge/OAuth_2.0-JWT_Secured-blue.svg)
![Redis](https://img.shields.io/badge/Redis-Idempotency_%26_Locks-red.svg)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-Event--Driven-red.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A production-grade, pure backend **Buy Now, Pay Later (BNPL) & Flexible EMI** distributed system built with **Java 17**, **Spring Boot 3**, **Redis (Idempotency & Redisson Distributed Locks)**, **Spring Cloud (API Gateway & Eureka Discovery)**, **Spring Security (OAuth 2.0 / JWT)**, **Apache Kafka**, **PostgreSQL**, and **Docker**.

---

## 📌 Architectural Highlights

* **Single Entry Point (`api-gateway` @ Port 8080):** Spring Cloud API Gateway routes and load-balances all client traffic.
* **Dynamic Service Discovery (`discovery-server` @ Port 8761):** Netflix Eureka registry eliminates hardcoded service URLs.
* **Redis Idempotency Engine (`X-Idempotency-Key`):** Prevents double-charging during network retries or accidental double-clicks.
* **Redisson Distributed Locks (`Redlock`):** Prevents credit race conditions during high-concurrency checkout requests.
* **Microservices Isolation:** Strict Database-per-Service pattern (`user_db` and `ledger_db`).
* **Stateless OAuth 2.0 / JWT Security:** HMAC-SHA256 token issuance and Bearer token interception filter.
* **Event-Driven Architecture (EDA):** Asynchronous message publishing & consumption via Apache Kafka (`bnpl-checkout-events`, `bnpl-repayment-events`).
* **Flexible EMI Calculation Engine:** Supports 1 to 12-month installment schedules with automated interest calculation.
* **Automated Late Fee Scheduler:** Spring `@Scheduled` CRON job for enforcing late fees on overdue accounts.

---

## 🛡️ Enterprise Transaction Safety (Redis & Redisson)

1. **Idempotency Engine (`X-Idempotency-Key` Header):**
   * Clients pass an `X-Idempotency-Key: <unique-key>` header with `POST /api/ledger/checkout`.
   * Redis checks if the key was already processed. If found, it returns the cached transaction response in **<2ms** without re-executing or double-charging!

2. **Distributed Locking (Redisson `RLock`):**
   * Synchronizes credit balance deduction (`lock:credit:<userId>`) and checkout execution using Redis.
   * Ensures parallel requests from the same user execute sequentially without memory corruption or negative credit balances.

---

## 📅 Flexible EMI & Interest Rate Matrix

| Tenure | Repayment Plan | Interest Rate | Installments |
| :--- | :--- | :--- | :--- |
| **1 Month** | Pay in 30 Days | **0% Interest** | 1 Full Payment |
| **3 Months** | Low-Cost EMI | **5% Total Interest** | 3 Monthly Installments |
| **6 Months** | Standard EMI | **10% Total Interest** | 6 Monthly Installments |
| **9 Months** | Extended EMI | **12% Total Interest** | 9 Monthly Installments |
| **12 Months** | Full-Year EMI | **15% Total Interest** | 12 Monthly Installments |

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

### 💳 Ledger & EMI Operations (`lb://LEDGER-SERVICE`)
| Method | Endpoint | Headers | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `http://localhost:8080/api/ledger/checkout` | `X-Idempotency-Key` (Optional) | Execute Idempotent BNPL / EMI checkout |
| `POST` | `http://localhost:8080/api/ledger/repay` | `X-Idempotency-Key` (Optional) | Process repayment for an installment |
| `GET` | `http://localhost:8080/api/ledger/transactions/{userId}` | - | Get transaction history |
| `GET` | `http://localhost:8080/api/ledger/repayments/pending/{userId}` | - | Get pending EMI installment dues |

---

## 🚀 Quick Start

### Option A: PowerShell 1-Click Launch (All Microservices)
```powershell
.\run-all.ps1
```

### Option B: Docker Compose (Zookeeper + Kafka + Redis + Postgres + Services)
```bash
docker compose up --build
```

---

## 📝 License & Author

Developed by **Vikas Singh Rawat** as a showcase backend project for Fintech Software Engineering roles.

Distributed under the MIT License. See `LICENSE` for more information.
