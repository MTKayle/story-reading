# ⚠️ SECURITY WARNING - ACTION REQUIRED

## 🚨 Critical Security Issue Detected

**Multiple secrets and credentials are currently hardcoded in the repository!**

### Exposed Credentials

The following sensitive information is currently exposed:

1. **Database Password**: `postgres123`
2. **JWT Secret**: `mySuperSecretKeyForJwtAuth1234567890`
3. **Gmail App Password**: `jfibgkvmrumnzfzx`
4. **VNPay TMN Code**: `S0AUOO0R`
5. **VNPay Hash Secret**: `M5ALW73FN5F89ZV97KUMKWMKD212IJWL`
6. **RabbitMQ Password**: `guest`

### Files Affected

- `docker-compose.prod.yml`
- `docker-compose.yml`
- `user-service/src/main/resources/application.properties`
- `story-service/src/main/resources/application.properties`
- `payment-service/src/main/resources/application.properties`
- `comment-service/src/main/resources/application.properties`
- `notification-service/src/main/resources/application.properties`
- `api-gateway/src/main/resources/application.yml`

---

## ✅ Immediate Actions (Do This Now!)

### 1. Create .env File

```bash
cp .env.example .env
```

Edit `.env` with your actual credentials (see `.env.example` for template)

### 2. Verify .env is Gitignored

```bash
# Check if .env is in .gitignore
cat .gitignore | grep "\.env"

# Should see: .env
```

### 3. Rotate All Exposed Secrets

**All secrets in this repository are compromised and MUST be changed:**

#### Generate New JWT Secret
```bash
openssl rand -base64 32
```

#### Generate New Database Password
```bash
openssl rand -base64 32
```

#### Get New Gmail App Password
1. Go to: https://myaccount.google.com/apppasswords
2. Generate new app password
3. Update in `.env`

#### Get New VNPay Credentials
- Contact VNPay support
- Request new credentials for production

### 4. Update Configuration Files

See detailed migration guide in: **[docs/SECURITY.md](docs/SECURITY.md)**

---

## 📋 Quick Setup Guide

### For Development

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Edit with your credentials
nano .env

# 3. Start services
docker-compose -f docker-compose.prod.yml up -d
```

### For Production

**DO NOT use the exposed credentials in production!**

1. Generate all new secrets
2. Update `.env` file
3. Use secret management service (AWS Secrets Manager, Vault, etc.)
4. Enable secret rotation

---

## 📚 Documentation

- **[docs/SECURITY.md](docs/SECURITY.md)** - Complete security guide
- **[.env.example](.env.example)** - Environment variables template
- **[docs/README-DOCKER.md](docs/README-DOCKER.md)** - Deployment guide

---

## 🔒 Security Checklist

Before deploying to production:

- [ ] Created `.env` file from `.env.example`
- [ ] Generated new JWT secret (256-bit)
- [ ] Changed database password
- [ ] Rotated Gmail app password
- [ ] Updated VNPay credentials
- [ ] Changed RabbitMQ password
- [ ] Verified `.env` is in `.gitignore`
- [ ] Tested with new credentials
- [ ] Removed this warning file

---

## ⚡ Quick Fix Commands

```bash
# Generate all new secrets at once
echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env
echo "POSTGRES_PASSWORD=$(openssl rand -base64 32)" >> .env
echo "RABBITMQ_DEFAULT_PASS=$(openssl rand -base64 16)" >> .env

# Edit .env to add remaining credentials
nano .env
```

---

## 🚫 What NOT To Do

- ❌ Do NOT commit `.env` file to Git
- ❌ Do NOT use exposed credentials in production
- ❌ Do NOT share credentials via email/chat
- ❌ Do NOT hardcode secrets in code
- ❌ Do NOT ignore this warning

---

## ✅ After Fixing

Once you've completed all security steps:

1. Delete this file: `rm SECURITY-WARNING.md`
2. Commit changes: `git add .gitignore .env.example`
3. Push: `git push`

---

**Priority**: 🔴 CRITICAL  
**Action Required**: IMMEDIATE  
**Estimated Time**: 30 minutes

For detailed instructions, see: **[docs/SECURITY.md](docs/SECURITY.md)**
