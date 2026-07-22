# Buy Now, Pay Later (BNPL) Microservices Backend

A modern, production-grade **Buy Now, Pay Later (BNPL)** backend system built with **Java 17**, **Spring Boot 3**, **Spring WebFlux (WebClient)**, **PostgreSQL**, and **Docker**.

---

## 🏛️ Architecture Overview

The system is decoupled into two independent microservices communicating over HTTP using Spring **WebClient**:

*   **`user-service` (Port 8081):** Manages user onboarding, KYC status, and credit limit allocations (default ₹10,000).
*   **`ledger-service` (Port 8082):** Manages BNPL checkout transactions, schedules 30-day repayment cycles, processes repayments, and executes automated late fee CRON jobs.

---

## 🚀 Quick Start (Docker Compose)

Run the entire system (PostgreSQL + Microservices) with a single command:

```bash
docker compose up --build
```

---

## 🌐 Swagger UI Documentation

Access the interactive API dashboards once the services are running:

*   **User Service Swagger:** `http://localhost:8081/swagger-ui.html`
*   **Ledger Service Swagger:** `http://localhost:8082/swagger-ui.html`

---

## 🛠️ Tech Stack

*   **Language:** Java 17
*   **Framework:** Spring Boot 3, Spring WebFlux, Spring Data JPA
*   **Database:** PostgreSQL
*   **Documentation:** OpenAPI 3 / Swagger UI
*   **Containerization:** Docker & Docker Compose
