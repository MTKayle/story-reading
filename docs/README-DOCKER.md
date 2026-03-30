# Story Reading Platform - Production Deployment Guide

[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

## 📋 System Overview

Production-ready microservices architecture with 6 core services:

| Service | Port | Description | Dependencies |
|---------|------|-------------|--------------|
| **API Gateway** | 8081 | Spring Cloud Gateway - Entry point | All services |
| **User Service** | 8882 | Authentication & User Management | PostgreSQL, RabbitMQ |
| **Story Service** | 8085 | Story Content Management | PostgreSQL, RabbitMQ |
| **Payment Service** | 8084 | VNPay Payment Integration | PostgreSQL, RabbitMQ |
| **Comment Service** | 8883 | Comments & Ratings | PostgreSQL, RabbitMQ |
| **Notification Service** | 8087 | Email & Push Notifications | PostgreSQL, RabbitMQ |

**Infrastructure:**
- PostgreSQL 15 (5 isolated databases)
- RabbitMQ 3 (Message broker with management UI)
- Docker Network (Isolated bridge network)

---

## 🚀 Quick Start

### Prerequisites

1. **Docker Desktop** must be installed and running
   - Windows: [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
   - Verify installation:
     ```bash
     docker --version
     docker-compose --version
     ```

2. **System Requirements**
   - RAM: Minimum 8GB (Recommended 16GB)
   - Disk: 10GB free space
   - CPU: 4 cores recommended

### One-Command Deployment

#### Windows:
```bash
deploy.bat
```

#### Linux/Mac:
```bash
chmod +x deploy.sh
./deploy.sh
```

#### Manual Docker Compose:
```bash
docker-compose -f docker-compose.prod.yml up --build -d
```

### Verify Deployment

```bash
# Check all containers are running
docker-compose -f docker-compose.prod.yml ps

# Expected output: 8 containers (6 services + postgres + rabbitmq)
# All should show "Up" status

# Check logs
docker-compose -f docker-compose.prod.yml logs -f
```


---

## 🏗️ Architecture

### Service Communication Flow

```
Client Request
    ↓
API Gateway (8081)
    ↓
┌─────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│             │              │              │              │              │
User Service  Story Service  Payment       Comment        Notification
(8882)        (8085)         Service       Service        Service
│             │              (8084)        (8883)         (8087)
│             │              │              │              │
└─────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
                    ↓                           ↓
            ┌───────────────┐           ┌──────────────┐
            │  PostgreSQL   │           │  RabbitMQ    │
            │  (5 DBs)      │           │  (Messaging) │
            └───────────────┘           └──────────────┘
```

### Docker Network Architecture

- **Network**: `story-network` (bridge driver)
- **DNS Resolution**: Services communicate via service names (e.g., `http://user-service:8882`)
- **Port Mapping**: All services exposed to host for development/debugging
- **Isolation**: Services cannot access host network directly

### Multi-Stage Docker Build

Each service uses optimized multi-stage builds:

1. **Build Stage**: 
   - Base: `eclipse-temurin:17-jdk`
   - Compiles Java source with Maven
   - Produces executable JAR

2. **Production Stage**:
   - Base: `eclipse-temurin:17-jre` (smaller image)
   - Only contains runtime + JAR
   - Reduces image size by ~40%

---

## 📦 Configuration

### Environment Variables

All services are configured via environment variables in `docker-compose.prod.yml`:

#### Database Configuration
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/{dbname}
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres123
SPRING_JPA_HIBERNATE_DDL_AUTO: update
```

#### RabbitMQ Configuration
```yaml
SPRING_RABBITMQ_HOST: rabbitmq
SPRING_RABBITMQ_PORT: 5672
SPRING_RABBITMQ_USERNAME: guest
SPRING_RABBITMQ_PASSWORD: guest
```

#### JWT Configuration
```yaml
JWT_SECRET: mySuperSecretKeyForJwtAuth1234567890
JWT_EXPIRATION: 3600000  # 1 hour
```

#### VNPay Configuration (Payment Service)
```yaml
VNPAY_TMN_CODE: S0AUOO0R
VNPAY_HASH_SECRET: M5ALW73FN5F89ZV97KUMKWMKD212IJWL
VNPAY_URL: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

### Database Schema

PostgreSQL automatically creates 5 isolated databases:

| Database | Service | Purpose |
|----------|---------|---------|
| `userdb` | User Service | Users, authentication, profiles |
| `storydb` | Story Service | Stories, chapters, categories |
| `paymentdb` | Payment Service | Transactions, payment history |
| `commentdb` | Comment Service | Comments, ratings, reactions |
| `notificationdb` | Notification Service | Notifications, email logs |

**Auto-initialization**: Databases are created via `init-databases.sql` on first startup.

---

## 🔧 Operations

### Service Management

#### Start All Services
```bash
docker-compose -f docker-compose.prod.yml up -d
```

#### Stop All Services
```bash
docker-compose -f docker-compose.prod.yml down
```

#### Restart Specific Service
```bash
docker-compose -f docker-compose.prod.yml restart user-service
```

#### Rebuild and Restart Service
```bash
docker-compose -f docker-compose.prod.yml up --build -d user-service
```

#### Scale Service (Horizontal Scaling)
```bash
docker-compose -f docker-compose.prod.yml up -d --scale user-service=3
```

### Logging

#### View All Logs (Follow Mode)
```bash
docker-compose -f docker-compose.prod.yml logs -f
```

#### View Specific Service Logs
```bash
docker-compose -f docker-compose.prod.yml logs -f user-service
```

#### View Last 100 Lines
```bash
docker-compose -f docker-compose.prod.yml logs --tail=100 user-service
```

#### Export Logs to File
```bash
docker-compose -f docker-compose.prod.yml logs > logs.txt
```

### Health Checks

#### Check Container Status
```bash
docker-compose -f docker-compose.prod.yml ps
```

#### Check Resource Usage
```bash
docker stats
```

#### Inspect Service Health
```bash
docker inspect --format='{{json .State.Health}}' user-service | jq
```

### Database Operations

#### Connect to PostgreSQL
```bash
docker exec -it postgres psql -U postgres
```

#### List All Databases
```bash
docker exec -it postgres psql -U postgres -c "\l"
```

#### Backup Database
```bash
docker exec postgres pg_dump -U postgres userdb > backup_userdb.sql
```

#### Restore Database
```bash
docker exec -i postgres psql -U postgres userdb < backup_userdb.sql
```

#### Reset All Databases (⚠️ Destructive)
```bash
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml up -d
```

### RabbitMQ Management

#### Access Management UI
- URL: http://localhost:15672
- Username: `guest`
- Password: `guest`

#### List Queues via CLI
```bash
docker exec rabbitmq rabbitmqctl list_queues
```

#### Check RabbitMQ Status
```bash
docker exec rabbitmq rabbitmq-diagnostics status
```

---

## 🌐 API Documentation

### Base URL
All requests go through API Gateway: `http://localhost:8081`

### Authentication

#### Register New User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "SecurePass123",
  "fullName": "John Doe"
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
  "expiresIn": 3600000
}
```

#### Get User Profile
```http
GET /api/user/profile
Authorization: Bearer {token}
```

### Story Management

#### List Stories
```http
GET /api/story?page=0&size=20
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
  "title": "Story Title",
  "description": "Story description",
  "categoryId": 1
}
```

### Payment

#### Create VNPay Payment
```http
POST /api/payment/vnpay/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 100000,
  "orderInfo": "Payment for story subscription"
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
  "content": "Great story!"
}
```

### Notifications

#### Get User Notifications
```http
GET /api/notification
Authorization: Bearer {token}
```

---

## 🔍 Troubleshooting

### Common Issues

#### 1. Docker Desktop Not Running
**Error**: `The system cannot find the file specified`

**Solution**:
```bash
# Start Docker Desktop
# Wait for "Docker Desktop is running" in system tray
# Verify:
docker ps
```

#### 2. Port Already in Use
**Error**: `Bind for 0.0.0.0:8081 failed: port is already allocated`

**Solution**:
```bash
# Find process using port
netstat -ano | findstr :8081

# Kill process (Windows)
taskkill /PID {process_id} /F

# Or change port in docker-compose.prod.yml
ports:
  - "8082:8081"  # Map to different host port
```

#### 3. Service Won't Start
**Error**: Container exits immediately

**Solution**:
```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs user-service

# Common causes:
# - Database connection failed (check postgres is healthy)
# - RabbitMQ connection failed (check rabbitmq is healthy)
# - Application error (check application logs)

# Restart with fresh state
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

#### 4. Database Connection Failed
**Error**: `Connection refused` or `Unknown database`

**Solution**:
```bash
# Check PostgreSQL is running
docker-compose -f docker-compose.prod.yml ps postgres

# Check databases exist
docker exec -it postgres psql -U postgres -c "\l"

# Recreate databases
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml up -d
```

#### 5. Out of Memory
**Error**: Container killed or OOMKilled

**Solution**:
```bash
# Increase Docker Desktop memory limit
# Settings > Resources > Memory > 8GB+

# Or limit service memory in docker-compose.prod.yml
services:
  user-service:
    deploy:
      resources:
        limits:
          memory: 512M
```

#### 6. Build Failed
**Error**: Maven build errors

**Solution**:
```bash
# Clean build
docker-compose -f docker-compose.prod.yml build --no-cache user-service

# Check Maven wrapper permissions
chmod +x user-service/mvnw

# Build locally first to verify
cd user-service
./mvnw clean package
```

### Debug Mode

#### Enter Container Shell
```bash
docker exec -it user-service bash
```

#### Check Java Process
```bash
docker exec user-service ps aux | grep java
```

#### Test Database Connection from Container
```bash
docker exec -it user-service bash
apt-get update && apt-get install -y postgresql-client
psql -h postgres -U postgres -d userdb
```

#### Test RabbitMQ Connection
```bash
docker exec -it user-service bash
apt-get update && apt-get install -y curl
curl http://rabbitmq:15672/api/overview
```

---

## 📈 Performance Optimization

### 1. JVM Tuning

Add to service environment in `docker-compose.prod.yml`:

```yaml
environment:
  JAVA_OPTS: >
    -Xms256m
    -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UseStringDeduplication
```

### 2. Database Connection Pooling

Already configured in `application.properties`:

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### 3. Enable Caching

Add Redis for caching (optional):

```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  networks:
    - story-network
```

### 4. Resource Limits

Set resource limits to prevent resource exhaustion:

```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

---

## 🔐 Security Best Practices

### ⚠️ CRITICAL: Before Production Deployment

1. **Change All Default Passwords**
   ```yaml
   # PostgreSQL
   POSTGRES_PASSWORD: {use-strong-password}
   
   # RabbitMQ
   RABBITMQ_DEFAULT_USER: {custom-username}
   RABBITMQ_DEFAULT_PASS: {use-strong-password}
   ```

2. **Update JWT Secret**
   ```yaml
   JWT_SECRET: {generate-random-256-bit-key}
   ```
   Generate secure key:
   ```bash
   openssl rand -base64 32
   ```

3. **Use Environment Files**
   Create `.env` file (add to `.gitignore`):
   ```bash
   POSTGRES_PASSWORD=secure_password_here
   JWT_SECRET=secure_jwt_secret_here
   RABBITMQ_PASSWORD=secure_rabbitmq_password
   ```

4. **Enable HTTPS**
   - Use reverse proxy (Nginx/Traefik)
   - Configure SSL certificates
   - Redirect HTTP to HTTPS

5. **Network Security**
   ```yaml
   # Remove port mappings for internal services
   # Only expose API Gateway
   api-gateway:
     ports:
       - "443:8081"
   
   # Internal services - no port mapping
   user-service:
     expose:
       - "8882"
   ```

6. **Update VNPay Credentials**
   - Use production credentials
   - Store in environment variables
   - Never commit to Git

7. **Enable RabbitMQ Authentication**
   ```bash
   docker exec rabbitmq rabbitmqctl add_user admin {password}
   docker exec rabbitmq rabbitmqctl set_user_tags admin administrator
   docker exec rabbitmq rabbitmqctl delete_user guest
   ```

### Security Checklist

- [ ] All default passwords changed
- [ ] JWT secret updated (256-bit random)
- [ ] Environment variables in `.env` file
- [ ] `.env` added to `.gitignore`
- [ ] HTTPS enabled
- [ ] Internal services not exposed to host
- [ ] RabbitMQ guest user disabled
- [ ] Database backups configured
- [ ] Log rotation enabled
- [ ] Monitoring/alerting configured

---

## 📊 Monitoring & Observability

### Basic Monitoring

#### Container Health
```bash
# Real-time stats
docker stats

# Health check status
docker inspect --format='{{.State.Health.Status}}' user-service
```

#### Application Logs
```bash
# Centralized logging
docker-compose -f docker-compose.prod.yml logs -f > app.log

# Error logs only
docker-compose -f docker-compose.prod.yml logs | grep ERROR
```

### Advanced Monitoring (Optional)

Add Prometheus + Grafana stack:

```yaml
prometheus:
  image: prom/prometheus
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana
  ports:
    - "3000:3000"
  environment:
    GF_SECURITY_ADMIN_PASSWORD: admin
```

---

## 🚀 Deployment Strategies

### Blue-Green Deployment

1. Deploy new version with different project name:
   ```bash
   docker-compose -p story-reading-green -f docker-compose.prod.yml up -d
   ```

2. Test new version

3. Switch traffic (update load balancer)

4. Remove old version:
   ```bash
   docker-compose -p story-reading-blue down
   ```

### Rolling Updates

Update services one at a time:

```bash
# Update user-service
docker-compose -f docker-compose.prod.yml up -d --no-deps --build user-service

# Verify
docker-compose -f docker-compose.prod.yml logs -f user-service

# Continue with other services
```

### Zero-Downtime Deployment

1. Scale up new version:
   ```bash
   docker-compose -f docker-compose.prod.yml up -d --scale user-service=2
   ```

2. Health check new instances

3. Remove old instances:
   ```bash
   docker stop {old-container-id}
   ```

---

## 📝 Maintenance

### Regular Tasks

#### Daily
- Check container health: `docker-compose -f docker-compose.prod.yml ps`
- Review error logs: `docker-compose -f docker-compose.prod.yml logs | grep ERROR`

#### Weekly
- Backup databases
- Review disk usage: `docker system df`
- Clean unused images: `docker image prune -a`

#### Monthly
- Update base images
- Security patches
- Performance review

### Backup Strategy

#### Automated Backup Script

Create `backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="./backups/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# Backup all databases
for db in userdb storydb paymentdb commentdb notificationdb; do
  docker exec postgres pg_dump -U postgres $db > $BACKUP_DIR/${db}.sql
done

# Compress
tar -czf $BACKUP_DIR.tar.gz $BACKUP_DIR
rm -rf $BACKUP_DIR

echo "Backup completed: $BACKUP_DIR.tar.gz"
```

#### Schedule with Cron (Linux)

```bash
# Run daily at 2 AM
0 2 * * * /path/to/backup.sh
```

---

## 📞 Support & Resources

### Documentation
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

### Useful Commands Reference

```bash
# Quick reference card
docker-compose -f docker-compose.prod.yml up -d          # Start all
docker-compose -f docker-compose.prod.yml down           # Stop all
docker-compose -f docker-compose.prod.yml ps             # Status
docker-compose -f docker-compose.prod.yml logs -f        # Logs
docker-compose -f docker-compose.prod.yml restart        # Restart all
docker system prune -a                                   # Clean all
docker stats                                             # Resource usage
```

---

## 📄 License

This project is proprietary software. All rights reserved.

---

**Last Updated**: March 2026  
**Version**: 1.0.0  
**Maintained by**: Story Reading Platform Team
