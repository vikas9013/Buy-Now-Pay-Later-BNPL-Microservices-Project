# 💳 Buy Now, Pay Later (BNPL) - Microservices Architecture

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A production-grade, distributed **Buy Now, Pay Later (BNPL)** backend application designed for high throughput, scalability, and financial data integrity. Built using **Java 17**, **Spring Boot 3**, **Spring WebFlux (WebClient)**, **PostgreSQL**, and **Docker**.

---

## 📌 Executive Summary

Buy Now, Pay Later (BNPL) is one of the fastest-growing fintech sectors, offering short-term, zero-cost credit to consumers at checkout. This repository implements a decoupled, event-ready microservices architecture that mimics real-world fintech infrastructures like Klarna, Affirm, and Slice.

---

## 🏗️ System Architecture

The application is decoupled into two independent microservices communicating over HTTP using non-blocking Spring **WebClient**:

```
                              +---------------------------------------+
                              |         Postman / Client / UI         |
                              +---------------------------------------+
                                        /                   \
                        (Onboard & Credit)                 (Checkout & Repay)
                                      /                       \
                                     v                         v
                       +---------------------------+     +---------------------------+
                       |       user-service        |     |      ledger-service       |
                       |        (Port 8081)        |     |        (Port 8082)        |
                       +---------------------------+     +---------------------------+
                                     |    ^                            |
                                     |    | Inter-Service REST         |
                                     |    | (WebClient)                |
                                     |    +----------------------------+
                                     v                                 v
                        +-------------------------+       +-------------------------+
                        |  PostgreSQL (user_db)   |       |  PostgreSQL (ledger_db) |
                        +-------------------------+       +-------------------------+
```

---

## ⚙️ Microservices Breakdown

### 1. 👤 `user-service` (Port 8081)
*   **Database:** `user_db`
*   **Core Responsibilities:**
    *   Customer Onboarding & KYC Management.
    *   Risk-based Credit Limit Allocation (Default: ₹10,000.00).
    *   Real-time Credit Balance Deduction and Restoration.
    *   Account Status Enforcement (ACTIVE, BLOCKED, OVERDUE).

### 2. 🧾 `ledger-service` (Port 8082)
*   **Database:** `ledger_db`
*   **Core Responsibilities:**
    *   BNPL Checkout Execution & Merchant Transactions.
    *   Inter-service communication via Spring `WebClient` to verify/deduct credit limits.
    *   30-Day Repayment Schedule Generation.
    *   Repayment Processing & Credit Restoration.
    *   Automated `@Scheduled` CRON Job for applying ₹500 late fees to overdue accounts.

---

## 🛠️ Tech Stack & Tools

*   **Core Language:** Java 17 (LTS)
*   **Frameworks:** Spring Boot 3.2.4, Spring Data JPA, Spring WebFlux
*   **Database:** PostgreSQL (Relational, ACID Compliant)
*   **Boilerplate Reduction:** Lombok
*   **API Documentation:** SpringDoc OpenAPI 3 / Swagger UI
*   **Containerization:** Docker, Docker Compose
*   **Build Tool:** Apache Maven

---

## 🚀 Getting Started

### Prerequisites
*   [Java 17 JDK](https://adoptium.net/)
*   [Apache Maven](https://maven.apache.org/)
*   [Docker Desktop](https://www.docker.com/products/docker-desktop/) *(Optional for containerized run)*

---

### Option 1: Run with Docker Compose (Recommended)

Spin up the entire microservices stack (PostgreSQL + User Service + Ledger Service) with a single command:

```bash
docker compose up --build
```

---

### Option 2: Run Concurrently via PowerShell Script

If running locally without Docker:

```powershell
.\run-all.ps1
```
*This automatically launches both microservices in separate background windows.*

---

## 📖 Interactive Swagger UI Testing

Once services are running, test all APIs interactively directly in your browser:

| Service | Port | Swagger Dashboard URL |
| :--- | :--- | :--- |
| **User Service** | `8081` | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **Ledger Service** | `8082` | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |

---

## 🧪 End-to-End API Flow Test Guide

### 1. Onboard a User (`user-service`)
```http
POST /api/users/onboard
Host: localhost:8081
Content-Type: application/json

{
  "fullName": "Vikas Singh Rawat",
  "email": "vikas@example.com",
  "phoneNumber": "9876543210"
}
```
*Response: Returns generated `userId` and grants ₹10,000 credit limit.*

### 2. Perform BNPL Checkout (`ledger-service`)
```http
POST /api/ledger/checkout
Host: localhost:8082
Content-Type: application/json

{
  "userId": "<COPIED_USER_ID>",
  "amount": 3500.00,
  "merchantName": "Amazon"
}
```
*Response: `ledger-service` calls `user-service` over HTTP -> verifies credit -> deducts ₹3,500 -> creates transaction & 30-day repayment schedule.*

### 3. Check Updated Credit Limit (`user-service`)
```http
GET /api/users/<COPIED_USER_ID>/credit
Host: localhost:8081
```
*Response: Available credit balance automatically reflects **₹6,500.00**.*

### 4. Process Repayment (`ledger-service`)
```http
POST /api/ledger/repay
Host: localhost:8082
Content-Type: application/json

{
  "repaymentId": "<COPIED_REPAYMENT_ID>",
  "amount": 3500.00
}
```
*Response: Repayment marked as paid -> notifies `user-service` over HTTP -> credit limit fully restored to **₹10,000.00**!*

---

## 🔮 Future Scope & Roadmap

*   [ ] **Apache Kafka Integration:** Replace HTTP polling with async event streams for order completion & notifications.
*   [ ] **API Gateway & Eureka:** Implement Spring Cloud Gateway and Netflix Eureka for dynamic service discovery.
*   [ ] **ML Underwriting:** Python microservice using Machine Learning for dynamic risk scoring and credit limit assignment.

---

## 📝 License & Author

Developed by **Vikas Singh Rawat** as a showcase project for Fintech Software Engineering roles.

Distributed under the MIT License. See `LICENSE` for more information.
