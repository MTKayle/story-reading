# 🔧 Environment Variables Setup Guide

## Cách hoạt động

### Spring Boot Environment Variable Priority

Spring Boot tự động override properties theo thứ tự ưu tiên (cao → thấp):

1. **Environment Variables** (từ Docker Compose) ← **Ưu tiên cao nhất**
2. **application.properties** (trong code)
3. **Default values**

### Ví dụ cụ thể

**File `application.properties`:**
```properties
spring.datasource.password=postgres123
jwt.secret=mySuperSecretKeyForJwtAuth1234567890
```

**Docker Compose environment:**
```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
```

**Kết quả:**
- Khi chạy local (không dùng Docker): Dùng giá trị từ `application.properties`
- Khi chạy Docker: Dùng giá trị từ `.env` file (override properties)

---

## 🚀 Quick Start

### 1. File .env đã được tạo sẵn

File `.env` đã có sẵn với các giá trị development:

```bash
# Xem nội dung
cat .env
```

### 2. Chạy với Docker Compose

```bash
# Docker Compose tự động load .env file
docker-compose -f docker-compose.prod.yml up -d
```

### 3. Verify Environment Variables

```bash
# Kiểm tra env vars trong container
docker exec user-service env | grep JWT_SECRET
docker exec user-service env | grep SPRING_DATASOURCE_PASSWORD
```

---

## 📋 Mapping Environment Variables

### Spring Boot tự động convert

Docker Compose environment variables → Spring Boot properties:

| Docker Env Var | Spring Boot Property |
|----------------|---------------------|
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` |
| `JWT_SECRET` | `jwt.secret` |
| `SPRING_MAIL_PASSWORD` | `spring.mail.password` |
| `VNPAY_TMN_CODE` | `vnpay.tmn-code` |
| `VNPAY_HASH_SECRET` | `vnpay.hash-secret` |

**Quy tắc:**
- Underscore `_` → Dot `.`
- Uppercase → Lowercase
- `SPRING_DATASOURCE_PASSWORD` → `spring.datasource.password`

---

## 🔐 Production Setup

### Bước 1: Tạo .env.production

```bash
cp .env.example .env.production
```

### Bước 2: Generate Secure Secrets

```bash
# JWT Secret (256-bit)
openssl rand -base64 32

# Database Password
openssl rand -base64 32

# RabbitMQ Password
openssl rand -base64 16
```

### Bước 3: Update .env.production

```env
POSTGRES_PASSWORD=<generated_password>
JWT_SECRET=<generated_jwt_secret>
RABBITMQ_DEFAULT_PASS=<generated_rabbitmq_password>
MAIL_PASSWORD=<your_gmail_app_password>
VNPAY_TMN_CODE=<your_production_vnpay_code>
VNPAY_HASH_SECRET=<your_production_vnpay_secret>
```

### Bước 4: Deploy với Production Env

```bash
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d
```

---

## 🧪 Testing Environment Variables

### Test 1: Verify Override

```bash
# Start services
docker-compose -f docker-compose.prod.yml up -d

# Check if env var is loaded
docker exec user-service env | grep JWT_SECRET

# Should show value from .env, NOT from application.properties
```

### Test 2: Test với Custom Value

```bash
# Tạo .env.test
echo "JWT_SECRET=test_secret_123" > .env.test

# Run với custom env
docker-compose -f docker-compose.prod.yml --env-file .env.test up -d

# Verify
docker exec user-service env | grep JWT_SECRET
# Should show: JWT_SECRET=test_secret_123
```

---

## 🔍 Troubleshooting

### Issue 1: Environment Variables không được load

**Triệu chứng:**
```bash
docker exec user-service env | grep JWT_SECRET
# Không thấy gì hoặc thấy giá trị cũ
```

**Giải pháp:**
```bash
# 1. Kiểm tra .env file tồn tại
ls -la .env

# 2. Rebuild containers
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up --build -d

# 3. Verify lại
docker exec user-service env | grep JWT_SECRET
```

### Issue 2: Service vẫn dùng giá trị từ application.properties

**Nguyên nhân:** Environment variable name không đúng format

**Giải pháp:**
```yaml
# SAI
environment:
  jwt.secret: ${JWT_SECRET}

# ĐÚNG
environment:
  JWT_SECRET: ${JWT_SECRET}
```

Spring Boot sẽ tự convert `JWT_SECRET` → `jwt.secret`

### Issue 3: .env file không được gitignore

**Kiểm tra:**
```bash
git status
# Nếu thấy .env trong danh sách → NGUY HIỂM!
```

**Giải pháp:**
```bash
# Xóa .env khỏi Git (nếu đã commit nhầm)
git rm --cached .env
git commit -m "Remove .env from repository"

# Verify .gitignore
cat .gitignore | grep "\.env"
# Should see: .env
```

---

## 📚 Best Practices

### 1. Không commit .env file

```bash
# .gitignore đã có
.env
.env.local
.env.*.local
```

### 2. Sử dụng .env.example làm template

```bash
# Developers mới clone repo
cp .env.example .env
# Sau đó edit .env với credentials thật
```

### 3. Separate environments

```
.env                    # Development (có thể commit với dummy values)
.env.production         # Production (NEVER commit)
.env.staging            # Staging (NEVER commit)
.env.test               # Testing (có thể commit)
```

### 4. Document required variables

Trong `.env.example`, luôn có comment giải thích:

```env
# JWT Secret - Generate with: openssl rand -base64 32
JWT_SECRET=your_jwt_secret_here

# Gmail App Password - Get from: https://myaccount.google.com/apppasswords
MAIL_PASSWORD=your_gmail_app_password
```

---

## 🎯 Summary

### Cách hoạt động:

1. **Development (local):**
   - Chạy service trực tiếp: `./mvnw spring-boot:run`
   - Dùng giá trị từ `application.properties`

2. **Docker (development):**
   - Chạy: `docker-compose -f docker-compose.prod.yml up -d`
   - Docker Compose load `.env` file
   - Environment variables override `application.properties`

3. **Production:**
   - Tạo `.env.production` với secure credentials
   - Chạy: `docker-compose -f docker-compose.prod.yml --env-file .env.production up -d`
   - Environment variables override `application.properties`

### Lợi ích:

✅ **Không cần sửa code** - `application.properties` giữ nguyên
✅ **Flexible** - Dễ dàng switch giữa environments
✅ **Secure** - Secrets không commit vào Git
✅ **Simple** - Docker Compose tự động load .env

---

## 📞 Need Help?

- Check [docs/SECURITY.md](SECURITY.md) for security best practices
- Check [README-DOCKER.md](README-DOCKER.md) for deployment guide
- Check Spring Boot docs: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config

---

**Last Updated**: March 2026
