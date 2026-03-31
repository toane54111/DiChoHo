# GoMarket - Đi Chợ Hộ

Ứng dụng di động kết nối người mua hàng với người đi chợ hộ (Shopper), tích hợp chợ đồng hương, AI gợi ý đặc sản và sổ tay nấu ăn thông minh.

## Tổng quan

GoMarket là nền tảng thương mại cộng đồng với 7 chức năng chính:

| Chức năng | Mô tả |
|-----------|-------|
| **Đi Chợ Hộ** | Tạo đơn nhờ người khác đi chợ, theo dõi realtime trên bản đồ |
| **Chợ Đồng Hương** | Chợ cộng đồng mua bán nông sản, đặc sản 63 tỉnh thành |
| **AI Thổ Địa** | Gợi ý đặc sản cá nhân hóa theo vị trí + thời tiết + khẩu vị |
| **Sổ Tay Nấu Ăn** | 30 công thức nấu ăn hệ thống + cộng đồng, có ảnh minh họa |
| **Ví Điện Tử** | Nạp tiền, thanh toán, đóng băng/hoàn tiền tự động |
| **Chat** | Nhắn tin realtime giữa người mua và shopper trong đơn hàng |
| **Từ Điển Sản Phẩm** | Tìm kiếm hybrid (text + vector semantic), autocomplete |

## Tech Stack

| Layer | Công nghệ |
|-------|-----------|
| **Android** | Java, Retrofit 2, OSMDroid (OpenStreetMap), Glide, Material Design, ViewPager2 |
| **Backend** | Spring Boot 3.2.4, Spring Data JPA, Spring Security Crypto |
| **Database** | PostgreSQL 15+ với pgvector (vector similarity search) |
| **AI/LLM** | Gemini 2.0 Flash (qua OpenRouter), Ollama bge-m3 embeddings (1024-dim) |
| **API bên ngoài** | OpenWeather API, Google Images (scraping) |

## Yêu cầu hệ thống

- **Android Studio** Hedgehog trở lên
- **JDK 17+**
- **PostgreSQL 15+** với extension **pgvector**
- **Ollama** (chạy model `bge-m3` cho embedding)
- **Maven 3.8+** (hoặc dùng Maven Wrapper có sẵn)

## Hướng dẫn cài đặt

### 1. Clone project

```bash
git clone https://github.com/toane54111/DiChoHo.git
cd DiChoHo
```

### 2. Cài đặt PostgreSQL + pgvector

```bash
psql -U postgres
CREATE DATABASE gomarket;
\c gomarket
CREATE EXTENSION IF NOT EXISTS vector;
\q
```

> Mặc định username: `postgres`, password: `123`. Nếu khác thì sửa trong file secret (bước 3).

### 3. Cấu hình API Keys

File chứa key **không được push lên GitHub**. Mỗi người phải tự tạo:

```bash
cd backend/src/main/resources/
cp application-secret.yml.example application-secret.yml
```

Mở `application-secret.yml` và điền key:

```yaml
api:
  openweather:
    key: <your_openweather_key>
  openrouter:
    key: <your_openrouter_key>
```

- **OpenWeather**: https://openweathermap.org/api (miễn phí)
- **OpenRouter**: https://openrouter.ai

### 4. Cài đặt Ollama + model embedding

```bash
# Cài Ollama: https://ollama.ai
ollama pull bge-m3

# Kiểm tra
curl http://localhost:11434/api/tags
```

### 5. Chạy Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend chạy tại `http://localhost:8080`

### 6. Chạy Android App

1. Mở thư mục gốc bằng **Android Studio**
2. Đợi Gradle sync xong
3. Chạy trên **Emulator** (đã cấu hình auto-detect `10.0.2.2:8080`)
4. Chạy trên **điện thoại thật**: app tự detect IP qua WiFi, đảm bảo cùng mạng với máy chạy backend

### 7. Seed dữ liệu mẫu (tùy chọn)

Dữ liệu bài đăng và công thức nấu ăn được seed tự động khi khởi động. Nếu muốn re-seed:

```bash
# Re-seed bài đăng cộng đồng (50+ bài với ảnh)
curl -X POST http://localhost:8080/api/posts/re-seed

# Re-embed tất cả bài đăng (tạo vector cho search)
curl -X POST http://localhost:8080/api/posts/re-embed

# Re-seed công thức nấu ăn (30 món với ảnh)
curl -X POST http://localhost:8080/api/cookbook/re-seed
```

## Chức năng chi tiết

### Đi Chợ Hộ (Shopping Service)

**Luồng người mua:**
- Tạo đơn: chọn vị trí giao trên bản đồ, thêm nguyên liệu (autocomplete), đặt ngân sách + phí shopper
- Thanh toán: COD hoặc Ví điện tử
- Theo dõi đơn realtime: vị trí shopper trên bản đồ, chat trực tiếp
- Đánh giá sao khi hoàn thành

**Luồng Shopper:**
- Nhận đơn gần vị trí (Haversine 15km), toggle online/offline
- Cập nhật trạng thái từng sản phẩm, nhập giá thực tế
- GPS tracking liên tục khi đang giao

**Trạng thái đơn hàng:**
```
OPEN → ACCEPTED → SHOPPING → DELIVERING → COMPLETED
                              └─ CANCELLED (hoàn tiền ví)
```

### Chợ Đồng Hương (Community Marketplace)

- Đăng bài bán/mua với ảnh, 4 danh mục: Nông sản, Đặc sản, Rao vặt, Gom chung
- Lọc theo 3 vùng miền (Bắc/Trung/Nam) + 63 tỉnh thành
- Tìm kiếm ngữ nghĩa RAG (vector search + text fallback)
- Like, comment, liên hệ trực tiếp

### AI Thổ Địa (Gợi ý cá nhân hóa)

Pipeline 4 bước:
1. Lấy thời tiết + xác định mùa vụ theo tháng
2. Phân tích taste profile người dùng
3. Gemini LLM gợi ý 3 đặc sản phù hợp
4. RAG vector search ghép bài đăng cộng đồng liên quan

Hiển thị dạng chip ngang trên Home, cache 24h per user.

### Sổ Tay Nấu Ăn (Cookbook)

- 3 tab: Gợi ý hệ thống (20 món) / Cộng đồng (10 món) / Cá nhân
- Mỗi công thức có ảnh, nguyên liệu chi tiết, các bước nấu, giá ước tính
- Like, comment, lưu yêu thích
- Nút "Nhờ đi chợ hộ" chuyển nguyên liệu thành đơn hàng

### Ví Điện Tử (Wallet)

- Nạp tiền nhanh (50K, 100K, 200K, 500K)
- Đóng băng tiền khi đặt đơn, hoàn tiền khi hủy
- Lịch sử giao dịch (TOPUP / CHARGE / REFUND)
- QR code thanh toán

## Cấu trúc project

```
DiChoHo/
├── app/                                    # Android App (Java)
│   └── src/main/java/.../gomarket/
│       ├── HomeActivity.java               # Trang chủ + AI Thổ Địa chips
│       ├── CreateShoppingRequestActivity    # Tạo đơn đi chợ
│       ├── ShopperDashboardActivity         # Dashboard shopper
│       ├── OrderWaitingActivity             # Theo dõi đơn realtime
│       ├── CommunityFeedActivity            # Chợ đồng hương
│       ├── CreatePostActivity               # Đăng bài + ảnh
│       ├── CookbookActivity                 # Sổ tay nấu ăn (3 tab)
│       ├── WalletActivity                   # Ví điện tử
│       ├── OrderChatActivity                # Chat trong đơn
│       ├── ProfileActivity                  # Hồ sơ cá nhân
│       ├── SearchActivity                   # Tìm kiếm
│       ├── network/
│       │   ├── ApiService.java              # Tất cả API endpoints
│       │   └── ApiClient.java               # Retrofit config (auto-detect IP)
│       ├── model/                           # Data models (29 classes)
│       ├── adapter/                         # RecyclerView adapters (15 classes)
│       └── util/
│           └── SessionManager.java          # Quản lý session đăng nhập
│
├── backend/                                # Spring Boot Backend
│   └── src/main/java/.../gomarket/
│       ├── controller/                     # 12 REST Controllers
│       │   ├── AuthController              # /api/auth
│       │   ├── ShoppingRequestController   # /api/shopping-requests
│       │   ├── PostController              # /api/posts
│       │   ├── CookbookController          # /api/cookbook
│       │   ├── LocalGuideController        # /api/local-guide (AI Thổ Địa)
│       │   ├── ProductController           # /api/products
│       │   ├── WalletController            # /api/wallet
│       │   ├── ChatController              # /api/chat
│       │   ├── ReviewController            # /api/reviews
│       │   └── UploadController            # /api/upload
│       ├── service/                        # 18 Services
│       │   ├── GeminiService               # LLM calls (OpenRouter)
│       │   ├── LocalGuideService           # AI Thổ Địa logic
│       │   ├── EmbeddingService            # Ollama bge-m3
│       │   ├── WeatherService              # OpenWeather API
│       │   ├── ProductSearchService        # Hybrid search
│       │   └── ...
│       ├── model/                          # 19 JPA Entities
│       ├── repository/                     # 17 Spring Data Repositories
│       ├── dto/                            # Request/Response DTOs
│       └── config/                         # CORS, WebConfig, VectorConverter
│
└── backend/uploads/seed/                   # 65+ ảnh seed (Wikipedia)
```

## API Endpoints

### Authentication
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/login` | Đăng nhập |
| POST | `/api/auth/register` | Đăng ký |
| GET | `/api/auth/profile/{userId}` | Xem hồ sơ |
| PUT | `/api/auth/{userId}/online-status` | Bật/tắt online (Shopper) |
| PUT | `/api/auth/{userId}/location` | Cập nhật vị trí GPS |
| GET | `/api/auth/shoppers/nearby` | Tìm shopper gần |

### Shopping Requests
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/shopping-requests` | Tạo đơn |
| GET | `/api/shopping-requests/nearby` | Đơn gần vị trí |
| PUT | `/api/shopping-requests/{id}/accept` | Nhận đơn |
| PUT | `/api/shopping-requests/{id}/status` | Cập nhật trạng thái |
| PUT | `/api/shopping-requests/{id}/cancel` | Hủy đơn |

### Community Posts
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/posts` | Đăng bài |
| GET | `/api/posts/feed` | Feed (lọc category, region, province) |
| GET | `/api/posts/search?q=` | Tìm kiếm (RAG semantic) |
| GET | `/api/posts/provinces` | Danh sách 63 tỉnh thành |
| POST | `/api/posts/re-seed` | Re-seed dữ liệu mẫu |
| POST | `/api/posts/re-embed` | Re-embed tất cả bài |

### AI & Recipes
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/local-guide/suggestions` | AI Thổ Địa gợi ý |
| GET | `/api/cookbook/suggestions` | Công thức hệ thống |
| GET | `/api/cookbook/community` | Công thức cộng đồng |
| POST | `/api/cookbook/re-seed` | Re-seed công thức |

### Wallet & Chat
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/wallet/{userId}` | Xem số dư |
| POST | `/api/wallet/{userId}/topup` | Nạp tiền |
| POST | `/api/chat/send` | Gửi tin nhắn |
| GET | `/api/chat/{requestId}/messages` | Lấy tin nhắn |

### Products & Upload
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/products/hybrid-search?q=` | Tìm kiếm hybrid |
| GET | `/api/products/autocomplete?q=` | Gợi ý tự động |
| POST | `/api/upload/image` | Upload ảnh (max 10MB) |

## Database Schema

Sử dụng PostgreSQL với pgvector cho vector similarity search:

- **users** - Thông tin người dùng (BUYER/SHOPPER), vị trí GPS, rating
- **shopping_requests** - Đơn hàng với trạng thái lifecycle
- **shopping_request_items** - Chi tiết sản phẩm trong đơn
- **posts** - Bài đăng cộng đồng với embedding vector (1024-dim)
- **post_images** - Ảnh đính kèm bài đăng
- **cookbook_recipes** - Công thức nấu ăn (ingredients/steps dạng JSON)
- **wallets** - Số dư ví + đóng băng
- **wallet_transactions** - Lịch sử giao dịch (TOPUP/CHARGE/REFUND)
- **chat_messages** - Tin nhắn trong đơn hàng
- **products** - Từ điển sản phẩm với embedding cho semantic search
- **shopper_reviews** - Đánh giá shopper (1-5 sao)

## Build Commands

```bash
# Android - Build debug APK
JAVA_HOME="/path/to/jbr" ./gradlew assembleDebug

# Backend - Compile
cd backend && ./mvnw compile -q

# Backend - Run
cd backend && ./mvnw spring-boot:run

# Backend - Build JAR
cd backend && ./mvnw clean package
```

## Lưu ý quan trọng

- **KHÔNG commit** file `application-secret.yml` (chứa API keys)
- File `.gitignore` đã cấu hình bỏ qua file secret
- Android app tự detect IP backend (emulator: `10.0.2.2`, physical: WiFi IP)
- Ollama phải đang chạy để embedding/search hoạt động
- Upload ảnh tối đa 10MB, lưu tại `backend/uploads/`
