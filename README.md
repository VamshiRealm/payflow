# PayFlow — Unified Payment Processing System

![Java](https://img.shields.io/badge/Java-23-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7.2-red?style=flat-square&logo=redis)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.1-black?style=flat-square&logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

A production-grade backend payment processing system simulating real-world Indian fintech infrastructure. Supports **Card payments** and **UPI payments** (Collect + Intent flows) with fraud detection, double-entry ledger, async event streaming, and distributed rate limiting.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Docker Infrastructure](#docker-infrastructure)
- [Environment Variables](#environment-variables)
- [Resume Highlights](#resume-highlights)

---

## Features

### Core Payment Features
- **Card Payment Processing** — validate, authorize, capture with masked card storage
- **UPI Collect Flow (Pull)** — merchant sends request, customer approves/declines
- **UPI Intent Flow (Push)** — customer directly pushes payment via VPA
- **Idempotency** — duplicate transaction prevention via idempotency keys
- **Refunds** — full and partial refund support with ledger reversal

### Security & Auth
- JWT-based authentication with role-based access control
- Three roles: `CUSTOMER`, `MERCHANT`, `ADMIN`
- BCrypt password hashing
- Stateless session management

### Financial Integrity
- **Double-Entry Ledger** — every payment creates a DEBIT (customer) + CREDIT (merchant) entry
- **Refund Reversal** — ledger entries reversed on refund
- Transaction history and analytics per user

### Fraud Detection
- Rule-based risk scoring (0–100)
- High-value transaction detection (>₹50,000 → score 50)
- Rapid transaction detection (>5 transactions in 10 minutes → score 40)
- Automatic blocking when risk score ≥ 70

### Infrastructure
- **Redis** — distributed rate limiting (60 requests/minute per user)
- **Kafka** — async event streaming for notifications, settlement, fraud alerts
- **Docker Compose** — one-command infrastructure setup
- **Swagger UI** — interactive API documentation

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Thunder Client / Swagger UI)     │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP Request
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot App (Port 8081)                │
│                                                             │
│  RateLimitFilter (Redis) → JwtAuthFilter → Controllers      │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │AuthController│  │CardPayment  │  │UpiPayment        │   │
│  │             │  │Controller   │  │Controller        │   │
│  └──────┬──────┘  └──────┬──────┘  └────────┬─────────┘   │
│         │                │                   │             │
│         ▼                ▼                   ▼             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   Service Layer                      │   │
│  │  AuthService │ CardPaymentService │ UpiPaymentService│   │
│  │  FraudService │ LedgerService │ TransactionService   │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│         ┌───────────────┼───────────────┐                  │
│         ▼               ▼               ▼                  │
│    PostgreSQL         Redis           Kafka                 │
│    (Persistence)  (Rate Limiting)  (Async Events)          │
└─────────────────────────────────────────────────────────────┘
                                          │
                    ┌─────────────────────┼──────────────────┐
                    ▼                     ▼                   ▼
           NotificationConsumer  SettlementConsumer  FraudLogConsumer
```

### Kafka Event Flow
```
Payment/Refund
      │
      ▼
PaymentEventProducer
      │
      ├── payment.success ──► NotificationConsumer + SettlementConsumer
      ├── payment.failed  ──► NotificationConsumer
      ├── payment.refund  ──► NotificationConsumer + SettlementConsumer
      └── fraud.alert     ──► FraudLogConsumer
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Framework | Spring Boot 4.0.5 |
| Database | PostgreSQL 16 (Docker) |
| ORM | Spring Data JPA + Hibernate 7 |
| Cache / Rate Limit | Redis 7.2 (Docker) |
| Message Broker | Apache Kafka 4.1 + Zookeeper (Docker) |
| Auth | JWT (jjwt 0.11.5) + Spring Security 7 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |
| Containers | Docker + Docker Compose |
| IDE | VS Code + Extension Pack for Java |

---

## Project Structure

```
src/main/java/com/payflow/payflow/
│
├── PayflowApplication.java
│
├── config/
│   ├── KafkaProducerConfig.java
│   ├── KafkaConsumerConfig.java
│   ├── KafkaTopicConfig.java
│   └── RedisConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── CardPaymentController.java
│   ├── UpiPaymentController.java
│   └── TransactionController.java
│
├── service/
│   ├── AuthService.java
│   ├── CardPaymentService.java
│   ├── UpiPaymentService.java
│   ├── UpiValidatorService.java
│   ├── FraudService.java
│   ├── LedgerService.java
│   └── TransactionService.java
│
├── kafka/
│   ├── PaymentEvent.java
│   ├── FraudAlertEvent.java
│   ├── PaymentEventProducer.java
│   ├── NotificationConsumer.java
│   ├── SettlementConsumer.java
│   └── FraudLogConsumer.java
│
├── model/
│   ├── User.java
│   ├── Transaction.java
│   ├── UpiTransaction.java
│   └── LedgerEntry.java
│
├── repository/
│   ├── UserRepository.java
│   ├── TransactionRepository.java
│   ├── UpiTransactionRepository.java
│   └── LedgerRepository.java
│
├── dto/
│   ├── RegisterRequest.java
│   ├── AuthResponse.java
│   ├── CardPaymentRequest.java
│   ├── UpiCollectRequest.java
│   ├── UpiIntentRequest.java
│   ├── UpiStatusResponse.java
│   ├── PaymentResponse.java
│   ├── TransactionHistoryResponse.java
│   ├── AnalyticsSummaryResponse.java
│   ├── RefundRequest.java
│   └── RefundResponse.java
│
└── security/
    ├── JwtUtil.java
    ├── JwtProperties.java
    ├── JwtAuthFilter.java
    ├── RateLimitFilter.java
    └── SecurityConfig.java
```

---

## Getting Started

### Prerequisites

- Java 21+ — [Download Temurin](https://adoptium.net)
- Maven — [Download](https://maven.apache.org/download.cgi)
- Docker Desktop — [Download](https://www.docker.com/products/docker-desktop)
- VS Code with Extension Pack for Java

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/payflow.git
cd payflow
```

### 2. Start Infrastructure (Docker)

```bash
docker-compose up -d
```

This starts all 4 services:

| Container | Port | Purpose |
|---|---|---|
| `payflow-postgres` | 5432 | Database |
| `payflow-redis` | 6379 | Rate limiting |
| `payflow-kafka` | 9092 | Event streaming |
| `payflow-zookeeper` | 2181 | Kafka coordination |

Verify all containers are running:
```bash
docker ps
```

### 3. Configure `application.properties`

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

Everything else is pre-configured for the Docker setup.

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

On Windows:
```bash
mvnw.cmd spring-boot:run
```

The app starts on **http://localhost:8081**

### 5. Open Swagger UI

```
http://localhost:8081/swagger-ui.html
```

All APIs are documented and testable from the browser.

---

## API Documentation

Full interactive documentation available at `http://localhost:8081/swagger-ui.html`

### Auth APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Register a new user | Public |
| POST | `/auth/login` | Login and get JWT token | Public |

**Register Request:**
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@test.com",
  "password": "password123",
  "role": "CUSTOMER"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGci...",
  "email": "rahul@test.com",
  "role": "CUSTOMER"
}
```

---

### Card Payment APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/payments/card/initiate` | Initiate a card payment | Required |
| GET | `/payments/{id}` | Get payment by ID | Required |

**Card Payment Request:**
```json
{
  "cardNumber": "1234567890123456",
  "cardHolderName": "Rahul Sharma",
  "expiryMonth": "12",
  "expiryYear": "26",
  "cvv": "123",
  "amount": 1500.00,
  "merchantId": "merchant-001",
  "description": "Order payment"
}
```

**Headers:**
```
Authorization: Bearer <token>
Idempotency-Key: unique-key-001
```

**Response:**
```json
{
  "transactionId": "uuid-here",
  "status": "CAPTURED",
  "amount": 1500.00,
  "message": "Payment successful",
  "riskScore": 0
}
```

---

### UPI Payment APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/payments/upi/collect` | Initiate collect request (Pull) | Required |
| POST | `/payments/upi/verify` | Approve or decline collect | Required |
| POST | `/payments/upi/intent` | Direct push payment (Intent) | Required |
| GET | `/payments/upi/status/{id}` | Get UPI transaction status | Required |

**UPI Intent Request:**
```json
{
  "payerVpa": "rahul@oksbi",
  "payeeVpa": "merchant@paytm",
  "amount": 500.00,
  "remarks": "Coffee payment",
  "merchantId": "merchant-001"
}
```

**Supported VPA Handles:** `@upi`, `@oksbi`, `@okhdfcbank`, `@okicici`, `@paytm`, `@ybl`

**UPI Collect Verify Request:**
```json
{
  "upiTransactionId": "uuid-here",
  "approved": true
}
```

---

### Transaction APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/transactions` | Get all your transactions | Required |
| GET | `/transactions/{id}` | Get single transaction | Required |
| GET | `/transactions/{id}/ledger` | View ledger entries | Required |
| POST | `/payments/{id}/refund` | Refund a transaction | Required |
| GET | `/analytics/summary` | Get payment analytics | Required |

**Refund Request:**
```json
{
  "amount": 500.00,
  "reason": "Customer requested refund"
}
```

**Analytics Response:**
```json
{
  "totalTransactions": 10,
  "successfulTransactions": 8,
  "failedTransactions": 1,
  "pendingTransactions": 1,
  "totalVolume": 15000.00,
  "cardPayments": 4,
  "upiPayments": 6,
  "successRate": 80.00
}
```

---

## Testing

### Manual Testing with Swagger UI

Visit `http://localhost:8081/swagger-ui.html` for full interactive testing.

### Testing with curl

**Register:**
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@test.com","password":"password123","role":"CUSTOMER"}'
```

**Card Payment:**
```bash
curl -X POST http://localhost:8081/payments/card/initiate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Idempotency-Key: test-001" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"1234567890123456","cardHolderName":"Test User","expiryMonth":"12","expiryYear":"26","cvv":"123","amount":1500.00,"merchantId":"merchant-001","description":"Test"}'
```

### Key Test Scenarios

| Scenario | Expected Result |
|---|---|
| Valid card payment | `status: CAPTURED`, Kafka events fired |
| Duplicate idempotency key | Same transaction returned, no duplicate |
| Amount > ₹50,000 | `riskScore: 50+`, may be blocked |
| Invalid VPA format | `status: FAILED`, Invalid VPA message |
| UPI collect → approve | `PENDING` → `SUCCESS` |
| UPI collect → decline | `PENDING` → `DECLINED` |
| Refund captured transaction | `status: REFUNDED`, ledger reversed |
| > 60 requests/minute | `HTTP 429 Too Many Requests` |

---

## Docker Infrastructure

### `docker-compose.yml` Services

```yaml
services:
  postgres:   PostgreSQL 16 on port 5432
  redis:      Redis 7.2 on port 6379
  zookeeper:  Confluent Zookeeper 7.5.0 on port 2181
  kafka:      Confluent Kafka 7.5.0 on port 9092
```

### Useful Docker Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker logs payflow-kafka --tail 20
docker logs payflow-postgres --tail 20

# List Kafka topics
docker exec -it payflow-kafka kafka-topics --list --bootstrap-server localhost:9092

# Redis CLI
docker exec -it payflow-redis redis-cli
```

---

## Environment Variables

All configuration is in `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/payflow
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

# JWT
jwt.secret=payflow_super_secret_key_change_this_in_production
jwt.expiration=86400000

# Rate Limiting
rate.limit.requests=60
rate.limit.window=60

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
```

> **Production Note:** Always use environment variables or a secrets manager for sensitive values. Never commit real secrets to Git.

---

## Database Schema

```
users
  id (UUID PK), email (unique), password (bcrypt),
  name, role (CUSTOMER/MERCHANT/ADMIN), created_at

transactions
  id (UUID PK), customer_id, merchant_id, amount,
  payment_method (CARD/UPI), status, description,
  masked_card_number, idempotency_key (unique),
  risk_score, created_at, updated_at

upi_transactions
  id (UUID PK), transaction_id (FK), payer_vpa,
  payee_vpa, amount, flow_type (COLLECT/INTENT),
  upi_status, bank_reference_id, expires_at

ledger_entries
  id (UUID PK), transaction_id, account_id,
  entry_type (DEBIT/CREDIT), amount, description, created_at
```

---

## Kafka Topics

| Topic | Producer | Consumers | Trigger |
|---|---|---|---|
| `payment.success` | PaymentEventProducer | NotificationConsumer, SettlementConsumer | Successful payment |
| `payment.failed` | PaymentEventProducer | NotificationConsumer | Failed payment |
| `payment.refund` | PaymentEventProducer | NotificationConsumer, SettlementConsumer | Refund processed |
| `fraud.alert` | PaymentEventProducer | FraudLogConsumer | Risk score ≥ 70 |

---

## Resume Highlights

> After building this project, you can confidently say in interviews:

- Built a **UPI + Card payment system** simulating real-world Indian fintech infrastructure supporting Collect and Intent flows
- Implemented **double-entry ledger** for financial consistency with full and partial refund support
- Designed **idempotent REST APIs** preventing duplicate transactions using unique idempotency keys
- Added **rule-based fraud detection** with risk scoring (0–100) blocking high-risk transactions automatically
- Built **JWT authentication** with role-based access control for Customer, Merchant, and Admin roles
- Implemented **Redis-backed distributed rate limiting** at 60 requests per minute per user
- Used **Apache Kafka** for async event-driven architecture — notifications, merchant settlement, and fraud alerts across decoupled consumer groups
- Containerized all infrastructure using **Docker Compose** enabling one-command environment setup

---


---

## Author

Built as a learning project to simulate real-world Indian fintech backend architecture.

> ⭐ If you found this project helpful, give it a star!