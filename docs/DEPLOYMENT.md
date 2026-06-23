# Deployment Guide

Hướng dẫn triển khai production cho Leave Management System. Có hai phương án:

1. **Một host bằng Docker Compose** (phần đầu tài liệu) — VPS tự quản.
2. **Tách dịch vụ Railway + Neon + Vercel** (phần "Deploy tách dịch vụ") — **đã deploy thật**
   từ v1.2.0; demo trực tuyến link trong README.

`v2.0.0` là lần nâng cấp production cuối dự kiến của miniproject. Sau mốc này, tính năng mới chỉ
chạy demo/test local. Trước khi dựng production Compose, phải tạo `.env` từ `.env.prod.example`;
không dùng lại `.env` dev vì `VITE_API_BASE_URL` của dev trỏ trực tiếp tới `localhost:8080`.

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

## Deploy tách dịch vụ: Railway + Neon + Vercel (đã thực thi — v1.2.0)

```
Browser ──▶ Vercel (SPA + rewrite /api) ──▶ Railway (Spring Boot, Docker) ──▶ Neon (Postgres)
```

Trình tự khuyến nghị: **Neon → Vercel → Railway cuối cùng** (trial Railway $5 hết hạn sau
30 ngày kể từ lúc tạo tài khoản — để đồng hồ chạy muộn nhất).

### 1. Neon (Postgres, free)

- Tạo project (region **ap-southeast-1 Singapore**, Postgres 16). Lấy **connection string
  DIRECT** — KHÔNG dùng endpoint `-pooler` (Hikari đã là pool; Flyway không hợp PgBouncer
  transaction mode). Bỏ tham số `channel_binding` khi chuyển sang dạng JDBC.
- Chạy thử backend prod local trỏ Neon để Flyway migrate:

```bash
docker compose -f docker-compose.prod.yml build backend
docker run --rm -p 8081:8080   -e SPRING_PROFILES_ACTIVE=prod   -e SPRING_DATASOURCE_URL='jdbc:postgresql://<host>/neondb?sslmode=require'   -e SPRING_DATASOURCE_USERNAME=... -e SPRING_DATASOURCE_PASSWORD=...   -e JWT_SECRET=<tạm ≥32 ký tự> -e TZ=Asia/Ho_Chi_Minh   leave-management-system-backend
# chờ /api/actuator/health = UP, log Flyway "applied 3 migrations"
# LƯU Ý: build prod ghi đè image dev cùng tên → sau đó `docker compose build backend`
```

- **(Tùy chọn, dùng cho demo) seed dữ liệu demo MỘT lần**: chạy lại container trên với
  `SPRING_PROFILES_ACTIVE=dev` → seeder đổ 19 user + ~63 đơn; tắt container ngay sau log
  "Seeded ... demo leave requests". Prod sau đó không bao giờ seed lại (DB đã có user).
- **Đổi mật khẩu admin** thành mật khẩu mạnh riêng (mật khẩu demo là công khai). Dùng
  pgcrypto ngay trên Neon (không cần htpasswd):

```bash
docker run --rm postgres:16-alpine psql "postgresql://<user>:<pw>@<host>/neondb?sslmode=require"   -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"   -c "UPDATE users SET password_hash = crypt('<MẬT-KHẨU-MỚI>', gen_salt('bf', 12)) WHERE email='admin@demo.local';"
```

### 2. Vercel (frontend, free Hobby)

- Import repo → **Root Directory = `frontend`**, framework Vite.
- Env var bắt buộc: **`VITE_API_BASE_URL=/api`** (fallback trong `lib/api.ts` là localhost).
- `frontend/vercel.json` (đã có trong repo) lo: rewrite `/api/*` → domain Railway
  (same-origin với browser), SPA fallback, security headers + HSTS.

### 3. Railway (backend, trial $5/30 ngày → Hobby $5/tháng)

- New Project → Deploy from GitHub repo → **Settings → Root Directory = `backend`**
  (tự nhận `backend/Dockerfile`).
- Variables:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-host>/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=<sinh mới ≥32 ký tự — KHÔNG dùng secret dev>
JAVA_TOOL_OPTIONS=-Xmx320m -XX:MaxMetaspaceSize=128m
TZ=Asia/Ho_Chi_Minh
APP_CORS_ALLOWED_ORIGINS=https://<app>.vercel.app
```

  **`APP_CORS_ALLOWED_ORIGINS` là bắt buộc kể cả khi dùng Vercel rewrites**: browser luôn
  gửi header `Origin` với POST; Spring CORS thấy Origin không nằm trong allowlist sẽ trả
  **403** dù request đến qua proxy. (Quên biến này → login fail 403, curl không Origin vẫn 200.)
- **Deploy → Serverless (App Sleeping): BẬT** — ngủ sau ~10 phút không có traffic, không tính
  phí compute khi ngủ. KHÔNG đặt uptime ping nếu bật (xung đột, đốt credit).
  Cap RAM bằng `JAVA_TOOL_OPTIONS` ở trên → burn khi thức ~$4/tháng.
- Healthcheck Path: `/api/actuator/health`. Region: Southeast Asia nếu chọn được (gần Neon).
- Networking → Generate Domain (target port **8080**) → điền domain vào
  `frontend/vercel.json` (rewrite destination) → commit + push → Vercel tự redeploy.

### 4. Kiểm tra sau deploy

```bash
curl https://<railway-domain>/api/actuator/health          # {"status":"UP",...}
# Browser: https://<app>.vercel.app → login demo → dashboard/lịch có dữ liệu
```

### 5. Vận hành

- **Cold start**: App Sleeping + Neon scale-to-zero → lượt truy cập đầu sau khi nhàn rỗi chờ
  ~20-40s. Đã ghi chú trong README cho người xem demo.
- **Backup**: Neon free có restore-window tích hợp (PITR ngắn). Backup tay định kỳ từ máy local
  (KHÔNG đẩy dump lên GitHub Actions artifact — repo public):

```bash
docker run --rm postgres:16-alpine pg_dump "postgresql://<user>:<pw>@<host>/neondb?sslmode=require" > backup-$(date +%F).sql
```

- **Email notification giữ TẮT ở prod**: v2.0.0 dùng thông báo in-app trên production; SMTP/Mailpit
  chỉ dùng để kiểm thử email ở local. Không cấu hình `MAIL_ENABLED=true` trên Railway.
- **Chi phí**: xem Railway Usage sau 4-5 ngày để ngoại suy $/tháng và quyết định lên Hobby.
- **Hikari + Neon**: prod đã cấu hình pool nhỏ, `idle-timeout` 2 phút và `max-lifetime` 4.5 phút
  (ngắn hơn mốc Neon suspend ~5 phút); cố ý KHÔNG bật keepalive vì sẽ giữ Neon thức và tốn
  compute-hours.

## Checklist bảo mật

- [ ] `.env` không nằm trong git; secret sinh ngẫu nhiên, đủ dài.
- [ ] Swagger UI đã tắt ở prod (profile `prod`).
- [ ] HTTPS qua reverse proxy / nền tảng host (compose này phục vụ HTTP, đặt sau TLS).
- [ ] `APP_CORS_ALLOWED_ORIGINS` đúng domain thật.
- [ ] Sao lưu DB định kỳ; xoay vòng `JWT_SECRET` khi cần (sẽ vô hiệu hóa token hiện hành).
