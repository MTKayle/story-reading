# HƯỚNG DẪN ĐĂNG NHẬP VỚI GOOGLE - CHI TIẾT

## 📋 Tổng quan

Chức năng đăng nhập với Google OAuth2 cho phép user đăng nhập bằng tài khoản Google của họ mà không cần tạo username/password riêng.

## 🔧 Phần 1: Cấu hình Google Cloud Console

### Bước 1: Tạo Google OAuth2 Credentials

1. **Truy cập Google Cloud Console:**
   - Mở https://console.cloud.google.com/
   - Đăng nhập bằng tài khoản Google của bạn

2. **Tạo Project mới (hoặc chọn project có sẵn):**
   - Click vào dropdown "Select a project" ở góc trên bên trái
   - Click "NEW PROJECT"
   - Nhập tên project: `story-reading-app`
   - Click "CREATE"

3. **Enable Google+ API:**
   - Vào menu "APIs & Services" > "Library"
   - Tìm "Google+ API"
   - Click "ENABLE"

4. **Tạo OAuth2 Credentials:**
   - Vào "APIs & Services" > "Credentials"
   - Click "CREATE CREDENTIALS" > "OAuth client ID"
   - Nếu chưa có OAuth consent screen, click "CONFIGURE CONSENT SCREEN":
     - Chọn "External" > "CREATE"
     - Nhập App name: `Story Reading App`
     - User support email: email của bạn
     - Developer contact email: email của bạn
     - Click "SAVE AND CONTINUE"
     - Phần Scopes: Click "ADD OR REMOVE SCOPES"
       - Chọn: `email`, `profile`, `openid`
       - Click "UPDATE" > "SAVE AND CONTINUE"
     - Test users: Thêm email của bạn để test
     - Click "SAVE AND CONTINUE" > "BACK TO DASHBOARD"
   
5. **Tạo OAuth Client ID:**
   - Quay lại "Credentials" > "CREATE CREDENTIALS" > "OAuth client ID"
   - Application type: **Web application**
   - Name: `Story Reading Web Client`
   - Authorized JavaScript origins:
     - `http://localhost:3000` (Frontend)
     - `http://localhost:8080` (API Gateway)
   - Authorized redirect URIs:
     - `http://localhost:3000/auth/google/callback`
     - `http://localhost:8080/auth/google/callback`
   - Click "CREATE"

6. **Lưu Client ID:**
   - Sau khi tạo xong, bạn sẽ thấy popup hiển thị:
     - **Client ID**: Dạng `123456789-abcdefg.apps.googleusercontent.com`
     - **Client Secret**: (không cần dùng cho frontend)
   - Copy **Client ID** này

### Bước 2: Cấu hình Backend

1. **Mở file `application.properties`:**
   ```
   D:\Microservices\story-reading\user-service\src\main\resources\application.properties
   ```

2. **Thay thế `YOUR_GOOGLE_CLIENT_ID_HERE` bằng Client ID vừa copy:**
   ```properties
   google.client.id=123456789-abcdefg.apps.googleusercontent.com
   ```

3. **Save file và restart user-service**

---

## 🧪 Phần 2: Test với Postman (Sử dụng Google OAuth Playground)

### Cách 1: Lấy Google ID Token từ OAuth 2.0 Playground

#### Bước 1: Truy cập Google OAuth 2.0 Playground

1. Mở: https://developers.google.com/oauthplayground/
2. Click biểu tượng ⚙️ (Settings) ở góc trên bên phải
3. Check ✅ "Use your own OAuth credentials"
4. Nhập:
   - **OAuth Client ID**: Client ID của bạn
   - **OAuth Client secret**: Client Secret của bạn (từ Google Cloud Console)
5. Click "Close"

#### Bước 2: Authorize APIs

1. Ở cột bên trái "Step 1: Select & authorize APIs":
   - Tìm **Google OAuth2 API v2**
   - Chọn:
     - ✅ `https://www.googleapis.com/auth/userinfo.email`
     - ✅ `https://www.googleapis.com/auth/userinfo.profile`
     - ✅ `openid`
2. Click "Authorize APIs"
3. Chọn tài khoản Google của bạn
4. Click "Allow" để cấp quyền

#### Bước 3: Exchange authorization code for tokens

1. Sau khi authorize, bạn sẽ thấy "Step 2: Exchange authorization code for tokens"
2. Click "Exchange authorization code for tokens"
3. Bạn sẽ nhận được response với:
   ```json
   {
     "access_token": "ya29.a0...",
     "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
     "expires_in": 3599,
     "token_type": "Bearer",
     "scope": "openid https://www.googleapis.com/auth/userinfo.email ...",
     "refresh_token": "1//0..."
   }
   ```
4. **Copy giá trị `id_token`** (đây là cái chúng ta cần!)

#### Bước 4: Test API với Postman

1. **Mở Postman**
2. **Tạo request mới:**
   - Method: `POST`
   - URL: `http://localhost:8080/api/auth/google`
   - Headers:
     ```
     Content-Type: application/json
     ```
   - Body (raw JSON):
     ```json
     {
       "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6... (paste id_token ở đây)"
     }
     ```

3. **Click Send**

4. **Expected Response (Success):**
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

5. **Copy accessToken** và sử dụng nó cho các API khác:
   ```
   Authorization: Bearer {accessToken}
   ```

---

### Cách 2: Lấy ID Token bằng Google Sign-In HTML (Đơn giản hơn)

#### Bước 1: Tạo file HTML test

Tạo file `google-signin-test.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Google Sign-In Test</title>
    <meta name="google-signin-client_id" content="YOUR_GOOGLE_CLIENT_ID_HERE">
    <script src="https://accounts.google.com/gsi/client" async defer></script>
</head>
<body>
    <h1>Google Sign-In Test</h1>
    
    <div id="g_id_onload"
         data-client_id="YOUR_GOOGLE_CLIENT_ID_HERE"
         data-callback="handleCredentialResponse">
    </div>
    <div class="g_id_signin" data-type="standard"></div>

    <h2>ID Token:</h2>
    <textarea id="idToken" rows="10" cols="100" readonly></textarea>

    <h2>Test API:</h2>
    <button onclick="testAPI()">Test Login API</button>
    <pre id="response"></pre>

    <script>
        function handleCredentialResponse(response) {
            console.log("Encoded JWT ID token: " + response.credential);
            document.getElementById('idToken').value = response.credential;
            
            // Tự động test API
            testAPI();
        }

        async function testAPI() {
            const idToken = document.getElementById('idToken').value;
            
            try {
                const response = await fetch('http://localhost:8080/api/auth/google', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ idToken: idToken })
                });

                const data = await response.json();
                document.getElementById('response').textContent = JSON.stringify(data, null, 2);
                
                // Lưu accessToken vào localStorage
                if (data.accessToken) {
                    localStorage.setItem('accessToken', data.accessToken);
                    alert('Login thành công! Access token đã được lưu.');
                }
            } catch (error) {
                document.getElementById('response').textContent = 'Error: ' + error.message;
            }
        }
    </script>
</body>
</html>
```

#### Bước 2: Sử dụng file HTML

1. **Thay thế `YOUR_GOOGLE_CLIENT_ID_HERE`** bằng Client ID của bạn (2 chỗ)
2. **Mở file HTML trong browser:**
   - Double-click file `google-signin-test.html`
   - Hoặc drag & drop vào Chrome/Edge
3. **Click nút "Sign in with Google"**
4. **Chọn tài khoản Google**
5. **ID Token sẽ tự động hiển thị** trong textarea
6. **Click "Test Login API"** để test luôn
7. **Kiểm tra response** - sẽ thấy accessToken và refreshToken

---

## 📝 Phần 3: Test Scenarios

### Test Case 1: Đăng nhập lần đầu với Google

**Request:**
```
POST http://localhost:8080/api/auth/google
Content-Type: application/json

{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

**Expected Result:**
- ✅ Tạo user mới trong database
- ✅ Username tự động generate từ email
- ✅ Avatar lấy từ Google
- ✅ Trả về accessToken và refreshToken

**Verify trong Database:**
```sql
SELECT id, username, email, google_id, avatar_url 
FROM users 
WHERE google_id IS NOT NULL;
```

### Test Case 2: Đăng nhập lần 2 với cùng Google account

**Request:** (Giống Test Case 1)

**Expected Result:**
- ✅ Không tạo user mới
- ✅ Login với user đã tồn tại
- ✅ Cập nhật avatar nếu có thay đổi
- ✅ Trả về accessToken mới

### Test Case 3: Link Google account với email đã tồn tại

**Setup:**
1. Đăng ký account thông thường:
```
POST http://localhost:8080/api/auth/register
{
  "username": "testuser",
  "email": "test@gmail.com",
  "password": "123456"
}
```

2. Đăng nhập với Google (sử dụng cùng email `test@gmail.com`):
```
POST http://localhost:8080/api/auth/google
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

**Expected Result:**
- ✅ Account được link với Google ID
- ✅ Có thể login bằng cả password và Google
- ✅ Avatar được cập nhật từ Google

### Test Case 4: Token không hợp lệ

**Request:**
```
POST http://localhost:8080/api/auth/google
{
  "idToken": "invalid_token_123"
}
```

**Expected Result:**
```
HTTP 400 Bad Request
```

---

## 🔍 Phần 4: Troubleshooting

### Lỗi 1: "Google token không hợp lệ"

**Nguyên nhân:**
- ID Token đã hết hạn (expires sau 1 giờ)
- Client ID không khớp
- Token không đúng định dạng

**Giải pháp:**
- Lấy ID Token mới từ OAuth Playground
- Kiểm tra Client ID trong `application.properties`
- Đảm bảo copy đúng toàn bộ token (rất dài)

### Lỗi 2: "Email chưa được xác thực bởi Google"

**Nguyên nhân:**
- Email Google chưa được verify

**Giải pháp:**
- Verify email trong tài khoản Google
- Hoặc sử dụng email Google khác đã verify

### Lỗi 3: CORS Error khi test từ HTML

**Giải pháp:**
- Thêm CORS configuration vào API Gateway hoặc user-service
- Hoặc test trực tiếp qua Postman (không bị CORS)

### Lỗi 4: "401 invalid_client" trong OAuth Playground

**Nguyên nhân:**
- Client Secret sai
- Client ID sai

**Giải pháp:**
- Copy lại Client ID và Client Secret từ Google Cloud Console
- Đảm bảo không có khoảng trắng thừa

---

## 🎯 Phần 5: Flow hoàn chỉnh

```
┌─────────┐          ┌──────────┐          ┌──────────────┐          ┌──────────┐
│ Browser │          │ Frontend │          │  User Service│          │  Google  │
└────┬────┘          └────┬─────┘          └──────┬───────┘          └────┬─────┘
     │                    │                        │                       │
     │  Click "Sign in    │                        │                       │
     │   with Google"     │                        │                       │
     ├───────────────────>│                        │                       │
     │                    │                        │                       │
     │                    │  Redirect to Google    │                       │
     │                    │  OAuth2 consent        │                       │
     │                    ├───────────────────────────────────────────────>│
     │                    │                        │                       │
     │                    │  User grants permission│                       │
     │                    │<───────────────────────────────────────────────┤
     │                    │                        │                       │
     │                    │  Receive ID Token      │                       │
     │                    │<───────────────────────────────────────────────┤
     │                    │                        │                       │
     │                    │  POST /api/auth/google │                       │
     │                    │  { idToken: "..." }    │                       │
     │                    ├───────────────────────>│                       │
     │                    │                        │                       │
     │                    │                        │  Verify token         │
     │                    │                        ├──────────────────────>│
     │                    │                        │                       │
     │                    │                        │  Token valid ✓        │
     │                    │                        │<──────────────────────┤
     │                    │                        │                       │
     │                    │                        │  Create/Update user   │
     │                    │                        │  in database          │
     │                    │                        ├────────┐              │
     │                    │                        │        │              │
     │                    │                        │<───────┘              │
     │                    │                        │                       │
     │                    │  { accessToken,        │                       │
     │                    │    refreshToken }      │                       │
     │                    │<───────────────────────┤                       │
     │                    │                        │                       │
     │  Show user profile │                        │                       │
     │<───────────────────┤                        │                       │
     │                    │                        │                       │
```

---

## 📚 Phần 6: Testing Checklist

- [ ] Google Cloud Console project đã tạo
- [ ] OAuth2 credentials đã tạo
- [ ] Client ID đã cấu hình trong `application.properties`
- [ ] User-service đã restart sau khi cấu hình
- [ ] OAuth Playground test thành công
- [ ] Lấy được ID Token
- [ ] API `/api/auth/google` response 200 OK
- [ ] Access token hoạt động với các API khác
- [ ] User được tạo trong database với `google_id`
- [ ] Avatar từ Google được lưu đúng
- [ ] Login lần 2 không tạo user trùng

---

## 🎉 Kết luận

Chức năng đăng nhập với Google đã hoàn thành! User có thể:
- ✅ Đăng nhập nhanh chóng bằng Google account
- ✅ Không cần nhớ thêm username/password
- ✅ Avatar tự động đồng bộ từ Google
- ✅ An toàn với OAuth2 standard

**Lưu ý:** 
- ID Token chỉ có hiệu lực trong 1 giờ
- Cần lấy token mới khi test sau 1 giờ
- Trong production, frontend sẽ tự động xử lý việc này

