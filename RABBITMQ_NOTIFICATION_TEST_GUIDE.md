# HƯỚNG DẪN TEST TÍCH HỢP RABBITMQ VÀ NOTIFICATION

## Tổng quan

Đã tích hợp RabbitMQ để gửi thông báo tự động khi:
1. **Thêm chương mới** → Thông báo đến tất cả user đang follow truyện đó
2. **Nạp tiền thành công** → Thông báo đến user đã nạp tiền
3. **Mua truyện thành công** → Thông báo đến user đã mua truyện

## Chuẩn bị

### 1. Đảm bảo RabbitMQ đang chạy
- Mở RabbitMQ Management UI: http://localhost:15672
- Login: guest/guest
- Kiểm tra các exchanges và queues đã được tạo

### 2. Khởi động các services theo thứ tự:
```cmd
# 1. User Service (Port 8081)
cd user-service
mvnw spring-boot:run

# 2. Story Service (Port 8083)
cd story-service
mvnw spring-boot:run

# 3. Payment Service (Port 8084)
cd payment-service
mvnw spring-boot:run

# 4. Favourite Service (Port 8085)
cd favourite-service
mvnw spring-boot:run

# 5. Notification Service (Port 8086)
cd notification-service
mvnw spring-boot:run

# 6. API Gateway (Port 8080)
cd api-gateway
mvnw spring-boot:run
```

## TEST 1: THÔNG BÁO CHƯƠNG MỚI

### Bước 1: Đăng ký và đăng nhập 2 user
**User 1 (Tác giả):**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "author1",
  "password": "123456",
  "email": "author1@test.com"
}
```

**User 2 (Reader):**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "reader1",
  "password": "123456",
  "email": "reader1@test.com"
}
```

**Login User 1:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "author1",
  "password": "123456"
}
```
→ Lưu `accessToken` của author1

**Login User 2:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "reader1",
  "password": "123456"
}
```
→ Lưu `accessToken` của reader1

### Bước 2: Author1 tạo truyện mới
```
POST http://localhost:8080/api/story
Authorization: Bearer {author1_token}
Content-Type: application/json

{
  "title": "Truyện Test Notification",
  "description": "Truyện để test notification",
  "genres": ["Action", "Adventure"],
  "paid": false,
  "price": 0
}
```
→ Lưu lại `storyId` (ví dụ: 1)

### Bước 3: Reader1 follow truyện
```
POST http://localhost:8080/api/follows
Authorization: Bearer {reader1_token}
Content-Type: application/json

{
  "storyId": 1
}
```

### Bước 4: Author1 thêm chương mới
```
POST http://localhost:8080/api/story/1/chapters
Authorization: Bearer {author1_token}
Content-Type: application/json

{
  "chapterNumber": 1,
  "title": "Chương 1: Khởi đầu",
  "imageIds": []
}
```

### Bước 5: Kiểm tra notification
**Check console của notification-service:**
```
📚 New chapter: Chương 1: Khởi đầu for story: Truyện Test Notification
```

**Kiểm tra trong database:**
```sql
SELECT * FROM notifications WHERE type = 'NEW_CHAPTER';
```

**Hoặc gọi API để lấy notifications của reader1:**
```
GET http://localhost:8080/api/notifications
Authorization: Bearer {reader1_token}
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "userId": 2,
    "type": "NEW_CHAPTER",
    "message": "Truyện 'Truyện Test Notification' vừa có chương mới: Chương 1: Khởi đầu",
    "relatedId": 1,
    "isRead": false,
    "createdAt": "2025-11-18T10:00:00"
  }
]
```

---

## TEST 2: THÔNG BÁO NẠP TIỀN

### Bước 1: Đăng nhập user
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "reader1",
  "password": "123456"
}
```
→ Lưu `accessToken`

### Bước 2: Tạo deposit request
```
POST http://localhost:8080/api/payment/deposit
Authorization: Bearer {reader1_token}
Content-Type: application/json

{
  "amount": 100000,
  "description": "Nạp tiền test notification"
}
```

**Response:**
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "transactionId": "DEPOSIT_1731910800000_2",
  "message": "Payment URL created successfully"
}
```

### Bước 3: Mở URL thanh toán trong browser
- Copy `paymentUrl` và mở trong browser
- Chọn ngân hàng NCB
- Số thẻ: `9704198526191432198`
- Tên: `NGUYEN VAN A`
- Ngày phát hành: `07/15`
- Mật khẩu OTP: `123456`

### Bước 4: Kiểm tra notification sau khi thanh toán thành công

**Check console của notification-service:**
```
💰 Deposit success: 100000 for userId=2
```

**Kiểm tra trong database:**
```sql
SELECT * FROM notifications WHERE type = 'DEPOSIT_SUCCESS';
```

**Hoặc gọi API:**
```
GET http://localhost:8080/api/notifications
Authorization: Bearer {reader1_token}
```

**Expected Response:**
```json
[
  {
    "id": 2,
    "userId": 2,
    "type": "DEPOSIT_SUCCESS",
    "message": "Bạn đã nạp thành công 100,000 VND vào tài khoản",
    "relatedId": 1,
    "isRead": false,
    "createdAt": "2025-11-18T10:05:00"
  }
]
```

---

## TEST 3: THÔNG BÁO MUA TRUYỆN

### Bước 1: Author tạo truyện premium
```
POST http://localhost:8080/api/story
Authorization: Bearer {author1_token}
Content-Type: application/json

{
  "title": "Truyện Premium Test",
  "description": "Truyện premium để test notification",
  "genres": ["Fantasy"],
  "paid": true,
  "price": 50000
}
```
→ Lưu `storyId` (ví dụ: 2)

### Bước 2: Reader mua truyện
```
POST http://localhost:8080/api/payment/purchase-story
Authorization: Bearer {reader1_token}
Content-Type: application/json

{
  "storyId": 2,
  "price": 50000
}
```

**Response:**
```json
{
  "id": 2,
  "userId": 2,
  "storyId": 2,
  "transactionId": "PURCHASE-xxx-xxx-xxx-xxx",
  "amount": 50000,
  "status": "SUCCESS",
  "paymentType": "PURCHASE"
}
```

### Bước 3: Kiểm tra notification

**Check console của notification-service:**
```
📖 Purchase story: Truyện Premium Test by userId=2
```

**Kiểm tra trong database:**
```sql
SELECT * FROM notifications WHERE type = 'PURCHASE_STORY';
```

**Hoặc gọi API:**
```
GET http://localhost:8080/api/notifications
Authorization: Bearer {reader1_token}
```

**Expected Response:**
```json
[
  {
    "id": 3,
    "userId": 2,
    "type": "PURCHASE_STORY",
    "message": "Bạn đã mua thành công truyện 'Truyện Premium Test'",
    "relatedId": 2,
    "isRead": false,
    "createdAt": "2025-11-18T10:10:00"
  }
]
```

---

## TEST 4: KIỂM TRA RABBITMQ

### Kiểm tra trong RabbitMQ Management UI

1. Mở http://localhost:15672
2. Login: guest/guest
3. Vào tab **Exchanges** - kiểm tra:
   - `new-chapter-exchange`
   - `deposit-exchange`
   - `payment-exchange`

4. Vào tab **Queues** - kiểm tra:
   - `new-chapter-queue`
   - `deposit-queue`
   - `payment-queue`

5. Xem message rate và confirm các message đã được consume

---

## TROUBLESHOOTING

### 1. Không nhận được notification
**Kiểm tra:**
- RabbitMQ có đang chạy không?
- Tất cả services đã khởi động chưa?
- Check console log của notification-service
- Kiểm tra exchanges và queues trong RabbitMQ UI

### 2. Lỗi "Could not autowire FavouriteServiceClient"
**Giải pháp:**
```
# Rebuild story-service
cd story-service
mvnw clean install
mvnw spring-boot:run
```

### 3. Lỗi "Could not autowire StoryServiceClient"
**Giải pháp:**
```
# Rebuild payment-service
cd payment-service
mvnw clean install
mvnw spring-boot:run
```

### 4. Không có followers nên không gửi notification
**Lưu ý:** Chỉ gửi notification chương mới khi có ít nhất 1 user đang follow truyện đó.
**Giải pháp:** Follow truyện trước khi thêm chương mới.

---

## KIỂM TRA API GATEWAY CONFIG

Đảm bảo các endpoint sau được cấu hình đúng trong API Gateway:

### Public endpoints (không cần token):
- `GET /api/story/**`
- `GET /api/follows/story/{storyId}/count`

### Internal endpoints (giữa các services):
- `GET /api/follows/story/{storyId}/followers` (story-service gọi favourite-service)
- `GET /api/story/{storyId}/title` (payment-service gọi story-service)

---

## KẾT QUẢ MONG ĐỢI

Sau khi test thành công, bạn sẽ thấy:

✅ **Console logs:**
- `📚 Sent new chapter notification for story: ... to X followers`
- `💰 Deposit success event sent to RabbitMQ for userId: ...`
- `📖 Purchase notification sent to RabbitMQ for userId: ...`

✅ **Database:**
- Bảng `notifications` có các bản ghi mới với type tương ứng

✅ **RabbitMQ:**
- Messages đã được publish và consume thành công
- Message count = 0 (đã được consume hết)

---

## GHI CHÚ

1. **Performance:** Các thông báo được gửi bất đồng bộ qua RabbitMQ, không ảnh hưởng đến response time của API chính.

2. **Error Handling:** Nếu gửi notification thất bại, không làm fail transaction chính (deposit, purchase, create chapter).

3. **Scalability:** Có thể deploy nhiều instance của notification-service để xử lý nhiều thông báo hơn.

4. **Retry:** RabbitMQ sẽ tự động retry nếu notification-service bị down tạm thời.

