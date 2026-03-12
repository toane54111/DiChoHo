# GoMarket - Đi Chợ Hộ 🛒

Ứng dụng Android gợi ý món ăn thông minh dựa trên thời tiết, sử dụng AI + RAG Vector Search.

## Flow hoạt động

```
GPS → OpenWeather API → LLM gợi ý món ăn → RAG Search nguyên liệu trong DB → A* Route tối ưu → Hiển thị
```

## Yêu cầu hệ thống

- **Android Studio** (Hedgehog trở lên)
- **JDK 17+**
- **PostgreSQL 15+** với extension **pgvector**
- **Ollama** (chạy model `bge-m3` cho embedding)
- **Maven 3.8+** (hoặc dùng Maven Wrapper có sẵn)

## Hướng dẫn cài đặt cho thành viên mới

### 1. Clone project

```bash
git clone https://github.com/toane54111/DiChoHo.git
cd DiChoHo
```

### 2. Cài đặt PostgreSQL + pgvector

```bash
# Tạo database
psql -U postgres
CREATE DATABASE gomarket;
\c gomarket
CREATE EXTENSION IF NOT EXISTS vector;
\q
```

> Mặc định username: `postgres`, password: `123`. Nếu khác thì sửa trong file secret (bước 3).

### 3. Cấu hình API Keys (BẮT BUỘC)

File chứa key **không được push lên GitHub**. Mỗi người phải tự tạo:

```bash
cd backend/src/main/resources/

# Copy template
cp application-secret.yml.example application-secret.yml
```

Mở `application-secret.yml` và điền key thật:

```yaml
api:
  openweather:
    key: <xin key từ nhóm trưởng>
  openrouter:
    key: <xin key từ nhóm trưởng>
```

**Lấy key ở đâu?**
- **OpenWeather**: Đăng ký miễn phí tại https://openweathermap.org/api (hoặc xin nhóm trưởng)
- **OpenRouter**: Đăng ký tại https://openrouter.ai (hoặc xin nhóm trưởng)

### 4. Cài đặt Ollama + model embedding

```bash
# Cài Ollama: https://ollama.ai
# Tải model embedding đa ngôn ngữ
ollama pull bge-m3
# Kiểm tra Ollama đang chạy
curl http://localhost:11434/api/tags
```

### 5. Chạy Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend sẽ chạy tại `http://localhost:8080`

### 6. Chạy Android App

1. Mở thư mục gốc project bằng **Android Studio**
2. Đợi Gradle sync xong
3. Chạy app trên **Emulator** (đã cấu hình `10.0.2.2:8080` cho localhost)
4. Nếu chạy trên **điện thoại thật**: sửa `BASE_URL` trong `app/src/main/java/com/example/gomarket/network/ApiClient.java` thành IP máy tính của bạn

## Cấu trúc project

```
DiChoHo/
├── app/                          # Android App (Java)
│   └── src/main/java/.../
│       ├── AIChefActivity.java   # Màn hình chính gợi ý món ăn
│       ├── model/                # Data models (Recipe, Product, ...)
│       ├── network/              # Retrofit API client
│       └── adapter/              # RecyclerView adapters
├── backend/                      # Spring Boot Backend
│   └── src/main/java/.../
│       ├── controller/           # REST APIs
│       ├── service/              # Business logic (Weather, Gemini, RAG, Route)
│       ├── model/                # JPA Entities
│       ├── repository/           # Spring Data repositories
│       └── dto/                  # Request/Response DTOs
└── README.md
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/recipe/weather?latitude=&longitude=` | Lấy thời tiết nhanh |
| POST | `/api/recipe/suggest` | Gợi ý món ăn + nguyên liệu + lộ trình |

## Tech Stack

- **Android**: Java, Retrofit, Glide, Google Maps & Location
- **Backend**: Spring Boot 3.2, JPA/Hibernate, PostgreSQL + pgvector
- **AI/ML**: OpenRouter (Gemini 2.0 Flash), Ollama bge-m3 embedding
- **APIs**: OpenWeather, Google Images

## Lưu ý quan trọng

- **KHÔNG commit file `application-secret.yml`** - file này chứa key thật
- File `.gitignore` đã được cấu hình để tự động bỏ qua file secret
- Nếu cần thêm key mới, thêm vào `application-secret.yml` và cập nhật file `.example`
