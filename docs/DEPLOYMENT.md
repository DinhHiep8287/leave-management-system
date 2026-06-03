# Deployment Guide

Hướng dẫn triển khai production cho Leave Management System. Bản này mô tả cách chạy
toàn bộ stack bằng Docker Compose trên một host, và gợi ý cho kiểu deploy tách dịch vụ.
Đây là **hướng dẫn** — chưa deploy thật lên cloud nào.

## Kiến trúc production

- **postgres** — PostgreSQL 16, dữ liệu trong named volume `postgres-data-prod`.
- **backend** — jar Spring Boot (multi-stage `backend/Dockerfile`), profile `prod`,
  Flyway tự migrate khi khởi động. Không publish cổng ra ngoài (chỉ truy cập nội bộ).
- **frontend** — SPA build sẵn, phục vụ bởi nginx; nginx **proxy `/api` → backend**.
  Chỉ cổng frontend (`:80`) lộ ra ngoài → FE và API **cùng origin**.

```
Internet ──▶ frontend (nginx :80) ──┬─▶ SPA tĩnh
                                     └─▶ /api/* ──▶ backend :8080 ──▶ postgres
```

## Yêu cầu

- Docker + Docker Compose.
- File `.env` (copy từ `.env.prod.example`) với secret mạnh. **Không commit `.env`.**

## Các bước

### 1. Chuẩn bị biến môi trường

```bash
cp .env.prod.example .env
# Sinh JWT secret (>=32 ký tự):
openssl rand -base64 48
# Điền JWT_SECRET, POSTGRES_PASSWORD, APP_CORS_ALLOWED_ORIGINS, VITE_API_BASE_URL.
```

`docker-compose.prod.yml` sẽ **báo lỗi ngay** nếu thiếu `JWT_SECRET` hoặc `POSTGRES_PASSWORD`.

### 2. Build & chạy

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Thứ tự: postgres (chờ healthy) → backend (Flyway migrate tự động) → frontend (nginx).

### 3. Kiểm tra

```bash
# Health (actuator, đã expose health+info ở profile prod):
docker compose -f docker-compose.prod.yml exec backend wget -qO- http://localhost:8080/api/actuator/health
# Kỳ vọng: {"status":"UP"}

# Mở SPA:
#   http://<host>:<FRONTEND_PORT>   (mặc định cổng 80)
```

### 4. Tạo tài khoản ADMIN đầu tiên

Profile `prod` **không** seed user demo (chỉ profile `dev` có `DemoDataInitializer`).
Tạo admin đầu tiên thủ công bằng SQL (password phải là hash BCrypt):

```bash
# Sinh BCrypt hash cho mật khẩu (ví dụ dùng htpasswd hoặc thư viện bất kỳ):
#   htpasswd -bnBC 12 "" 'MatKhauManh' | tr -d ':\n'
docker compose -f docker-compose.prod.yml exec postgres psql -U leave_admin -d leave_management -c \
  "INSERT INTO users (employee_code,email,password_hash,full_name,role,department_id,join_date,is_active,created_at,updated_at,created_by,updated_by)
   VALUES ('E0001','admin@yourco.com','<BCRYPT_HASH>','Quản trị viên',(SELECT 'ADMIN'),
   (SELECT id FROM departments WHERE code='HR'), '2024-01-01', TRUE, NOW(), NOW(), 'system', 'system');"
```

Sau khi đăng nhập, dùng giao diện quản trị để tạo phòng ban/người dùng còn lại và khởi tạo
quỹ phép năm (`POST /leave-balances/initialize?year=`).

## Vận hành

```bash
# Log
docker compose -f docker-compose.prod.yml logs -f backend
# Dừng
docker compose -f docker-compose.prod.yml down
# Backup DB
docker compose -f docker-compose.prod.yml exec postgres pg_dump -U leave_admin leave_management > backup.sql
# Cập nhật phiên bản (rebuild)
git pull && docker compose -f docker-compose.prod.yml up -d --build
```

## Deploy tách dịch vụ (gợi ý, chưa thực thi)

Khi không chạy chung một host:

- **Database**: dịch vụ Postgres quản lý như **Neon** / RDS. Đặt `SPRING_DATASOURCE_*` trỏ tới đó.
- **Backend**: build image từ `backend/Dockerfile`, deploy lên **Railway** / **Fly.io**.
  Cấu hình env: `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_*`, `JWT_SECRET`,
  `APP_CORS_ALLOWED_ORIGINS=https://<domain-FE>`.
- **Frontend**: build tĩnh (`pnpm build`) với `VITE_API_BASE_URL=https://<api-domain>/api`,
  deploy lên **Vercel** / **Netlify**. Khi đó block proxy `/api` trong `nginx.conf` không dùng tới.

## Checklist bảo mật

- [ ] `.env` không nằm trong git; secret sinh ngẫu nhiên, đủ dài.
- [ ] Swagger UI đã tắt ở prod (profile `prod`).
- [ ] HTTPS qua reverse proxy / nền tảng host (compose này phục vụ HTTP, đặt sau TLS).
- [ ] `APP_CORS_ALLOWED_ORIGINS` đúng domain thật.
- [ ] Sao lưu DB định kỳ; xoay vòng `JWT_SECRET` khi cần (sẽ vô hiệu hóa token hiện hành).
