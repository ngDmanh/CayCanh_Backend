# 🌿 Cây Cảnh - Backend (Spring Boot)

Máy chủ ứng dụng cho hệ thống quản lý và kinh doanh cây cảnh (mua + cho thuê).
Cung cấp REST API cho ứng dụng di động Android.

> 📱 Ứng dụng di động (Android): xem repo riêng — https://github.com/ngDmanh/CayCanh_Mobile

---

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 3.5.13 |
| Cơ sở dữ liệu | PostgreSQL |
| Truy cập dữ liệu | Spring Data JPA / Hibernate |
| Bảo mật | Spring Security + JWT |
| Lưu trữ ảnh | Cloudinary |
| Gửi email | JavaMailSender (OTP) |

---

## 2. Yêu cầu môi trường

Trước khi cài đặt, máy cần có:

- **JDK 21** trở lên — kiểm tra bằng lệnh `java -version`
- **PostgreSQL** (bản 14 trở lên khuyến nghị) — cài trực tiếp trên máy
- **Git** để tải mã nguồn
- Một tài khoản **Cloudinary** (miễn phí) để lấy thông tin lưu ảnh
- Một địa chỉ **email Gmail** (hoặc SMTP khác) để gửi mã OTP

---

## 3. Tải mã nguồn

```bash
git clone <ĐƯỜNG_DẪN_REPO_BACKEND>
cd <TÊN_THƯ_MỤC_BACKEND>
```

---

## 4. Cài đặt cơ sở dữ liệu PostgreSQL

### 4.1. Tạo database

Mở công cụ dòng lệnh `psql` hoặc pgAdmin, tạo một database mới:

```sql
CREATE DATABASE caycanh;
```

### 4.2. Khởi tạo bảng

Chạy tập lệnh SQL khởi tạo (gồm 12 bảng + trigger + view) trong dự án.
Ví dụ với psql:

```bash
psql -U postgres -d caycanh -f schema.sql
```

> 💡 Nếu dự án bật `spring.jpa.hibernate.ddl-auto=update`, các bảng sẽ tự tạo
> khi khởi động. Tuy nhiên các **trigger** và **view** cần chạy script SQL thủ công.

---

## 5. Cấu hình ứng dụng

Mở tệp `src/main/resources/application.properties`
(hoặc `application.yml`) và cập nhật các thông tin sau:

```properties
# === Cơ sở dữ liệu ===
spring.datasource.url=jdbc:postgresql://localhost:5432/caycanh
spring.datasource.username=postgres
spring.datasource.password=MẬT_KHẨU_POSTGRES_CỦA_BẠN

# === JWT ===
app.jwt.secret=CHUỖI_BÍ_MẬT_DÀI_NGẪU_NHIÊN
app.jwt.expiration=7200000   # 120 phút (đơn vị mili-giây)

# === Cloudinary ===
cloudinary.cloud-name=TÊN_CLOUD_CỦA_BẠN
cloudinary.api-key=API_KEY
cloudinary.api-secret=API_SECRET

# === Email (gửi OTP) ===
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=EMAIL_CỦA_BẠN@gmail.com
spring.mail.password=MẬT_KHẨU_ỨNG_DỤNG_GMAIL
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> ⚠️ **Lưu ý bảo mật:** không đẩy file chứa mật khẩu thật lên GitHub.
> Nên dùng biến môi trường hoặc file `application-local.properties` và thêm vào `.gitignore`.

> 💡 Với Gmail, cần bật "Mật khẩu ứng dụng" (App Password) thay cho mật khẩu thường.

---

## 6. Chạy ứng dụng

Tùy theo dự án dùng **Maven** hay **Gradle** (kiểm tra trong thư mục gốc:
có `pom.xml` là Maven, có `build.gradle` là Gradle).

### Nếu dùng Maven

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### Nếu dùng Gradle

```bash
# Linux / macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

Sau khi khởi động thành công, máy chủ chạy tại:

```
http://localhost:8080
```

---

## 7. Kiểm tra hoạt động

- Tài liệu API (nếu bật Swagger): `http://localhost:8080/swagger-ui.html`
- Thử một API công khai bằng trình duyệt hoặc Postman:

```
GET http://localhost:8080/api/plants
```

Nếu trả về danh sách cây (hoặc mảng rỗng `[]` khi chưa có dữ liệu) là máy chủ đã chạy đúng.

---

## 8. Kết nối với ứng dụng di động

Trong dự án Android, cấu hình `BASE_URL` trỏ về máy chủ:

- Máy ảo Android (emulator): `http://10.0.2.2:8080/`
  *(10.0.2.2 là địa chỉ máy tính chủ nhìn từ máy ảo)*
- Thiết bị thật cùng mạng Wi-Fi: `http://ĐỊA_CHỈ_IP_MÁY_TÍNH:8080/`

---

## 9. Cấu trúc thư mục (tham khảo)

```
src/main/java/com/caycanh/caycanh_backend/
├── controller/    # Tiếp nhận request (REST API)
├── service/       # Xử lý nghiệp vụ
├── repository/    # Truy cập cơ sở dữ liệu (JPA)
├── entity/        # Các thực thể ánh xạ bảng
├── dto/           # Đối tượng truyền dữ liệu
├── config/        # Cấu hình chung, scheduler, hằng số, OpenAPI
└── security/      # JWT (JwtUtil, JwtFilter), cấu hình bảo mật
```

---

## 10. Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân & cách khắc phục |
|---|---|
| `Connection refused` đến CSDL | PostgreSQL chưa chạy, hoặc sai cổng/mật khẩu trong `application.properties` |
| `relation ... does not exist` | Chưa chạy script tạo bảng, hoặc sai tên database |
| Không gửi được OTP | Sai cấu hình email, hoặc chưa bật App Password của Gmail |
| Tải ảnh thất bại | Sai thông tin Cloudinary (cloud-name / api-key / api-secret) |
| App điện thoại không gọi được API | Sai `BASE_URL`, hoặc máy chủ và thiết bị không cùng mạng |
