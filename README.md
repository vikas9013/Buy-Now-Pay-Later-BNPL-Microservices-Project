# 💳 Buy Now, Pay Later (BNPL) & Flexible EMI - Event-Driven Microservices Backend

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-brightgreen.svg)
![Spring Security](https://img.shields.io/badge/OAuth_2.0-JWT_Secured-blue.svg)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-Event--Driven-red.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A production-grade, pure backend **Buy Now, Pay Later (BNPL) & Flexible EMI** distributed system built with **Java 17**, **Spring Boot 3**, **Spring Security (OAuth 2.0 / JWT)**, **Apache Kafka**, **Spring WebFlux (WebClient)**, **PostgreSQL**, and **Docker**.

---

## 📌 Architectural Core Focus

This project is strictly focused on backend engineering principles:
* **Microservices Decoupling & Isolation:** Database-per-service pattern (`user_db` and `ledger_db`).
* **Stateless Security:** JWT authentication and authorization across microservices.
* **Event-Driven Architecture (EDA):** Asynchronous event streaming using Apache Kafka.
* **Resilient Financial Calculations:** EMI interest calculations up to 12 months, transactional credit management, and automated late fee CRON scheduling.

---

## 🔒 OAuth 2.0 & JWT Security Flow

The system implements stateless **JWT Bearer Token Authentication**:

1. **Authentication Server (`user-service` @ Port 8081):**
   * Exposes `POST /api/auth/register` and `POST /api/auth/login`.
   * Encrypts user credentials via `BCryptPasswordEncoder`.
   * Issues signed **HMAC-SHA256 JWT Tokens** upon successful login.

2. **Resource Server (`ledger-service` @ Port 8082):**
   * Employs `JwtAuthenticationFilter` to validate Bearer tokens (`Authorization: Bearer <token>`).
   * Protects credit deduction, checkout, and repayment endpoints from unauthorized access.

---

## 📅 Flexible EMI & Interest Rate Matrix

Customers can select flexible repayment tenures at checkout:

| Tenure | Repayment Plan | Interest Rate | Database Schedule Created |
| :--- | :--- | :--- | :--- |
| **1 Month** | Pay in 30 Days | **0% Interest** | 1 Full Payment |
| **3 Months** | Low-Cost EMI | **5% Total Interest** | 3 Monthly Installments |
| **6 Months** | Standard EMI | **10% Total Interest** | 6 Monthly Installments |
| **9 Months** | Extended EMI | **12% Total Interest** | 9 Monthly Installments |
| **12 Months** | Full-Year EMI | **15% Total Interest** | 12 Monthly Installments |

---

## 🏗️ Backend System Architecture

```
                            +---------------------------------------+
                            |         Postman / Swagger UI          |
                            +---------------------------------------+
                                      /                   \
                        1. POST /api/auth/login          2. POST /api/ledger/checkout
                      (Get Bearer JWT Token)             [Header: Authorization: Bearer token]
                                    /                       \
                                   v                         v
                     +---------------------------+     +---------------------------+
                     |       user-service        |     |      ledger-service       |
                     |  (Auth & Credit Manager)  |     |   (Resource & EMI Server) |
                     +---------------------------+     +---------------------------+
                                   ^                                 |
                                   |                                 | Publishes Events
                                   |                                 v
                     =================================================================
                                            APACHE KAFKA BROKER
                     Topics: [bnpl-checkout-events] [bnpl-repayment-events]
                     =================================================================
                                   |                                 |
                     Consumes      |                                 | Consumes
                     Events        v                                 v Events
                     +---------------------------+     +---------------------------+
                     |       user-service        |     |   notification-service    |
                     |  (Credit Sync & Updates)  |     |        (Port 8083)        |
                     +---------------------------+     +---------------------------+
                                   |                                 |
                        [ PostgreSQL: user_db ]             [ Logs & Alerts ]
```

---

## ⚙️ Microservices Breakdown

### 1. 👤 `user-service` (Port 8081)
*   **Database:** `user_db`
*   **Responsibilities:** Registration, Login, JWT Token Generation, Credit Account Management (Default ₹10,000 limit), Balance Deduction & Restoration.

### 2. 🧾 `ledger-service` (Port 8082)
*   **Database:** `ledger_db`
*   **Responsibilities:** BNPL Checkout Execution, **1 to 12 Month EMI Schedule Generation**, Repayment Processing, Inter-Service REST WebClient Calls, and Automated Late Fee `@Scheduled` CRON Jobs.

### 3. 📲 `notification-service` (Port 8083)
*   **Responsibilities:** Event Consumer listening asynchronously to Kafka topics (`bnpl-checkout-events`, `bnpl-repayment-events`) and logging simulated alerts.

---

## 🚀 Quick Start (Docker Compose)

Run the entire microservices stack (Zookeeper + Kafka + PostgreSQL + 3 Microservices) with one command:

```bash
docker compose up --build
```

---

## 📖 Interactive Swagger UI Testing

Backend endpoints are documented and testable interactively via Swagger UI:

| Service | Port | Swagger Dashboard URL |
| :--- | :--- | :--- |
| **User Service** | `8081` | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **Ledger Service** | `8082` | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |

---

## 📝 License & Author

Developed by **Vikas Singh Rawat** as a showcase backend project for Fintech Software Engineering roles.

Distributed under the MIT License. See `LICENSE` for more information.
