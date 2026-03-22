# University Tech Conference Event Platform

A cloud-native event management system built with microservices architecture, designed for university technology conferences. Purpose-built to learn and demonstrate **AWS cloud deployment** and **CI/CD pipeline** implementations using free-tier AWS services.

---

## Monorepo Structure

```
Event-Management-System-CTSE/
├── user-service/           # Manages user accounts & authentication
├── event-service/          # Manages conference events (planned)
├── registration-service/   # Handles event registrations (planned)
└── notification-service/   # Sends confirmation notifications (planned)
```

---

## Microservices Overview

| Service                | Port | Responsibility                                   | Status     |
|------------------------|------|--------------------------------------------------|------------|
| `user-service`         | 8081 | User registration, login, JWT auth               | ✅ Active  |
| `event-service`        | 8082 | CRUD for conference events                       | 🔜 Planned |
| `registration-service` | 8083 | Register users for events, validate via User Svc | 🔜 Planned |
| `notification-service` | 8084 | Email/push notifications on registration         | 🔜 Planned |

---

## Database Strategy

- **Single database** (Supabase / PostgreSQL) with **isolated tables per service**
- Each service owns its schema and queries only its own tables
- No cross-service direct DB access — inter-service communication via REST APIs

---

## Example Conference Events

- AI Workshop
- Cybersecurity Talk
- Cloud Computing Workshop
- Startup Pitch Event

---

## Tech Stack

| Layer     | Technology                                |
|-----------|-------------------------------------------|
| Backend   | Spring Boot 3.x (Java 17)                |
| Database  | Supabase (PostgreSQL)                     |
| Auth      | JWT (Spring Security)                     |
| Cloud     | AWS (EC2 / ECS / ECR / RDS — free tier)  |
| CI/CD     | GitHub Actions                            |
| Container | Docker                                    |

---

## Getting Started

Each service is independently runnable. See the `README.md` inside each service folder for setup instructions.
