# Chức Năng Nạp Tiền VNPay - Tóm Tắt Hệ Thống

## Tổng Quan

Hệ thống nạp tiền sử dụng:
- **VNPay Sandbox** để thanh toán
- **RabbitMQ** để giao tiếp giữa các microservices
- **PostgreSQL** để lưu trữ dữ liệu

## Luồng Hoạt Động

```
1. User gọi API nạp tiền → Payment Service
2. Payment Service tạo payment record (PENDING)
3. Payment Service trả về VNPay URL
4. User thanh toán trên VNPay
5. VNPay callback về Payment Service
6. Payment Service cập nhật status (SUCCESS)
7. Payment Service gửi event qua RabbitMQ
8. User Service nhận event và cập nhật số dư
```

## Cấu Trúc Thư Mục

```
payment-service/
├── controller/
│   └── PaymentController.java        # API endpoints
├── service/
│   └── PaymentService.java          # Business logic
├── entity/
│   └── Payment.java                 # Payment entity
├── dto/
│   ├── DepositRequest.java          # Request DTO
│   ├── VNPayResponse.java           # Response DTO
│   └── PaymentEvent.java            # Event DTO
├── config/
│   ├── VNPayConfig.java             # VNPay configuration
│   └── RabbitMQConfig.java          # RabbitMQ configuration
├── util/
│   └── VNPayUtil.java               # VNPay utilities
└── repository/
    └── PaymentRepository.java       # Data access

user-service/
├── controller/
│   └── UserController.java          # Thêm endpoint /balance
├── listener/
│   └── PaymentEventListener.java    # RabbitMQ consumer
├── config/
│   └── RabbitMQConfig.java          # RabbitMQ configuration
└── dto/
    └── PaymentEvent.java            # Event DTO
```

## API Endpoints

### Payment Service (Port 8084)

1. **Tạo yêu cầu nạp tiền**
   - POST `/api/payment/deposit`
   - Headers: `X-User-Id`
   - Body: `{ "amount": 100000, "description": "..." }`

2. **VNPay callback**
   - GET `/api/payment/vnpay/callback`
   - Tự động được gọi bởi VNPay

3. **Kiểm tra payment**
   - GET `/api/payment/transaction/{transactionId}`

4. **Lịch sử giao dịch**
   - GET `/api/payment/user/history`
   - Headers: `X-User-Id`

### User Service (Port 8082)

1. **Kiểm tra số dư**
   - GET `/api/user/balance`
   - Headers: `Authorization: Bearer {token}`

## Database Schema

### Payment Service - Table: payments

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_id VARCHAR(255) UNIQUE NOT NULL,
    vnpay_txn_ref VARCHAR(50) UNIQUE,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    description TEXT,
    vnpay_response_code VARCHAR(10),
    vnpay_transaction_no VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### User Service - Table: users (đã có, chỉ thêm balance)

```sql
ALTER TABLE users 
ADD COLUMN balance DECIMAL(15,2) DEFAULT 0 NOT NULL;
```

## RabbitMQ Configuration

- **Exchange**: `payment.exchange` (Topic)
- **Queue**: `payment.queue` (Durable)
- **Routing Key**: `payment.success`

## Dependencies Đã Thêm

### payment-service/pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### user-service/pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## Cấu Hình VNPay Sandbox

1. Đăng ký tại: https://sandbox.vnpayment.vn/
2. Lấy thông tin:
   - TMN_CODE
   - HASH_SECRET
3. Cập nhật trong `payment-service/application.properties`

## Thông Tin Test VNPay

**Thẻ test:**
- Số thẻ: `9704198526191432198`
- Tên: `NGUYEN VAN A`
- Ngày phát hành: `07/15`
- Mật khẩu OTP: `123456`

## Khởi Động Hệ Thống

### 1. Start RabbitMQ
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 2. Tạo Database
```sql
CREATE DATABASE paymentdb;
```

### 3. Start Services
```bash
# Terminal 1: User Service
cd user-service
mvn spring-boot:run

# Terminal 2: Payment Service
cd payment-service
mvn spring-boot:run
```

### 4. Kiểm Tra
- User Service: http://localhost:8082
- Payment Service: http://localhost:8084
- RabbitMQ Management: http://localhost:15672

## Monitoring & Logs

### Kiểm tra RabbitMQ Queue
```bash
# Truy cập: http://localhost:15672
# Login: guest/guest
# Vào tab Queues → payment.queue
```

### Xem Logs
- Payment Service logs: Xem transaction creation và callback
- User Service logs: Xem balance update events

## Troubleshooting

| Vấn đề | Giải pháp |
|--------|-----------|
| Cannot connect to RabbitMQ | `docker start rabbitmq` |
| Payment không cập nhật số dư | Kiểm tra RabbitMQ queue có message không |
| Invalid secure hash | Kiểm tra HASH_SECRET trong config |
| Database connection error | Kiểm tra PostgreSQL và tạo database |

## Testing Flow

1. **Login** → Lấy access token
2. **Check balance** → Xem số dư ban đầu
3. **Create deposit** → Nhận VNPay URL
4. **Open VNPay URL** → Thanh toán với thẻ test
5. **Confirm payment** → Chờ callback
6. **Check balance again** → Xác nhận số dư đã tăng
7. **View history** → Xem lịch sử giao dịch

## Security Notes

⚠️ **Quan trọng cho Production:**

1. Không dùng `X-User-Id` header trực tiếp, lấy từ JWT token
2. Thêm validation cho callback URL
3. Sử dụng HTTPS cho callback URL
4. Thêm rate limiting cho API
5. Log tất cả transactions
6. Implement idempotency key
7. Thêm transaction timeout
8. Encrypt sensitive data

## Next Steps

- [ ] Thêm chức năng mua truyện (deduct balance)
- [ ] Thêm transaction history với pagination
- [ ] Implement refund mechanism
- [ ] Add email notification sau khi nạp tiền
- [ ] Dashboard để quản lý payments
- [ ] Export transaction reports

---

**Xem hướng dẫn chi tiết tại:** `PAYMENT_TEST_GUIDE.md`

