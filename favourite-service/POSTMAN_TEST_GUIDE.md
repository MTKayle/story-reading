# FAVOURITE SERVICE - POSTMAN TEST GUIDE

## Cấu hình Database

Tạo database mới cho favourite-service:

```sql
CREATE DATABASE favourite_db;
```

## Khởi động Service

Port: 8085
Database: favourite_db (PostgreSQL)

## API Endpoints

### 1. BOOKMARK APIs (Đánh dấu vị trí đọc)

#### 1.1. Tạo hoặc cập nhật bookmark (Đánh dấu đã đọc đến chương nào)

**POST** `http://localhost:8085/api/bookmarks`

**Headers:**
- `X-User-Id: 1`
- `Content-Type: application/json`

**Body:**
```json
{
  "storyId": 10,
  "chapterId": 5,
  "chapterNumber": 1
}
```

**Response Success (200):**
```json
{
  "id": 1,
  "userId": 1,
  "storyId": 10,
  "chapterId": 5,
  "chapterNumber": 1,
  "lastReadAt": "2025-11-14T10:30:00",
  "createdAt": "2025-11-14T10:30:00",
  "updatedAt": "2025-11-14T10:30:00"
}
```

**Giải thích:**
- API này vừa tạo mới vừa cập nhật bookmark
- Nếu user đã có bookmark cho truyện này, sẽ cập nhật chapterId và chapterNumber mới
- `lastReadAt` được tự động cập nhật mỗi khi gọi API

---

#### 1.2. Lấy bookmark của user cho một truyện cụ thể

**GET** `http://localhost:8085/api/bookmarks/story/10`

**Headers:**
- `X-User-Id: 1`

**Response Success (200):**
```json
{
  "id": 1,
  "userId": 1,
  "storyId": 10,
  "chapterId": 5,
  "chapterNumber": 1,
  "lastReadAt": "2025-11-14T10:30:00",
  "createdAt": "2025-11-14T10:30:00",
  "updatedAt": "2025-11-14T10:30:00"
}
```

**Response Not Found (404):** Nếu user chưa đánh dấu truyện này

---

#### 1.3. Lấy tất cả bookmark của user (Lịch sử đọc)

**GET** `http://localhost:8085/api/bookmarks`

**Headers:**
- `X-User-Id: 1`

**Response Success (200):**
```json
[
  {
    "id": 1,
    "userId": 1,
    "storyId": 10,
    "chapterId": 5,
    "chapterNumber": 1,
    "lastReadAt": "2025-11-14T10:30:00",
    "createdAt": "2025-11-14T10:30:00",
    "updatedAt": "2025-11-14T10:30:00"
  },
  {
    "id": 2,
    "userId": 1,
    "storyId": 9,
    "chapterId": 3,
    "chapterNumber": 2,
    "lastReadAt": "2025-11-14T09:00:00",
    "createdAt": "2025-11-14T08:00:00",
    "updatedAt": "2025-11-14T09:00:00"
  }
]
```

**Giải thích:** Danh sách được sắp xếp theo `lastReadAt` giảm dần (truyện đọc gần nhất ở đầu)

---

#### 1.4. Xóa bookmark

**DELETE** `http://localhost:8085/api/bookmarks/story/10`

**Headers:**
- `X-User-Id: 1`

**Response Success (204 No Content)**

---

### 2. FOLLOW APIs (Theo dõi truyện)

#### 2.1. Theo dõi một truyện

**POST** `http://localhost:8085/api/follows`

**Headers:**
- `X-User-Id: 1`
- `Content-Type: application/json`

**Body:**
```json
{
  "storyId": 10
}
```

**Response Success (200):**
```json
{
  "id": 1,
  "userId": 1,
  "storyId": 10,
  "createdAt": "2025-11-14T10:30:00"
}
```

**Response Error (400):** Nếu đã follow truyện này rồi
```json
{
  "timestamp": "2025-11-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Already following this story"
}
```

---

#### 2.2. Bỏ theo dõi một truyện

**DELETE** `http://localhost:8085/api/follows/story/10`

**Headers:**
- `X-User-Id: 1`

**Response Success (204 No Content)**

**Response Error (400):** Nếu chưa follow truyện này
```json
{
  "timestamp": "2025-11-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Not following this story"
}
```

---

#### 2.3. Lấy danh sách truyện user đang theo dõi

**GET** `http://localhost:8085/api/follows`

**Headers:**
- `X-User-Id: 1`

**Response Success (200):**
```json
[
  {
    "id": 1,
    "userId": 1,
    "storyId": 10,
    "createdAt": "2025-11-14T10:30:00"
  },
  {
    "id": 2,
    "userId": 1,
    "storyId": 9,
    "createdAt": "2025-11-14T09:00:00"
  }
]
```

**Giải thích:** Danh sách được sắp xếp theo `createdAt` giảm dần (follow gần nhất ở đầu)

---

#### 2.4. Kiểm tra user có đang theo dõi truyện không

**GET** `http://localhost:8085/api/follows/story/10/check`

**Headers:**
- `X-User-Id: 1`

**Response Success (200):**
```json
true
```
hoặc
```json
false
```

---

#### 2.5. Lấy trạng thái follow và số người theo dõi

**GET** `http://localhost:8085/api/follows/story/10/status`

**Headers:**
- `X-User-Id: 1`

**Response Success (200):**
```json
{
  "isFollowing": true,
  "followerCount": 25
}
```

**Giải thích:**
- `isFollowing`: User hiện tại có đang follow truyện này không
- `followerCount`: Tổng số người đang follow truyện này

---

#### 2.6. Lấy số lượng người theo dõi (Public API)

**GET** `http://localhost:8085/api/follows/story/10/count`

**Headers:** Không cần X-User-Id

**Response Success (200):**
```json
25
```

---

## TEST SCENARIO - Luồng đọc truyện thực tế

### Bước 1: User bắt đầu đọc truyện mới
1. Theo dõi truyện:
   ```
   POST http://localhost:8085/api/follows
   Body: {"storyId": 10}
   ```

2. Đọc chương 1 và đánh dấu:
   ```
   POST http://localhost:8085/api/bookmarks
   Body: {"storyId": 10, "chapterId": 5, "chapterNumber": 1}
   ```

### Bước 2: User tiếp tục đọc
1. Đọc chương 2 và cập nhật bookmark:
   ```
   POST http://localhost:8085/api/bookmarks
   Body: {"storyId": 10, "chapterId": 6, "chapterNumber": 2}
   ```
   (API sẽ tự động cập nhật bookmark cũ)

### Bước 3: Kiểm tra lịch sử đọc
1. Xem danh sách truyện đang đọc:
   ```
   GET http://localhost:8085/api/bookmarks
   ```

2. Xem danh sách truyện đang theo dõi:
   ```
   GET http://localhost:8085/api/follows
   ```

### Bước 4: Kiểm tra tiến độ đọc của một truyện
```
GET http://localhost:8085/api/bookmarks/story/10
```
→ Trả về chương cuối cùng user đã đọc

### Bước 5: Bỏ theo dõi truyện
```
DELETE http://localhost:8085/api/follows/story/10
```

---

## TÍCH HỢP VỚI STORY-SERVICE

Khi user đọc một chương, frontend nên gọi cả 2 APIs:

1. **Story Service** - Lấy nội dung chương:
   ```
   GET http://localhost:8081/api/story/chapters/{chapterId}
   ```

2. **Favourite Service** - Lưu tiến độ đọc:
   ```
   POST http://localhost:8085/api/bookmarks
   Body: {"storyId": X, "chapterId": Y, "chapterNumber": Z}
   ```

Khi hiển thị danh sách truyện, có thể:
- Gọi Bookmark API để lấy vị trí đọc hiện tại
- Gọi Follow API để hiển thị số lượng người theo dõi
- Hiển thị nút "Tiếp tục đọc" dựa vào bookmark

---

## DATABASE STRUCTURE

### Table: bookmarks
```sql
CREATE TABLE bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    chapter_number INTEGER NOT NULL,
    last_read_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, story_id)
);
```

### Table: follows
```sql
CREATE TABLE follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, story_id)
);
```

---

## LƯU Ý

1. **X-User-Id Header**: Tất cả các API đều yêu cầu header này (trừ API public lấy follower count)

2. **Bookmark tự động cập nhật**: Khi gọi POST bookmark với cùng userId và storyId, hệ thống sẽ tự động cập nhật chapterId và chapterNumber mới

3. **Unique constraint**: 
   - Một user chỉ có một bookmark cho mỗi truyện
   - Một user chỉ có thể follow một truyện một lần

4. **Soft delete**: Hiện tại các API DELETE sẽ xóa hẳn dữ liệu khỏi database

5. **Sorting**:
   - Bookmarks: Sắp xếp theo lastReadAt DESC (đọc gần nhất trước)
   - Follows: Sắp xếp theo createdAt DESC (follow gần nhất trước)

