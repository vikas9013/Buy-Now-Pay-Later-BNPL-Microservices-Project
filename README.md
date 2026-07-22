# 💳 Buy Now, Pay Later (BNPL) - Event-Driven Microservices Architecture

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-brightgreen.svg)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-Event--Driven-red.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A production-grade, event-driven **Buy Now, Pay Later (BNPL)** microservices backend application built with **Java 17**, **Spring Boot 3**, **Apache Kafka**, **Spring WebFlux (WebClient)**, **PostgreSQL**, and **Docker**.

---

## 📌 Executive Summary

Buy Now, Pay Later (BNPL) requires asynchronous, highly resilient architectures to handle real-time checkout volume spikes without cascading failures. This system leverages **Apache Kafka Event Streaming** to decouple core transaction processing from credit management and automated notifications.

---

## 🏗️ System Architecture (Event-Driven)

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
*   **Responsibilities:** Customer Onboarding, Risk-Based Credit Limit Allocation (Default ₹10,000.00), Credit Balance Deduction & Restoration.
*   **Kafka Role:** Consumes `bnpl-checkout-events` and `bnpl-repayment-events` to log and synchronize credit limit events asynchronously.

### 2. 🧾 `ledger-service` (Port 8082)
*   **Database:** `ledger_db`
*   **Responsibilities:** BNPL Checkout Execution, 30-Day Repayment Schedules, Repayments, and Automated Late Fee CRON Jobs.
*   **Kafka Role:** Acts as an **Event Producer**. Publishes `CheckoutEvent` and `RepaymentEvent` to Kafka topics upon successful transactions.

### 3. 📲 `notification-service` (Port 8083)
*   **Responsibilities:** Listens to Kafka topics asynchronously and handles real-time SMS & Email notification dispatches without blocking checkout threads.
*   **Kafka Role:** Acts as an **Event Consumer** listening to `bnpl-checkout-events` and `bnpl-repayment-events`.

---

## 🛠️ Tech Stack & Tools

*   **Core Language:** Java 17 (LTS)
*   **Frameworks:** Spring Boot 3.2.4, Spring Data JPA, Spring WebFlux, Spring Kafka
*   **Event Broker:** Apache Kafka & Zookeeper
*   **Database:** PostgreSQL (Relational, ACID Compliant)
*   **API Documentation:** SpringDoc OpenAPI 3 / Swagger UI
*   **Containerization:** Docker & Docker Compose

---

## 🚀 Getting Started (Docker Compose)

Run the entire event-driven system (Zookeeper + Kafka + PostgreSQL + 3 Microservices) with one command:

```bash
docker compose up --build
```

---

## 📖 Interactive Swagger UI Testing

| Service | Port | Swagger Dashboard URL |
| :--- | :--- | :--- |
| **User Service** | `8081` | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **Ledger Service** | `8082` | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |

---

## 📝 License & Author

Developed by **Vikas Singh Rawat** as a showcase project for Fintech Software Engineering roles.

Distributed under the MIT License. See `LICENSE` for more information.
