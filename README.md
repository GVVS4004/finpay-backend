# FinPay — Microservices Banking Platform

Full-stack digital banking platform built with Spring Boot microservices, event-driven architecture, real-time notifications, and containerised deployment.

---

## Architecture

![alt text](image-3.png)

### Kafka Topics (Saga Choreography)

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `pending-transactions` | Transaction Service | Account Service | Async debit/credit request |
| `transaction-results` | Account Service | Transaction Service | Result (success/fail) |
| `compensation-events` | Transaction Service | Account Service | Rollback on failure |
| `payment-events` | Transaction Service | Notification Service | Trigger notifications |

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| API Gateway | 8080 | JWT validation, rate limiting, routing |
| Auth Service | 8081 | Register, login, JWT + refresh tokens |
| Account Service | 8082 | Accounts, balances, Redis cache |
| Transaction Service | 8083 | Transfer, deposit, withdraw, saga orchestration |
| Notification Service | 8085 | SSE stream, Kafka consumer, Redis pub/sub |
| Service Registry | 8761 | Eureka — service discovery |

---

## Tech Stack

| | |
|---|---|
| Backend | Spring Boot 3.2.5, Spring Cloud Gateway, Spring Security |
| Messaging | Apache Kafka (saga choreography) |
| Cache / Pub-Sub | Redis 7 (balance cache + rate limiting + SSE fan-out) |
| Database | PostgreSQL 15 |
| Real-time | Server-Sent Events (SSE) |
| Service Discovery | Netflix Eureka |
| Tracing | Zipkin + Micrometer |
| Frontend | React + Vite + MUI |
| Containerisation | Docker + Docker Compose |

---

## Running Locally

```bash
# 1. Build JARs
mvn package -DskipTests

# 2. Start full stack
docker compose up -d

# 3. Wait ~60s for Eureka registration, then hit :8080
```

| URL | |
|---|---|
| `localhost:8080` | API Gateway |
| `localhost:8761` | Eureka Dashboard |
| `localhost:9411` | Zipkin Tracing |
| `localhost:808{1,2,3,5}/swagger-ui.html` | Swagger per service |
