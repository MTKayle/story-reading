# HƯỚNG DẪN TEST CHỨC NĂNG NẠP TIỀN VỚI VNPAY

## Yêu cầu trước khi test

1. **Cài đặt và chạy RabbitMQ:**
   ```bash
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
   ```
   - Truy cập RabbitMQ Management: http://localhost:15672
   - Username/Password: guest/guest

2. **Tạo database cho payment-service:**
   ```sql
   CREATE DATABASE paymentdb;
   ```

3. **Đăng ký tài khoản VNPay Sandbox:**
   - Truy cập: https://sandbox.vnpayment.vn/
   - Đăng ký tài khoản merchant
   - Lấy TMN_CODE và HASH_SECRET
   - Cập nhật vào file `payment-service/src/main/resources/application.properties`:
     ```properties
     vnpay.tmn-code=YOUR_TMN_CODE
     vnpay.hash-secret=YOUR_HASH_SECRET
     ```

4. **Chạy các services theo thứ tự:**
   - User Service (port 8082)
   - Payment Service (port 8084)

---

## BƯỚC 1: ĐĂNG NHẬP VÀ LẤY TOKEN

### Request: Login
**POST** `http://localhost:8082/api/auth/login`

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "username": "user1",
  "password": "password123"
}
```

**Response mẫu:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidXNlcjEi...",
  "refreshToken": "abc123...",
  "expiresIn": 3600000
}
```

**Lưu ý:** Copy `accessToken` để dùng cho các request tiếp theo

---

## BƯỚC 2: KIỂM TRA SỐ DƯ BAN ĐẦU

### Request: Get Balance
**GET** `http://localhost:8082/api/user/balance`

**Headers:**
```
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response mẫu:**
```json
{
  "userId": 1,
  "username": "user1",
  "balance": 0
}
```

---

## BƯỚC 3: TẠO YÊU CẦU NẠP TIỀN

### Request: Create Deposit Payment
**POST** `http://localhost:8084/api/payment/deposit`

**Headers:**
```
Content-Type: application/json
Accept: application/json
X-User-Id: 1
```

**Body (raw JSON):**
```json
{
  "amount": 100000,
  "description": "Nạp tiền vào tài khoản"
}
```

**Response mẫu:**
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10000000&vnp_Command=pay&...",
  "transactionId": "DEPOSIT_1699123456789_1",
  "message": "Payment URL created successfully"
}
```

**Lưu ý:** 
- Số tiền tối thiểu: 10,000 VND
- Copy `paymentUrl` để thanh toán
- Lưu `transactionId` để kiểm tra sau

---

## BƯỚC 4: THANH TOÁN QUA VNPAY SANDBOX

1. **Mở `paymentUrl`** trong trình duyệt
2. **Nhập thông tin thẻ test VNPay:**
   - Số thẻ: `9704198526191432198`
   - Tên chủ thẻ: `NGUYEN VAN A`
   - Ngày phát hành: `07/15`
   - Mật khẩu OTP: `123456`

3. **Xác nhận thanh toán**
4. Sau khi thanh toán thành công, bạn sẽ được redirect về:
   ```
   http://localhost:8084/api/payment/vnpay/callback?vnp_Amount=...&vnp_ResponseCode=00&...
   ```

5. Hệ thống sẽ tự động:
   - Cập nhật trạng thái payment trong database
   - Gửi event vào RabbitMQ queue: `payment.queue`
   - User-service nhận event và cập nhật số dư

---

## BƯỚC 5: KIỂM TRA KẾT QUẢ

### 5.1. Kiểm tra Payment Status
**GET** `http://localhost:8084/api/payment/transaction/DEPOSIT_1699123456789_1`

**Response mẫu:**
```json
{
  "id": 1,
  "userId": 1,
  "transactionId": "DEPOSIT_1699123456789_1",
  "vnpayTxnRef": "12345678",
  "amount": 100000,
  "status": "SUCCESS",
  "paymentType": "DEPOSIT",
  "description": "Nạp tiền vào tài khoản",
  "vnpayResponseCode": "00",
  "vnpayTransactionNo": "14012345",
  "createdAt": "2025-11-07T10:30:00",
  "updatedAt": "2025-11-07T10:31:00"
}
```

### 5.2. Kiểm tra số dư sau khi nạp
**GET** `http://localhost:8082/api/user/balance`

**Headers:**
```
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response mẫu:**
```json
{
  "userId": 1,
  "username": "user1",
  "balance": 100000
}
```

### 5.3. Xem lịch sử giao dịch
**GET** `http://localhost:8084/api/payment/user/history`

**Headers:**
```
X-User-Id: 1
```

**Response mẫu:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "transactionId": "DEPOSIT_1699123456789_1",
    "amount": 100000,
    "status": "SUCCESS",
    "paymentType": "DEPOSIT",
    "createdAt": "2025-11-07T10:30:00"
  }
]
```

---

## BƯỚC 6: KIỂM TRA RABBITMQ

1. Truy cập RabbitMQ Management Console: http://localhost:15672
2. Username/Password: guest/guest
3. Vào tab **Queues**
4. Tìm queue `payment.queue`
5. Xem:
   - **Messages ready**: Số message đang chờ xử lý (nên là 0)
   - **Messages total**: Tổng số message đã xử lý
   - **Get messages**: Xem nội dung message

---

## KIỂM TRA LOG

### Payment Service Log:
```
INFO  - Created payment record: DEPOSIT_1699123456789_1
INFO  - VNPay callback received for txnRef: 12345678, responseCode: 00
INFO  - Payment success for txnRef: 12345678
INFO  - Payment success event sent to RabbitMQ for transaction: DEPOSIT_1699123456789_1
```

### User Service Log:
```
INFO  - Received payment event: userId=1, transactionId=DEPOSIT_1699123456789_1, amount=100000, type=DEPOSIT
INFO  - Updated balance for user 1: 0 -> 100000
```

---

## TEST CASE NÂNG CAO

### Test Case 1: Nạp tiền nhiều lần
1. Nạp 50,000 VND → Balance = 50,000
2. Nạp 100,000 VND → Balance = 150,000
3. Nạp 200,000 VND → Balance = 350,000

### Test Case 2: Thanh toán thất bại
- Nhập sai OTP hoặc hủy giao dịch
- Kiểm tra payment status = "FAILED"
- Số dư user không thay đổi

### Test Case 3: Số tiền không hợp lệ
**Body:**
```json
{
  "amount": 5000
}
```
**Response:**
```json
{
  "amount": "Minimum deposit amount is 10,000 VND"
}
```

---

## XỬ LÝ LỖI THƯỜNG GẶP

### Lỗi 1: "Connection refused" khi gọi API
**Nguyên nhân:** Service chưa chạy
**Giải pháp:** 
```bash
cd payment-service
mvn spring-boot:run
```

### Lỗi 2: "Could not connect to RabbitMQ"
**Nguyên nhân:** RabbitMQ chưa chạy
**Giải pháp:**
```bash
docker start rabbitmq
```

### Lỗi 3: "Invalid secure hash"
**Nguyên nhân:** HASH_SECRET không đúng
**Giải pháp:** Kiểm tra lại cấu hình VNPay trong application.properties

### Lỗi 4: User không nhận được tiền
**Nguyên nhân:** 
- RabbitMQ không hoạt động
- User-service không lắng nghe queue
**Giải pháp:**
1. Kiểm tra log user-service
2. Kiểm tra RabbitMQ queue có message không
3. Restart user-service

---

## POSTMAN COLLECTION

Tạo Collection trong Postman với các request sau:

### Variables:
```
base_url_user: http://localhost:8082
base_url_payment: http://localhost:8084
access_token: (set sau khi login)
user_id: 1
```

### Requests:
1. **Login** → Save `accessToken` to variable
2. **Get Balance Before** → Kiểm tra số dư ban đầu
3. **Create Deposit** → Lưu `paymentUrl` và `transactionId`
4. **Get Payment Status** → Kiểm tra trạng thái payment
5. **Get Balance After** → Kiểm tra số dư sau nạp tiền
6. **Get Payment History** → Xem lịch sử giao dịch

---

## LƯU Ý QUAN TRỌNG

1. **VNPay Sandbox**: Chỉ dùng để test, không xử lý tiền thật
2. **X-User-Id Header**: Trong production nên lấy từ JWT token thay vì truyền trực tiếp
3. **RabbitMQ Retry**: Nếu user-service xử lý lỗi, RabbitMQ sẽ retry message
4. **Transaction Idempotency**: Mỗi transaction chỉ được xử lý 1 lần
5. **Security**: Nên thêm validation và authentication cho các API

---

## KIẾN TRÚC LUỒNG DỮ LIỆU

```
Client (Postman)
    ↓ POST /deposit
Payment-Service
    ↓ Save to DB (PENDING)
    ↓ Return VNPay URL
Client Browser
    ↓ Open VNPay URL
VNPay Gateway
    ↓ Payment Success
    ↓ Callback
Payment-Service
    ↓ Update DB (SUCCESS)
    ↓ Publish Event
RabbitMQ Queue
    ↓ Consume Event
User-Service
    ↓ Update User Balance
    ↓ Save to DB
```

---

**Chúc bạn test thành công! 🎉**
