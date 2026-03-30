# 📚 Story Reading Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

> A modern, scalable microservices-based platform for reading and managing stories with integrated payment, comments, and notifications.

---

## 🌟 Features

### Core Functionality
- 📖 **Story Management** - Create, read, update, and manage stories with chapters
- 👥 **User Management** - Registration, authentication, and profile management
- 💳 **Payment Integration** - VNPay payment gateway for premium content
- 💬 **Comments & Ratings** - Interactive commenting and rating system
- 🔔 **Notifications** - Real-time email and push notifications
- 🔐 **JWT Authentication** - Secure token-based authentication
- 🌐 **API Gateway** - Centralized routing and security

### Technical Highlights
- ⚡ **Microservices Architecture** - Independently deployable services
- 🐳 **Docker Support** - One-command deployment
- 🔄 **Message Queue** - RabbitMQ for async communication
- 📊 **Database Per Service** - Isolated PostgreSQL databases
- 🚀 **Production Ready** - Health checks, auto-restart, monitoring
- 🔧 **Spring Cloud Gateway** - Advanced routing and filtering

---

## 🏗️ Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│                    (Web/Mobile Applications)                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway (8081)                          │
│              Spring Cloud Gateway + JWT Security                │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ User Service │    │Story Service │    │Payment Svc   │
│   (8882)     │    │   (8085)     │    │   (8084)     │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                    │
       ▼                   ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│Comment Svc   │    │Notification  │    │              │
│   (8883)     │    │Svc (8087)    │    │              │
└──────┬───────┘    └──────┬───────┘    └──────────────┘
       │                   │
       └───────────┬───────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌──────────────┐      ┌──────────────┐
│ PostgreSQL   │      │  RabbitMQ    │
│  (5 DBs)     │      │  (Messaging) │
└──────────────┘      └──────────────┘
```

### Microservices

| Service | Port | Technology | Database | Description |
|---------|------|------------|----------|-------------|
| **API Gateway** | 8081 | Spring Cloud Gateway | - | Entry point, routing, JWT validation |
| **User Service** | 8882 | Spring Boot + JPA | userdb | User management, authentication |
| **Story Service** | 8085 | Spring Boot + JPA | storydb | Story CRUD, chapters, categories |
| **Payment Service** | 8084 | Spring Boot + Redis | paymentdb | VNPay integration, transactions |
| **Comment Service** | 8883 | Spring Boot + WebSocket | commentdb | Comments, ratings, reactions |
| **Notification Service** | 8087 | Spring Boot + Mail | notificationdb | Email, push notifications |

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 17
- **Build Tool**: Maven
- **API Gateway**: Spring Cloud Gateway
- **Security**: JWT (JJWT 0.11.5)
- **ORM**: Spring Data JPA + Hibernate
- **Validation**: Spring Validation

#### Infrastructure
- **Database**: PostgreSQL 15
- **Message Broker**: RabbitMQ 3
- **Caching**: Redis (Payment, Notification services)
- **Containerization**: Docker + Docker Compose
- **Service Communication**: OpenFeign (REST), RabbitMQ (Async)

#### External Integrations
- **Payment Gateway**: VNPay Sandbox
- **Email**: SMTP (Gmail)
- **File Storage**: Local filesystem

---

## 🚀 Quick Start

### Prerequisites

- **Docker Desktop** (required)
  - [Download for Windows](https://www.docker.com/products/docker-desktop)
  - [Download for Mac](https://www.docker.com/products/docker-desktop)
  - [Download for Linux](https://docs.docker.com/engine/install/)
- **System Requirements**:
  - RAM: 8GB minimum (16GB recommended)
  - Disk: 10GB free space
  - CPU: 4 cores recommended

### One-Command Deployment

#### Windows
```bash
deploy.bat
```

#### Linux/Mac
```bash
chmod +x deploy.sh
./deploy.sh
```

#### Manual Docker Compose
```bash
docker-compose -f docker-compose.prod.yml up --build -d
```

### Verify Installation

```bash
# Check all services are running
docker-compose -f docker-compose.prod.yml ps

# Expected: 8 containers running (6 services + postgres + rabbitmq)

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:8081 | - |
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| PostgreSQL | localhost:5432 | postgres/postgres123 |

---

## 📖 API Documentation

### Base URL
```
http://localhost:8081
```

### Authentication

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "SecurePass123",
  "fullName": "John Doe",
  "email": "user@example.com"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "SecurePass123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "username": "user@example.com",
    "fullName": "John Doe"
  }
}
```

### Stories

#### List Stories
```http
GET /api/story?page=0&size=20&sort=createdAt,desc
```

#### Get Story Details
```http
GET /api/story/{storyId}
```

#### Create Story (Authenticated)
```http
POST /api/story
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "My Amazing Story",
  "description": "A captivating tale...",
  "categoryId": 1,
  "coverImage": "base64_encoded_image"
}
```

#### Update Story
```http
PUT /api/story/{storyId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description"
}
```

#### Delete Story
```http
DELETE /api/story/{storyId}
Authorization: Bearer {token}
```

### Chapters

#### Get Story Chapters
```http
GET /api/story/{storyId}/chapters
```

#### Create Chapter
```http
POST /api/story/{storyId}/chapter
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Chapter 1: The Beginning",
  "content": "Once upon a time...",
  "chapterNumber": 1
}
```

### Comments

#### Get Story Comments
```http
GET /api/comment/story/{storyId}?page=0&size=20
```

#### Post Comment
```http
POST /api/comment
Authorization: Bearer {token}
Content-Type: application/json

{
  "storyId": 1,
  "content": "Great story!",
  "rating": 5
}
```

#### Reply to Comment
```http
POST /api/comment/{commentId}/reply
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "Thank you!"
}
```

### Payments

#### Create Payment
```http
POST /api/payment/vnpay/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 100000,
  "orderInfo": "Premium subscription",
  "orderType": "subscription"
}

Response:
{
  "paymentUrl": "https://sandbox.vnpayment.vn/...",
  "orderId": "ORD123456"
}
```

#### Payment Callback (VNPay)
```http
GET /api/payment/vnpay/callback?vnp_ResponseCode=00&...
```

### Notifications

#### Get User Notifications
```http
GET /api/notification
Authorization: Bearer {token}
```

#### Mark as Read
```http
PUT /api/notification/{notificationId}/read
Authorization: Bearer {token}
```

---

## 🛠️ Development

### Local Development Setup

#### 1. Clone Repository
```bash
git clone <repository-url>
cd story-reading
```

#### 2. Start Infrastructure (PostgreSQL + RabbitMQ)
```bash
docker-compose -f docker-compose.prod.yml up -d postgres rabbitmq
```

#### 3. Run Services Locally

**User Service:**
```bash
cd user-service
./mvnw spring-boot:run
```

**Story Service:**
```bash
cd story-service
./mvnw spring-boot:run
```

**Other Services:**
```bash
cd {service-name}
./mvnw spring-boot:run
```

#### 4. Build JAR Files
```bash
# Build all services
cd user-service && ./mvnw clean package
cd story-service && ./mvnw clean package
cd payment-service && ./mvnw clean package
cd comment-service && ./mvnw clean package
cd notification-service && ./mvnw clean package
cd api-gateway && ./mvnw clean package
```

### Project Structure

```
story-reading/
├── api-gateway/              # API Gateway service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── user-service/             # User management service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── story-service/            # Story management service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── payment-service/          # Payment service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── comment-service/          # Comment service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── notification-service/     # Notification service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.prod.yml   # Production Docker Compose
├── init-databases.sql        # Database initialization
├── deploy.sh                 # Linux/Mac deployment script
├── deploy.bat                # Windows deployment script
├── README.md                 # This file
└── docs/                     # Documentation folder
    └── README-DOCKER.md      # Docker documentation
```

### Configuration Files

Each service has `application.properties` with:
- Database connection
- RabbitMQ configuration
- Service-specific settings
- JWT configuration (where applicable)

**Example** (`user-service/src/main/resources/application.properties`):
```properties
spring.application.name=user-service
server.port=8882

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/userdb
spring.datasource.username=postgres
spring.datasource.password=postgres123

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672

# JWT
jwt.secret=mySuperSecretKeyForJwtAuth1234567890
jwt.expiration=3600000
```

---

## 🧪 Testing

### Run Unit Tests

```bash
# Test specific service
cd user-service
./mvnw test

# Test all services
for dir in */; do
  if [ -f "$dir/pom.xml" ]; then
    cd "$dir"
    ./mvnw test
    cd ..
  fi
done
```

### Integration Testing

```bash
# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Run integration tests
./mvnw verify -P integration-tests
```

### API Testing with cURL

```bash
# Register user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"Test123","fullName":"Test User"}'

# Login
TOKEN=$(curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"Test123"}' \
  | jq -r '.token')

# Get profile
curl http://localhost:8081/api/user/profile \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📊 Monitoring & Operations

### View Logs

```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f user-service

# Last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail=100 user-service
```

### Check Service Health

```bash
# Container status
docker-compose -f docker-compose.prod.yml ps

# Resource usage
docker stats

# Service health
curl http://localhost:8882/actuator/health
```

### Database Management

```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres

# List databases
\l

# Connect to specific database
\c userdb

# List tables
\dt

# Backup database
docker exec postgres pg_dump -U postgres userdb > backup_userdb.sql

# Restore database
docker exec -i postgres psql -U postgres userdb < backup_userdb.sql
```

### RabbitMQ Management

- **Management UI**: http://localhost:15672
- **Username**: guest
- **Password**: guest

```bash
# List queues
docker exec rabbitmq rabbitmqctl list_queues

# Check status
docker exec rabbitmq rabbitmq-diagnostics status
```

---

## 🔐 Security

### Authentication Flow

1. User registers via `/api/auth/register`
2. User logs in via `/api/auth/login` → receives JWT token
3. Client includes token in `Authorization: Bearer {token}` header
4. API Gateway validates token
5. Request forwarded to backend service

### JWT Configuration

- **Algorithm**: HS256
- **Secret**: Configurable via environment variable
- **Expiration**: 1 hour (3600000ms)
- **Claims**: userId, username, roles

### Security Best Practices

⚠️ **Before Production Deployment:**

1. **Change Default Passwords**
   ```yaml
   POSTGRES_PASSWORD: {strong-password}
   RABBITMQ_DEFAULT_PASS: {strong-password}
   ```

2. **Update JWT Secret**
   ```bash
   # Generate secure secret
   openssl rand -base64 32
   ```

3. **Enable HTTPS**
   - Use reverse proxy (Nginx/Traefik)
   - Configure SSL certificates

4. **Environment Variables**
   - Never commit secrets to Git
   - Use `.env` files (add to `.gitignore`)

5. **Network Security**
   - Restrict port exposure
   - Use firewall rules
   - Enable VPC/private networks

See [docs/README-DOCKER.md](docs/README-DOCKER.md) for complete security checklist.

---

## 🚀 Deployment

### Production Deployment

See [docs/README-DOCKER.md](docs/README-DOCKER.md) for comprehensive deployment guide including:
- Docker deployment strategies
- Blue-green deployment
- Rolling updates
- Zero-downtime deployment
- Monitoring setup
- Backup strategies

### Environment Variables

Create `.env` file (add to `.gitignore`):

```env
# Database
POSTGRES_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_jwt_secret_key

# RabbitMQ
RABBITMQ_PASSWORD=your_rabbitmq_password

# Email
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# VNPay
VNPAY_TMN_CODE=your_vnpay_code
VNPAY_HASH_SECRET=your_vnpay_secret
```

### Scaling

```bash
# Scale specific service
docker-compose -f docker-compose.prod.yml up -d --scale user-service=3

# Load balancer required for multiple instances
```

---

## 🐛 Troubleshooting

### Common Issues

#### Docker Desktop Not Running
```bash
# Error: cannot find dockerDesktopLinuxEngine
# Solution: Start Docker Desktop and wait for it to be ready
docker ps  # Verify Docker is running
```

#### Port Already in Use
```bash
# Find process using port
netstat -ano | findstr :8081

# Kill process (Windows)
taskkill /PID {process_id} /F

# Or change port in docker-compose.prod.yml
```

#### Service Won't Start
```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs user-service

# Restart service
docker-compose -f docker-compose.prod.yml restart user-service

# Rebuild service
docker-compose -f docker-compose.prod.yml up --build -d user-service
```

#### Database Connection Failed
```bash
# Check PostgreSQL is running
docker-compose -f docker-compose.prod.yml ps postgres

# Verify databases exist
docker exec -it postgres psql -U postgres -c "\l"

# Reset databases
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml up -d
```

See [docs/README-DOCKER.md](docs/README-DOCKER.md) for detailed troubleshooting guide.

---

## 📚 Documentation

- **[docs/README-DOCKER.md](docs/README-DOCKER.md)** - Complete Docker deployment guide
- **[API Documentation](#-api-documentation)** - REST API reference
- **[Architecture](#-architecture)** - System architecture overview

### External Resources
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

---

## 🤝 Contributing

### Development Workflow

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Standards

- Follow Java coding conventions
- Write unit tests for new features
- Update documentation
- Ensure all tests pass
- Follow commit message conventions

### Commit Message Format

```
type(scope): subject

body

footer
```

**Types**: feat, fix, docs, style, refactor, test, chore

**Example**:
```
feat(user-service): add email verification

- Add email verification endpoint
- Send verification email via notification service
- Update user model with verified flag

Closes #123
```

---

## 📝 License

This project is proprietary software. All rights reserved.

---

## 👥 Team

**Story Reading Platform Team**

- Backend Development
- DevOps & Infrastructure
- API Design
- Database Architecture

---

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Contact the development team
- Check documentation in [docs/README-DOCKER.md](docs/README-DOCKER.md)

---

## 🗺️ Roadmap

### Current Version (v1.0.0)
- ✅ Microservices architecture
- ✅ User authentication & management
- ✅ Story CRUD operations
- ✅ Payment integration (VNPay)
- ✅ Comments & ratings
- ✅ Email notifications
- ✅ Docker deployment

### Upcoming Features
- 🔄 Real-time notifications (WebSocket)
- 🔄 Advanced search & recommendations
- 🔄 Social features (follow, share)
- 🔄 Analytics dashboard
- 🔄 Mobile app support
- 🔄 Multi-language support
- 🔄 CDN integration for images
- 🔄 Elasticsearch integration

### Future Enhancements
- 📋 GraphQL API
- 📋 Kubernetes deployment
- 📋 Service mesh (Istio)
- 📋 Distributed tracing
- 📋 Advanced monitoring (Prometheus + Grafana)
- 📋 CI/CD pipeline
- 📋 Automated testing

---

**Last Updated**: March 2026  
**Version**: 1.0.0  
**Status**: Production Ready 🚀