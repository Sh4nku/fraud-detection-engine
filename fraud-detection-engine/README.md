# Fraud Detection Engine

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Kafka](https://img.shields.io/badge/Kafka-KRaft-black)
![Redis](https://img.shields.io/badge/Redis-7-red)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)

A real-time fraud detection microservice that consumes payment events from the Payment Processing Service via Kafka, applies a configurable rule-based risk scoring engine, and manages fraud cases with full audit trail.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Payment Processing Service                  │
│                    (Port 8080)                           │
└───────────────────────┬─────────────────────────────────┘
                        │
              Kafka Topic: payment-transactions
                        │
┌───────────────────────▼─────────────────────────────────┐
│               Fraud Detection Engine                     │
│                    (Port 8081)                           │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │           Kafka Consumer                        │    │
│  │    Listens: payment-transactions                │    │
│  └──────────────────┬──────────────────────────────┘    │
│                     │                                    │
│  ┌──────────────────▼──────────────────────────────┐    │
│  │           Fraud Rule Engine                     │    │
│  │  ┌─────────────┐  ┌──────────────────────────┐  │    │
│  │  │ Rule checks │  │    Redis Velocity        │  │    │
│  │  │ HIGH_AMOUNT │  │    Tracker               │  │    │
│  │  │ ROUND_AMT   │  │  (Sliding Window)        │  │    │
│  │  │ NIGHT_TIME  │  └──────────────────────────┘  │    │
│  │  │ VELOCITY    │                                 │    │
│  │  │ REPEAT_DEST │                                 │    │
│  │  └─────────────┘                                 │    │
│  └──────────────────┬──────────────────────────────┘    │
│                     │                                    │
│  ┌──────────────────▼──────────────────────────────┐    │
│  │           Risk Scorer                           │    │
│  │   0-30: SAFE | 31-60: SUSPICIOUS               │    │
│  │   61-80: HIGH_RISK | 81-100: FRAUD             │    │
│  └──────────────────┬──────────────────────────────┘    │
│                     │                                    │
│  ┌──────────────────▼──────────────────────────────┐    │
│  │        Fraud Case Management                    │    │
│  │     PostgreSQL (fraud_cases table)              │    │
│  └──────────────────┬──────────────────────────────┘    │
│                     │                                    │
│  ┌──────────────────▼──────────────────────────────┐    │
│  │           Kafka Producer                        │    │
│  │    Publishes: fraud-alerts                      │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 15 |
| Messaging | Apache Kafka (KRaft) |
| Cache | Redis 7 (Velocity Tracking) |
| Security | JWT |
| Testing | JUnit 5 + Mockito |
| DevOps | Docker Compose (shared with Payment Service) |

---

## Fraud Rules

| Rule | Score | Trigger Condition |
|---|---|---|
| HIGH_AMOUNT | +40 | Amount > ₹50,000 |
| VELOCITY_EXCEEDED | +30 | > 5 transactions in 60 seconds |
| NIGHT_TIME_TRANSACTION | +20 | Between 1AM - 4AM |
| ROUND_AMOUNT | +10 | Exact multiples of ₹1,000 |
| REPEATED_DESTINATION | +25 | Same destination 3+ times/hour |

## Risk Levels

| Score | Level | Action |
|---|---|---|
| 0–30 | SAFE | No action |
| 31–60 | SUSPICIOUS | Flag for review |
| 61–80 | HIGH_RISK | Immediate review |
| 81–100 | FRAUD | Block + Alert |

---

## Key Features

- **Real-time Kafka consumption** from Payment Processing Service
- **Configurable rule engine** — fraud rules defined in `application.properties`
- **Redis sliding window** velocity tracking per account
- **Fraud case management** with full lifecycle (OPEN → UNDER_REVIEW → CONFIRMED_FRAUD / FALSE_POSITIVE)
- **Immutable audit trail** for every fraud decision
- **Kafka fraud alerts** published back for downstream consumption
- **JWT-based stateless authentication**
- **Duplicate transaction protection** — idempotency check before processing

---

## Project Structure

```
src/main/java/com/fraud/
├── fraudcase/
│   ├── controller/     → Case management REST endpoints
│   ├── service/        → Fraud processing logic
│   ├── repository/     → JPA queries
│   ├── entity/         → FraudCase, CaseStatus, RiskLevel
│   └── dto/            → Request/Response models
├── rule/               → FraudRuleEngine — evaluates all rules
├── score/              → RiskScore, RiskLevel calculation
├── velocity/           → Redis sliding window tracker + controller
├── audit/              → Immutable fraud audit logs
├── kafka/              → Consumer, Producer, Event models
├── security/           → JWT filter, Security config
└── exception/          → Global error handling
```

---

## Running Locally

### Prerequisites
- Java 21
- Maven
- Payment Processing Service running on port 8080
- Docker containers running (shared infrastructure)

### Step 1 — Start Infrastructure (if not already running)
```bash
docker compose up -d
```

### Step 2 — Run Application
```bash
mvn spring-boot:run
```
Application starts on **port 8081**

### Step 3 — Run Tests
```bash
mvn test
```

---

## API Reference

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/token` | Generate JWT token |

### Fraud Cases
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/fraud/cases` | Get all fraud cases |
| GET | `/api/v1/fraud/cases/{id}` | Get case by ID |
| GET | `/api/v1/fraud/cases/transaction/{txnId}` | Get case by transaction |
| GET | `/api/v1/fraud/cases/status/{status}` | Filter by status |
| GET | `/api/v1/fraud/cases/account/{accountNumber}` | Cases by account |
| PATCH | `/api/v1/fraud/cases/{id}/review` | Review a case |

### Velocity
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/fraud/velocity/{accountNumber}` | Get velocity stats |
| DELETE | `/api/v1/fraud/velocity/{accountNumber}/reset` | Reset velocity |

---

## End-to-End Flow

```
1. POST /api/v1/transactions/transfer (Port 8080)
         ↓
2. Payment Service processes transfer
         ↓
3. Kafka event → payment-transactions topic
         ↓
4. Fraud Engine consumes event
         ↓
5. Rule Engine evaluates:
   - Amount ₹80,000 > ₹50,000 → HIGH_AMOUNT (+40)
   - Amount is round number   → ROUND_AMOUNT (+10)
   - Total score: 50          → SUSPICIOUS
         ↓
6. Fraud case created (status: OPEN)
         ↓
7. Fraud alert published → fraud-alerts topic
         ↓
8. Analyst reviews via PATCH /api/v1/fraud/cases/{id}/review
         ↓
9. Case status → CONFIRMED_FRAUD
```

---

## Sample API Calls

### Review a Fraud Case
```bash
curl -X PATCH http://localhost:8081/api/v1/fraud/cases/{caseId}/review \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED_FRAUD",
    "reviewedBy": "analyst@hsbc.com",
    "reviewNotes": "Confirmed fraudulent transaction"
  }'
```

### Check Velocity Stats
```bash
curl -X GET http://localhost:8081/api/v1/fraud/velocity/{accountNumber} \
  -H "Authorization: Bearer <token>"
```

---

## Design Decisions

**Why separate service instead of adding to Payment Service?**
Fraud detection and payment processing have different scaling, deployment, and team ownership requirements. Kafka decouples them — if the fraud engine is down, payments still process and events are replayed when it recovers.

**Why Redis for velocity tracking?**
Redis atomic increment operations with TTL provide an efficient sliding window counter without database load. Each velocity check is O(1).

**Why rule-based over ML?**
Rule-based engines are auditable, explainable, and regulatory-compliant — critical for banking. ML models are a future enhancement on top of this foundation.

---

## Related Project
- [Payment Processing Service](https://github.com/Sh4nku/payment-processing-service) — publishes payment events consumed by this service
