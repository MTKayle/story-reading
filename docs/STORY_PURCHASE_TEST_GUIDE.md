# Story Purchase Payment System - Postman Test Guide

## Prerequisites
1. **RabbitMQ** must be running on localhost:5672
2. **PostgreSQL** databases must be created:
   - `paymentdb` (port 5432)
   - `userdb` (port 5432)
   - `storydb` (port 5432)
3. All services must be running:
   - user-service (port 8081)
   - story-service (port 8083)
   - payment-service (port 8084)

## System Architecture

### Purchase Flow:
1. User calls payment-service to purchase a story
2. Payment-service calls user-service to check balance and deduct amount (with SELECT FOR UPDATE lock)
3. Payment-service creates a payment record
4. Payment-service publishes event to RabbitMQ
5. Story-service listens to the event and grants access by creating a purchase record
6. Payment-service returns the payment result

---

## Test Scenarios

### Scenario 1: Setup - Create User and Add Balance

#### 1.1 Register a User
```http
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

**Expected Response:**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com"
}
```

#### 1.2 Add Balance to User (Simulate Deposit)
You can either:
- Use VNPay deposit flow (create payment URL and complete payment)
- Or manually update balance in database for testing:

```sql
-- Run in PostgreSQL userdb
UPDATE users SET balance = 500000 WHERE id = 1;
```

---

### Scenario 2: Create a Paid Story

#### 2.1 Create a Paid Story
```http
POST http://localhost:8083/api/story
Content-Type: application/json
X-User-Id: 1

{
  "title": "Premium Story",
  "description": "This is a premium story",
  "author": "Famous Author",
  "genres": "Action,Adventure",
  "isPaid": true,
  "price": 50000
}
```

**Expected Response:**
```json
{
  "id": 1,
  "title": "Premium Story",
  "author": "Famous Author",
  "price": 50000,
  "isPaid": true
}
```

**Note:** Save the story ID for the next steps.

---

### Scenario 3: Purchase Story

#### 3.1 Purchase Story (Main Test)
```http
POST http://localhost:8084/api/payment/purchase-story
Content-Type: application/json
X-User-Id: 1

{
  "storyId": 1,
  "price": 50000
}
```

**Expected Response (Success):**
```json
{
  "id": 1,
  "userId": 1,
  "storyId": 1,
  "transactionId": "PURCHASE-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "amount": 50000,
  "status": "SUCCESS",
  "paymentType": "PURCHASE",
  "description": "Purchase story ID: 1",
  "createdAt": "2025-11-11T10:00:00",
  "updatedAt": "2025-11-11T10:00:00"
}
```

**Expected Response (Insufficient Balance):**
```json
{
  "error": "Story purchase failed: Insufficient balance. Current: 10000, Required: 50000"
}
```

#### 3.2 Verify Balance Deducted
Check user balance in database:
```sql
SELECT id, username, balance FROM users WHERE id = 1;
```

Expected: Balance should be reduced by 50000

#### 3.3 Verify Purchase Access in Story-Service
```http
GET http://localhost:8083/api/story/1/check-access
X-User-Id: 1
```

Or check database:
```sql
-- Run in PostgreSQL storydb
SELECT * FROM purchases WHERE user_id = 1 AND story_id = 1;
```

Expected: A record should exist showing the user has access to the story.

---

### Scenario 4: Test Concurrent Purchase Prevention

#### 4.1 Try to Purchase Same Story Again
```http
POST http://localhost:8084/api/payment/purchase-story
Content-Type: application/json
X-User-Id: 1

{
  "storyId": 1,
  "price": 50000
}
```

**Expected Behavior:**
- Payment will be created
- Balance will be deducted again (if sufficient)
- Story-service will detect duplicate and not create another purchase record
- This is a valid scenario (user might accidentally click twice)

---

### Scenario 5: Test Insufficient Balance

#### 5.1 Create Expensive Story
```http
POST http://localhost:8083/api/story
Content-Type: application/json
X-User-Id: 1

{
  "title": "Very Expensive Story",
  "description": "This costs a lot",
  "author": "Rich Author",
  "genres": "Premium",
  "isPaid": true,
  "price": 999999999
}
```

#### 5.2 Try to Purchase
```http
POST http://localhost:8084/api/payment/purchase-story
Content-Type: application/json
X-User-Id: 1

{
  "storyId": 2,
  "price": 999999999
}
```

**Expected Response:**
```json
{
  "error": "Story purchase failed: Insufficient balance. Current: 450000, Required: 999999999"
}
```

**Verify:**
- Payment record should be created with status "FAILED"
- Balance should NOT be deducted
- No purchase record in story-service

---

### Scenario 6: View Payment History

#### 6.1 Get User Payment History
```http
GET http://localhost:8084/api/payment/user/history
X-User-Id: 1
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "storyId": 1,
    "transactionId": "PURCHASE-xxx",
    "amount": 50000,
    "status": "SUCCESS",
    "paymentType": "PURCHASE",
    "description": "Purchase story ID: 1",
    "createdAt": "2025-11-11T10:00:00"
  },
  {
    "id": 2,
    "userId": 1,
    "storyId": 2,
    "transactionId": "PURCHASE-yyy",
    "amount": 999999999,
    "status": "FAILED",
    "paymentType": "PURCHASE",
    "description": "Error: Insufficient balance...",
    "createdAt": "2025-11-11T10:05:00"
  }
]
```

#### 6.2 Get Specific Transaction
```http
GET http://localhost:8084/api/payment/transaction/PURCHASE-xxx
```

Replace `PURCHASE-xxx` with the actual transaction ID from the purchase response.

---

### Scenario 7: Test Premium Chapter Access Control

#### 7.1 Test Chapter 1 - Free Access (No Login Required)
```http
GET http://localhost:8083/api/story/chapters/1
```

**Expected Response (Success):**
```json
{
  "id": 1,
  "storyId": 1,
  "chapterNumber": 1,
  "title": "Chapter 1",
  "imageIds": ["001.png", "002.png"]
}
```

**Note:** Chapter 1 của truyện premium có thể đọc miễn phí, không cần login.

---

#### 7.2 Test Chapter 2+ - Without Purchase (Should Fail)
```http
GET http://localhost:8083/api/story/chapters/2
X-User-Id: 1
```

**Expected Response (Error - Not Purchased):**
```json
{
  "timestamp": "2025-11-11T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Bạn cần mua truyện premium này để đọc chapter 2"
}
```

---

#### 7.3 Test Chapter 2+ - Without Login (Should Fail)
```http
GET http://localhost:8083/api/story/chapters/2
```

**Note:** Không gửi header X-User-Id

**Expected Response (Error - Login Required):**
```json
{
  "timestamp": "2025-11-11T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Truyện premium yêu cầu đăng nhập để đọc"
}
```

---

#### 7.4 Purchase Story and Access Chapter 2+
**Step 1: Purchase the story**
```http
POST http://localhost:8084/api/payment/purchase-story
Content-Type: application/json
X-User-Id: 1

{
  "storyId": 1,
  "price": 50000
}
```

**Step 2: Try to access Chapter 2 again**
```http
GET http://localhost:8083/api/story/chapters/2
X-User-Id: 1
```

**Expected Response (Success):**
```json
{
  "id": 2,
  "storyId": 1,
  "chapterNumber": 2,
  "title": "Chapter 2",
  "imageIds": ["001.png", "002.png", "003.png"]
}
```

**Verify:** Sau khi mua truyện, user có thể đọc tất cả các chapter.

---

#### 7.5 Test Free Story - All Chapters Accessible
Create a free story first:
```http
POST http://localhost:8083/api/story
Content-Type: application/json
X-User-Id: 1

{
  "title": "Free Story",
  "description": "This is a free story",
  "author": "Author Name",
  "genres": "Action",
  "isPaid": false,
  "price": 0
}
```

Then access any chapter without purchase:
```http
GET http://localhost:8083/api/story/chapters/{chapterId}
```

**Expected:** All chapters accessible without purchase for free stories.

---

### Scenario 8: Test Race Condition Protection

To test the SELECT FOR UPDATE lock, you need to simulate concurrent requests:

#### 8.1 Create Two Users with Same Balance
```sql
-- Run in PostgreSQL userdb
INSERT INTO users (username, email, password, balance, role_id, created_at, updated_at) 
VALUES ('user1', 'user1@test.com', 'pass', 100000, 1, NOW(), NOW());

INSERT INTO users (username, email, password, balance, role_id, created_at, updated_at) 
VALUES ('user2', 'user2@test.com', 'pass', 100000, 1, NOW(), NOW());
```

#### 8.2 Make Concurrent Purchase Requests
Open two Postman tabs and execute simultaneously:

**Tab 1:**
```http
POST http://localhost:8084/api/payment/purchase-story
Content-Type: application/json
X-User-Id: 2

{
  "storyId": 1,
  "price": 60000
}
```

**Tab 2 (execute immediately after Tab 1):**
```http
POST http://localhost:8084/api/payment/purchase-story
Content-Type: application/json
X-User-Id: 2

{
  "storyId": 3,
  "price": 60000
}
```

**Expected Behavior:**
- First request succeeds, balance becomes 40000
- Second request succeeds, balance becomes -20000 (WITHOUT lock) ❌
- Second request FAILS with insufficient balance (WITH lock) ✅

With proper SELECT FOR UPDATE implementation, the second request should wait for the first to complete, then check the updated balance and fail.

---

## Database Verification Queries

### Check Payment Records
```sql
-- Run in PostgreSQL paymentdb
SELECT 
    id, 
    user_id, 
    story_id, 
    transaction_id, 
    amount, 
    status, 
    payment_type,
    description,
    created_at 
FROM payments 
ORDER BY created_at DESC;
```

### Check User Balance
```sql
-- Run in PostgreSQL userdb
SELECT id, username, email, balance FROM users;
```

### Check Purchase Access
```sql
-- Run in PostgreSQL storydb
SELECT 
    p.id,
    p.user_id,
    p.story_id,
    s.title,
    s.price,
    p.purchased_at
FROM purchases p
JOIN stories s ON p.story_id = s.id
ORDER BY p.purchased_at DESC;
```

---

## Testing RabbitMQ Integration

### 1. Check RabbitMQ Management Console
Open: http://localhost:15672
- Username: myadmin
- Password: mypassword

### 2. Verify Exchanges
- Look for: `story.purchase.exchange`

### 3. Verify Queues
- Look for: `story.purchase.queue`
- Check if messages are being consumed

### 4. Monitor Message Flow
After making a purchase request, check:
1. Queue receives message
2. Story-service consumes message
3. Purchase record is created

---

## Troubleshooting

### Issue: "Insufficient balance" but balance is sufficient
**Solution:** Check if balance is BigDecimal in database and request

### Issue: Payment succeeds but no purchase record in story-service
**Solutions:**
1. Check RabbitMQ is running: `netstat -an | findstr 5672`
2. Check story-service logs for listener errors
3. Verify queue bindings in RabbitMQ console
4. Check if story-service is running

### Issue: Race condition - negative balance
**Solution:** Verify UserRepository.findByIdWithLock() is using @Lock(LockModeType.PESSIMISTIC_WRITE)

### Issue: "User not found" when deducting balance
**Solution:** Ensure X-User-Id header is set correctly

### Issue: Connection refused to user-service
**Solution:** 
1. Check user-service is running on port 8081
2. Verify user-service.url in payment-service application.properties

---

## Complete Test Flow Summary

```
1. Register user → User ID: 1
2. Add balance (500000) → Balance: 500000
3. Create paid story (price: 50000) → Story ID: 1
4. Purchase story → Payment SUCCESS
5. Verify balance deducted → Balance: 450000
6. Verify purchase access → Purchase record exists
7. Try purchasing same story → Success but no duplicate purchase
8. Create expensive story (price: 999999999) → Story ID: 2
9. Try purchasing expensive story → Payment FAILED (insufficient balance)
10. View payment history → 2 payments (1 SUCCESS, 1 FAILED)
```

---

## Notes

- All monetary amounts are in BigDecimal (e.g., 50000 = 50,000 VND)
- Transaction IDs are auto-generated with prefix "PURCHASE-"
- SELECT FOR UPDATE prevents race conditions in concurrent purchases
- RabbitMQ ensures eventual consistency between payment and story access
- Unique constraint on (user_id, story_id) in purchases table prevents duplicates
