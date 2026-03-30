# Story Purchase Payment System - Implementation Summary

## Overview
This system implements a complete story purchase payment flow with:
- Balance checking and deduction with database locking (SELECT FOR UPDATE)
- RabbitMQ message queue for event-driven architecture
- Transaction management to prevent negative balance
- Payment record tracking

---

## Architecture Components

### Services Involved

1. **Payment-Service (Port 8084)**
   - Handles purchase requests
   - Coordinates with user-service for balance operations
   - Creates payment records
   - Publishes purchase events to RabbitMQ

2. **User-Service (Port 8081)**
   - Manages user balance
   - Implements pessimistic locking (SELECT FOR UPDATE) to prevent race conditions
   - Validates and deducts balance

3. **Story-Service (Port 8083)**
   - Listens to purchase events from RabbitMQ
   - Grants access by creating purchase records
   - Prevents duplicate purchases with unique constraint

---

## Database Schema Changes

### Payment-Service (paymentdb)

**Updated `payments` table:**
```sql
ALTER TABLE payments 
ADD COLUMN story_id BIGINT;
```

Fields in Payment entity:
- `id` - Primary key
- `user_id` - User making the purchase
- `story_id` - Story being purchased (new field)
- `transaction_id` - Unique transaction identifier
- `amount` - Purchase amount
- `status` - PENDING, SUCCESS, FAILED, CANCELLED
- `payment_type` - DEPOSIT or PURCHASE
- `description` - Transaction description
- `created_at`, `updated_at` - Timestamps

### User-Service (userdb)

**No schema changes needed** - balance field already exists

The UserRepository now includes:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
Optional<UserEntity> findByIdWithLock(@Param("userId") Long userId);
```

This ensures database-level row locking during balance operations.

### Story-Service (storydb)

**No schema changes needed** - purchases table already exists

Existing unique constraint ensures no duplicate purchases:
```sql
UNIQUE (user_id, story_id)
```

---

## New Files Created

### Payment-Service
1. **`PurchaseStoryRequest.java`** - DTO for purchase requests
2. **`StoryPurchaseEvent.java`** - Event published to RabbitMQ
3. **`BalanceCheckRequest.java`** - Request to check balance
4. **`DeductBalanceRequest.java`** - Request to deduct balance
5. **`UserServiceClient.java`** - HTTP client to communicate with user-service
6. **`StoryPurchaseService.java`** - Service handling purchase logic

### User-Service
1. **`DeductBalanceRequest.java`** - DTO for balance deduction
2. **`BalanceService.java`** - Service with SELECT FOR UPDATE logic
3. **`BalanceController.java`** - REST endpoint for balance operations

### Story-Service
1. **`RabbitMQConfig.java`** - RabbitMQ queue/exchange configuration
2. **`StoryPurchaseEvent.java`** - Event received from RabbitMQ
3. **`StoryPurchaseListener.java`** - Listener consuming purchase events

---

## Updated Files

### Payment-Service
- `Payment.java` - Added storyId field
- `RabbitMQConfig.java` - Added story purchase queue configuration
- `PaymentController.java` - Added /purchase-story endpoint
- `application.properties` - Added user-service.url configuration

### User-Service
- `UserRepository.java` - Added findByIdWithLock() method with pessimistic locking

### Story-Service
- `PurchaseService.java` - Added grantAccess() method
- `IPurchaseService.java` - Added grantAccess() to interface
- `pom.xml` - Added spring-boot-starter-amqp dependency
- `application.properties` - Added RabbitMQ configuration

---

## API Endpoints

### Payment-Service

#### Purchase Story
```http
POST /api/payment/purchase-story
Headers:
  Content-Type: application/json
  X-User-Id: {userId}
Body:
{
  "storyId": 1,
  "price": 50000
}
```

**Success Response (200):**
```json
{
  "id": 1,
  "userId": 1,
  "storyId": 1,
  "transactionId": "PURCHASE-xxx",
  "amount": 50000,
  "status": "SUCCESS",
  "paymentType": "PURCHASE"
}
```

**Error Response (400):**
```json
{
  "error": "Story purchase failed: Insufficient balance. Current: 10000, Required: 50000"
}
```

### User-Service

#### Deduct Balance (Internal API)
```http
POST /api/users/balance/deduct
Content-Type: application/json
Body:
{
  "userId": 1,
  "amount": 50000,
  "transactionId": "PURCHASE-xxx"
}
```

---

## Purchase Flow Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 1. POST /purchase-story
       ▼
┌─────────────────────┐
│  Payment-Service    │
│  (Port 8084)        │
└──────┬──────────────┘
       │ 2. POST /balance/deduct
       │    (with transaction lock)
       ▼
┌─────────────────────┐
│   User-Service      │◄───── SELECT FOR UPDATE
│   (Port 8081)       │       prevents concurrent
└──────┬──────────────┘       balance issues
       │ 3. Balance OK
       │    Amount deducted
       ▼
┌─────────────────────┐
│  Payment-Service    │
│  Creates Payment    │
│  Record (SUCCESS)   │
└──────┬──────────────┘
       │ 4. Publish Event
       │    to RabbitMQ
       ▼
┌─────────────────────┐
│     RabbitMQ        │
│  story.purchase     │
│      .queue         │
└──────┬──────────────┘
       │ 5. Consume Event
       ▼
┌─────────────────────┐
│   Story-Service     │
│   (Port 8083)       │
│  Creates Purchase   │
│  Record (Access)    │
└─────────────────────┘
```

---

## Key Features Implemented

### 1. **SELECT FOR UPDATE - Race Condition Prevention**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
Optional<UserEntity> findByIdWithLock(@Param("userId") Long userId);
```

This prevents scenarios like:
- User has balance: 100,000
- Two concurrent purchase requests for 60,000 each
- WITHOUT lock: Both succeed, balance becomes -20,000 ❌
- WITH lock: First succeeds, second fails with insufficient balance ✅

### 2. **Transaction Management**

```java
@Transactional
public void deductBalance(Long userId, BigDecimal amount, String transactionId) {
    // Lock user row
    UserEntity user = userRepository.findByIdWithLock(userId);
    
    // Check balance
    if (currentBalance.compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient balance");
    }
    
    // Deduct
    user.setBalance(currentBalance.subtract(amount));
    userRepository.save(user);
}
```

### 3. **Event-Driven Architecture**

Payment-service publishes events to RabbitMQ, ensuring:
- Loose coupling between services
- Retry capability if story-service is down
- Eventual consistency
- Asynchronous processing

### 4. **Payment Status Tracking**

All payment attempts are recorded with status:
- `PENDING` - Initial state
- `SUCCESS` - Balance deducted and event published
- `FAILED` - Insufficient balance or other error

### 5. **Duplicate Purchase Prevention**

Story-service has unique constraint:
```sql
UNIQUE (user_id, story_id)
```

If user tries to purchase same story twice, second purchase event is ignored.

---

## Configuration Requirements

### RabbitMQ Configuration
```properties
# All services need these settings
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=myadmin
spring.rabbitmq.password=mypassword
spring.rabbitmq.virtual-host=/
```

### Service URLs
```properties
# Payment-service needs
user-service.url=http://localhost:8081
```

---

## Testing Checklist

- [ ] RabbitMQ is running on port 5672
- [ ] All databases are created (paymentdb, userdb, storydb)
- [ ] All services are running (8081, 8083, 8084)
- [ ] User has sufficient balance
- [ ] Story exists and is marked as paid (isPaid=true)
- [ ] Test successful purchase
- [ ] Verify balance deducted in userdb
- [ ] Verify payment record in paymentdb
- [ ] Verify purchase record in storydb
- [ ] Test insufficient balance scenario
- [ ] Test concurrent purchases (race condition)
- [ ] Check RabbitMQ console for message flow

---

## Building and Running

### 1. Build All Services
```bash
# Payment-service
cd D:\Microservices\story-reading\payment-service
mvn clean install -DskipTests

# User-service
cd D:\Microservices\story-reading\user-service
mvn clean install -DskipTests

# Story-service
cd D:\Microservices\story-reading\story-service
mvn clean install -DskipTests
```

### 2. Start RabbitMQ
- Ensure RabbitMQ is running on port 5672
- Access management console: http://localhost:15672

### 3. Run Services
```bash
# User-service
cd D:\Microservices\story-reading\user-service
mvn spring-boot:run

# Story-service (new terminal)
cd D:\Microservices\story-reading\story-service
mvn spring-boot:run

# Payment-service (new terminal)
cd D:\Microservices\story-reading\payment-service
mvn spring-boot:run
```

---

## Monitoring

### Check RabbitMQ
- URL: http://localhost:15672
- Username: myadmin
- Password: mypassword
- Look for: `story.purchase.exchange` and `story.purchase.queue`

### Check Logs
Watch for:
- Payment-service: "Publishing story purchase event to RabbitMQ"
- Story-service: "Received story purchase event"
- Story-service: "Successfully granted access"

### Database Queries
```sql
-- Check payment records
SELECT * FROM payments WHERE payment_type = 'PURCHASE' ORDER BY created_at DESC;

-- Check user balance
SELECT id, username, balance FROM users;

-- Check purchase access
SELECT * FROM purchases ORDER BY purchased_at DESC;
```

---

## Error Handling

### Insufficient Balance
- Payment status set to FAILED
- Balance NOT deducted
- No event published to RabbitMQ
- User receives error message

### User Not Found
- Payment fails immediately
- Error: "User not found with id: X"

### Story Not Found (in story-service listener)
- Event is logged as error
- Purchase record NOT created
- Consider implementing dead letter queue for retry

### RabbitMQ Down
- Payment succeeds
- Event publish fails
- Payment record shows SUCCESS but no purchase access
- Manual intervention may be needed

---

## Future Enhancements

1. **Dead Letter Queue**: Handle failed purchase grant attempts
2. **Refund Mechanism**: If story-service fails, refund user balance
3. **Idempotency**: Track transaction IDs to prevent duplicate processing
4. **Saga Pattern**: Implement compensating transactions
5. **Webhook**: Notify user when purchase is complete
6. **Admin Dashboard**: View all transactions and failed purchases

---

## Support

For detailed testing instructions, see: `STORY_PURCHASE_TEST_GUIDE.md`

For issues:
1. Check service logs
2. Verify RabbitMQ connections
3. Check database constraints
4. Verify service-to-service communication

