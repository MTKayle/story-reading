# Hướng dẫn Test Story Service với Postman

## Thiết lập ban đầu

### Environment Variables (Postman)
Tạo environment mới trong Postman với các biến sau:
- `base_url`: `http://localhost:8083`
- `gateway_url`: `http://localhost:8081` (nếu test qua API Gateway)
- `user_id`: `123` (hoặc user ID thực tế)
- `story_id`: (sẽ được set tự động từ response)
- `chapter_id`: (sẽ được set tự động từ response)

---

## LUỒNG 1: Tạo truyện từng bước (Phương pháp thủ công)

### Bước 1: Tạo truyện mới (metadata only)

**Method:** `POST`  
**URL:** `{{base_url}}/api/story`

**Headers:**
```
Content-Type: application/json
X-User-Id: {{user_id}}
```

**Body (raw JSON):**
```json
{
  "title": "Truyện Của Tôi",
  "description": "Đây là mô tả truyện",
  "genres": ["fantasy", "adventure"],
  "paid": false,
  "price": 0
}
```

**Expected Response:** `200 OK`
```json
{
  "id": 1,
  "title": "Truyện Của Tôi",
  "description": "Đây là mô tả truyện",
  "genres": ["fantasy", "adventure"],
  "coverImageId": null,
  "paid": false,
  "price": 0,
  "author": "123"
}
```

**Postman Script (Tests tab):**
```javascript
// Lưu story_id vào environment
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("story_id", jsonData.id);
    console.log("Story ID saved:", jsonData.id);
}
```

---

### Bước 2: Upload ảnh cover cho truyện

**Method:** `POST`  
**URL:** `{{base_url}}/api/story/{{story_id}}/cover`

**Headers:**
```
(Không set Content-Type, để Postman tự động set multipart/form-data)
```

**Body (form-data):**
- Key: `file` (type: File)
- Value: Chọn file ảnh cover (jpg, png, etc.)

**Expected Response:** `200 OK`
```json
"/public/images/truyen-cua-toi/cover.jpg"
```

**Kiểm tra:**
- Mở browser: `http://localhost:8083/public/images/truyen-cua-toi/cover.jpg`
- Hoặc qua Gateway: `http://localhost:8081/public/images/truyen-cua-toi/cover.jpg`

---

### Bước 3: Tạo chương đầu tiên (không có ảnh)

**Method:** `POST`  
**URL:** `{{base_url}}/api/story/{{story_id}}/chapters`

**Headers:**
```
Content-Type: application/json
X-User-Id: {{user_id}}
```

**Body (raw JSON):**
```json
{
  "chapterNumber": 1,
  "title": "Chương 1: Khởi đầu",
  "imageIds": []
}
```

**Expected Response:** `200 OK`
```json
{
  "id": 1,
  "storyId": 1,
  "chapterNumber": 1,
  "title": "Chương 1: Khởi đầu",
  "imageIds": []
}
```

**Postman Script (Tests tab):**
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("chapter_id", jsonData.id);
    console.log("Chapter ID saved:", jsonData.id);
}
```

---

### Bước 4: Upload ảnh cho chương

**Method:** `POST`  
**URL:** `{{base_url}}/api/story/{{story_id}}/chapters/1/images`

**Headers:**
```
(Không set Content-Type)
```

**Body (form-data):**
- Key: `files` (type: File, cho phép multiple files)
- Value: Chọn nhiều file ảnh (001.jpg, 002.jpg, 003.jpg, ...)

**Expected Response:** `200 OK`
```json
[
  "/public/images/truyen-cua-toi/1/001.jpg",
  "/public/images/truyen-cua-toi/1/002.jpg",
  "/public/images/truyen-cua-toi/1/003.jpg"
]
```

**Kiểm tra:**
- `http://localhost:8083/public/images/truyen-cua-toi/1/001.jpg`
- `http://localhost:8083/public/images/truyen-cua-toi/1/002.jpg`

---

### Bước 5: Tạo chương 2 với ảnh

**5.1) Tạo chương:**

**Method:** `POST`  
**URL:** `{{base_url}}/api/story/{{story_id}}/chapters`

**Body:**
```json
{
  "chapterNumber": 2,
  "title": "Chương 2: Phát triển",
  "imageIds": []
}
```

**5.2) Upload ảnh cho chương 2:**

**Method:** `POST`  
**URL:** `{{base_url}}/api/story/{{story_id}}/chapters/2/images`

**Body (form-data):**
- Key: `files`
- Value: Chọn file ảnh cho chapter 2

---

### Bước 6: Xem danh sách các chương

**Method:** `GET`  
**URL:** `{{base_url}}/api/story/{{story_id}}/chapters`

**Headers:** (không cần)

**Expected Response:** `200 OK`
```json
[
  {
    "id": 1,
    "storyId": 1,
    "chapterNumber": 1,
    "title": "Chương 1: Khởi đầu",
    "imageIds": [
      "/public/images/truyen-cua-toi/1/001.jpg",
      "/public/images/truyen-cua-toi/1/002.jpg"
    ]
  },
  {
    "id": 2,
    "storyId": 1,
    "chapterNumber": 2,
    "title": "Chương 2: Phát triển",
    "imageIds": [...]
  }
]
```

---

### Bước 7: Xem chi tiết một chương

**Method:** `GET`  
**URL:** `{{base_url}}/api/story/chapters/{{chapter_id}}`

**Headers:**
```
X-User-Id: {{user_id}}
```

**Expected Response:** `200 OK`
```json
{
  "id": 1,
  "storyId": 1,
  "chapterNumber": 1,
  "title": "Chương 1: Khởi đầu",
  "imageIds": [...]
}
```

---

## LUỒNG 2: Tạo truyện nhanh (All-in-one với /full endpoint)

### Tạo truyện + cover + chương đầu tiên trong 1 request

**Method:** `POST`  
**URL:** `{{base_url}}/api/story/full`

**Headers:**
```
X-User-Id: {{user_id}}    # IMPORTANT: /full requires the X-User-Id header so the created story has an author
(Không set Content-Type - Postman tự động)
```

**Body (form-data):**

1. **meta** (type: Text, Content-Type: application/json)
```json
{
  "title": "Truyện Mới Hoàn Chỉnh",
  "description": "Mô tả đầy đủ",
  "genres": ["action", "romance"],
  "paid": false,
  "price": 0
}
```

2. **cover** (type: File)
   - Chọn file ảnh cover

3. **chapterFiles** (type: File, multiple files enabled)
   - Chọn nhiều file ảnh cho chapter đầu tiên

4. **chapterNumber** (type: Text)
   - Value: `1`

**Expected Response:** `200 OK`
```json
{
  "id": 2,
  "title": "Truyện Mới Hoàn Chỉnh",
  "description": "Mô tả đầy đủ",
  "genres": ["action", "romance"],
  "coverImageId": "/public/images/truyen-moi-hoan-chinh/cover.jpg",
  "paid": false,
  "price": 0,
  "author": "123"
}
```

**Lưu ý:**
- Endpoint `/full` tự động tạo truyện, upload cover, upload ảnh chapter và tạo chapter 1
- Chương được tạo tự động với title "Chapter 1"

---

## LUỒNG 3: Xem thông tin truyện

### Xem chi tiết truyện

**Method:** `GET`  
**URL:** `{{base_url}}/api/story/{{story_id}}`

**Expected Response:** `200 OK`

### Xem danh sách tất cả truyện

**Method:** `GET`  
**URL:** `{{base_url}}/api/story`

**Expected Response:** `200 OK` (Array of stories)

---

## Kiểm tra cấu trúc thư mục

Sau khi test, kiểm tra thư mục `story-service/public/images/`:

```
public/
└── images/
    └── truyen-cua-toi/           # slug của story
        ├── cover.jpg             # ảnh cover
        ├── 1/                    # chapter 1
        │   ├── 001.jpg
        │   ├── 002.jpg
        │   └── 003.jpg
        └── 2/                    # chapter 2
            ├── 001.jpg
            └── 002.jpg
```

---

## Các lỗi thường gặp và cách khắc phục

### 1. Lỗi: "Required header 'X-User-Id' is not present"
**Nguyên nhân:** Thiếu header `X-User-Id`  
**Cách sửa:** Thêm header `X-User-Id: 123` vào request

### 2. Lỗi: "null value in column 'author' violates not-null constraint"
**Nguyên nhân:** Service không set author từ userId  
**Cách sửa:** Đã được fix trong code, đảm bảo truyền header `X-User-Id`

### 3. Lỗi 404 khi truy cập ảnh `/public/images/...`
**Nguyên nhân:** 
- Thư mục `public/` chưa tồn tại
- API Gateway chưa route `/public/**` đến story-service

**Cách sửa:**
- Tạo thư mục: `mkdir public\images` trong thư mục story-service
- Kiểm tra `api-gateway/application.yml` có route `/public/**` đến story-service

### 4. Lỗi upload file "Content type 'multipart/form-data' not supported"
**Nguyên nhân:** Đặt Content-Type thủ công  
**Cách sửa:** XÓA header Content-Type, để Postman tự động set với boundary

---

## Tips cho Postman

### 1. Tạo Collection cho Story Service
- New Collection → "Story Service Tests"
- Thêm tất cả các request trên vào collection
- Sắp xếp theo thứ tự: Create Story → Upload Cover → Create Chapter → Upload Images

### 2. Sử dụng Pre-request Scripts
```javascript
// Auto-generate random title
pm.environment.set("random_title", "Story " + Date.now());
```

### 3. Chain requests với Tests
```javascript
// Trong Create Story request
pm.environment.set("story_id", pm.response.json().id);

// Request tiếp theo dùng {{story_id}}
```

### 4. Test qua API Gateway
Thay `{{base_url}}` = `http://localhost:8081` để test routing

---

## Ví dụ cURL (Backup)

### Tạo truyện:
```bash
curl -X POST http://localhost:8083/api/story \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 123" \
  -d '{"title":"Test Story","description":"Desc","genres":["fantasy"],"paid":false,"price":0}'
```

### Upload cover:
```bash
curl -X POST http://localhost:8083/api/story/1/cover \
  -F "file=@cover.jpg"
```

### Tạo chapter:
```bash
curl -X POST http://localhost:8083/api/story/1/chapters \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 123" \
  -d '{"chapterNumber":1,"title":"Chapter 1","imageIds":[]}'
```

### Upload ảnh chapter:
```bash
curl -X POST http://localhost:8083/api/story/1/chapters/1/images \
  -F "files=@page1.jpg" \
  -F "files=@page2.jpg" \
  -F "files=@page3.jpg"
```

---

## Checklist hoàn chỉnh

- [ ] Service story-service đang chạy trên port 8083
- [ ] Database PostgreSQL đã khởi động
- [ ] Thư mục `public/images/` đã được tạo
- [ ] API Gateway (nếu dùng) đang chạy và route đúng
- [ ] Environment variables đã được set trong Postman
- [ ] Test tạo truyện thành công
- [ ] Test upload cover thành công
- [ ] Test tạo chapter thành công
- [ ] Test upload ảnh chapter thành công
- [ ] Truy cập ảnh qua browser thành công
- [ ] Test endpoint `/full` thành công

---

**Tác giả:** Story Service Team  
**Ngày cập nhật:** 7 tháng 11, 2025  
**Phiên bản:** 1.0
