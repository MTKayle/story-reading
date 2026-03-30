# 🔐 Security Guide - Story Reading Platform

## ⚠️ CRITICAL: Exposed Secrets Found

### Files Currently Containing Hardcoded Secrets

The following files contain **hardcoded secrets** that should **NEVER** be committed to Git:

#### 1. Docker Compose Files
- ✅ `docker-compose.prod.yml` - **KEEP** (will use .env)
- ✅ `docker-compose.yml` - **KEEP** (will use .env)

**Secrets Found:**
- PostgreSQL password: `postgres123`
- JWT secret: `mySuperSecretKeyForJwtAuth1234567890`
- Email password: `jfibgkvmrumnzfzx`
- VNPay credentials: `S0AUOO0R`, `M5ALW73FN5F89ZV97KUMKWMKD212IJWL`

#### 2. Application Properties Files
**⚠️ These files should be gitignored or use environment variables:**

- `user-service/src/main/resources/application.properties`
  - Database password
  - JWT secret
  - RabbitMQ password

- `story-service/src/main/resources/application.properties`
  - Database password
  - JWT secret
  - RabbitMQ password

- `payment-service/src/main/resources/application.properties`
  - Database password
  - VNPay credentials
  - RabbitMQ password

- `comment-service/src/main/resources/application.properties`
  - Database password
  - RabbitMQ password

- `notification-service/src/main/resources/application.properties`
  - Database password
  - Email credentials (Gmail app password)
  - RabbitMQ password

- `api-gateway/src/main/resources/application.yml`
  - JWT secret

---

## 🛡️ Immediate Actions Required

### Step 1: Create .env File

```bash
# Copy example file
cp .env.example .env

# Edit with your actual credentials
nano .env  # or use your preferred editor
```

### Step 2: Update Docker Compose to Use .env

The `docker-compose.prod.yml` should reference environment variables:

```yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
  SPRING_MAIL_PASSWORD: ${MAIL_PASSWORD}
  # etc...
```

### Step 3: Create Application Properties Templates

For each service, create `application.properties.example`:

```properties
# Example for user-service
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
spring.rabbitmq.password=${RABBITMQ_PASSWORD}
```

### Step 4: Rotate All Exposed Secrets

**All secrets in the current codebase are compromised and must be changed:**

#### Database Password
```bash
# Generate new password
openssl rand -base64 32
```

#### JWT Secret
```bash
# Generate 256-bit secret
openssl rand -base64 32
```

#### Email Password
- Go to: https://myaccount.google.com/apppasswords
- Generate new app password
- Update in .env file

#### VNPay Credentials
- Contact VNPay support
- Request new credentials
- Update in .env file

#### RabbitMQ Password
```bash
# Generate new password
openssl rand -base64 16
```

---

## 📋 Security Checklist

### Before Production Deployment

- [ ] All secrets moved to `.env` file
- [ ] `.env` file added to `.gitignore`
- [ ] All hardcoded secrets removed from code
- [ ] All exposed secrets rotated (changed)
- [ ] `application.properties` files use environment variables
- [ ] Docker Compose files use environment variables
- [ ] `.env.example` created with placeholder values
- [ ] Security documentation updated
- [ ] Team members notified of secret rotation

### Git Repository Cleanup

If secrets were already committed:

```bash
# Remove sensitive files from Git history
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch docker-compose.prod.yml" \
  --prune-empty --tag-name-filter cat -- --all

# Or use BFG Repo-Cleaner (recommended)
bfg --delete-files docker-compose.prod.yml
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

⚠️ **Warning**: This rewrites Git history. Coordinate with team before doing this.

### GitHub/GitLab Secret Scanning

If your repository is on GitHub/GitLab:

1. Check for secret scanning alerts
2. Revoke all exposed secrets immediately
3. Generate new credentials
4. Update `.env` file with new credentials

---

## 🔒 Best Practices

### 1. Never Commit Secrets

**Bad:**
```properties
spring.datasource.password=postgres123
jwt.secret=mySuperSecretKeyForJwtAuth1234567890
```

**Good:**
```properties
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

### 2. Use Environment Variables

**Docker Compose:**
```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
```

**Spring Boot:**
```properties
spring.datasource.password=${DB_PASSWORD:default_value}
```

### 3. Separate Configs by Environment

```
application.properties          # Common config
application-dev.properties      # Development (can commit)
application-prod.properties     # Production (NEVER commit)
```

### 4. Use Secret Management Tools

**For Production:**
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault
- Google Secret Manager
- Kubernetes Secrets

### 5. Implement Secret Rotation

- Rotate secrets every 90 days
- Use automated rotation where possible
- Keep audit logs of secret access

---

## 🚨 Incident Response

### If Secrets Are Exposed

1. **Immediate Actions** (within 1 hour):
   - Revoke/rotate all exposed credentials
   - Check access logs for unauthorized access
   - Notify security team

2. **Short-term Actions** (within 24 hours):
   - Remove secrets from Git history
   - Update all services with new credentials
   - Review and update security policies

3. **Long-term Actions** (within 1 week):
   - Implement secret management solution
   - Set up monitoring and alerting
   - Conduct security audit
   - Train team on security best practices

---

## 📊 Current Risk Assessment

### High Risk Items

| Item | Risk Level | Action Required |
|------|------------|-----------------|
| Gmail App Password | 🔴 HIGH | Rotate immediately |
| VNPay Credentials | 🔴 HIGH | Contact VNPay, get new credentials |
| JWT Secret | 🔴 HIGH | Generate new 256-bit secret |
| Database Password | 🟡 MEDIUM | Change to strong password |
| RabbitMQ Password | 🟡 MEDIUM | Change to strong password |

### Exposed in Files

| File | Secrets | Status |
|------|---------|--------|
| `docker-compose.prod.yml` | All secrets | ⚠️ Needs .env migration |
| `notification-service/application.properties` | Email password | ⚠️ Needs env vars |
| `payment-service/application.properties` | VNPay credentials | ⚠️ Needs env vars |
| `user-service/application.properties` | JWT secret, DB password | ⚠️ Needs env vars |
| `api-gateway/application.yml` | JWT secret | ⚠️ Needs env vars |

---

## 🔧 Migration Guide

### Step-by-Step: Migrate to Environment Variables

#### 1. Update application.properties

**Before:**
```properties
spring.datasource.password=postgres123
jwt.secret=mySuperSecretKeyForJwtAuth1234567890
```

**After:**
```properties
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

#### 2. Update docker-compose.prod.yml

**Before:**
```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: postgres123
  JWT_SECRET: mySuperSecretKeyForJwtAuth1234567890
```

**After:**
```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
```

#### 3. Create .env file

```bash
cp .env.example .env
# Edit .env with actual values
```

#### 4. Test locally

```bash
# Load .env and start services
docker-compose -f docker-compose.prod.yml --env-file .env up -d
```

#### 5. Verify

```bash
# Check environment variables are loaded
docker exec user-service env | grep JWT_SECRET
```

---

## 📚 Additional Resources

- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [GitHub Secret Scanning](https://docs.github.com/en/code-security/secret-scanning)
- [12-Factor App: Config](https://12factor.net/config)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

---

## 📞 Security Contact

For security issues:
- Create a **private** security advisory
- Email: security@yourdomain.com
- Do NOT create public issues for security vulnerabilities

---

**Last Updated**: March 2026  
**Severity**: HIGH  
**Action Required**: IMMEDIATE
