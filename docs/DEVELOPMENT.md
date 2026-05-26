# Hướng dẫn phát triển

## 1. Yêu cầu môi trường

| Bắt buộc | Khuyến nghị |
|---|---|
| Docker Desktop (Engine 24+, Compose v2) | IntelliJ IDEA Community (cho backend) |
| Git | VS Code (cho frontend) |
| | Java 21 trên host (để IDE autocomplete) |
| | Node 22 trên host (để IDE autocomplete) |
| | DBeaver (xem database) |
| | pnpm (thay npm, nhanh hơn) |

## 2. Lần đầu setup

```bash
# 1. Clone
git clone <repo-url>
cd leave-management-system

# 2. Tạo .env từ template
cp .env.example .env
# (Đổi JWT_SECRET và password Postgres trong .env)

# 3. Khởi động toàn bộ stack — lần đầu sẽ build image (3-5 phút)
docker compose up --build

# 4. Đợi tới khi log hiện:
#    backend  | Started LeaveApplication ...
#    frontend | Local: http://localhost:5173

# 5. Mở browser:
#    http://localhost:5173
```

## 3. Workflow hằng ngày

### Khởi động
```bash
docker compose up           # foreground
docker compose up -d        # background
```

### Dừng
```bash
docker compose down         # giữ data
docker compose down -v      # xóa cả volume (reset DB)
```

### Xem log
```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f --tail=100
```

### Vào shell container
```bash
docker compose exec backend bash
docker compose exec frontend sh
docker compose exec postgres psql -U leave_admin -d leave_management
```

## 4. Hot reload

| Service | Cơ chế | Trigger |
|---|---|---|
| Backend | Spring DevTools + Docker Compose support | Sửa `.java` → tự restart context (3-5s) |
| Frontend | Vite HMR | Sửa `.tsx/.ts/.css` → reload ngay (<1s) |
| DB Migration | Flyway chạy lúc backend start | Restart container backend |

**Lưu ý Windows**: file watching qua volume mount có thể chậm. Đã cấu hình polling sẵn. Nếu vẫn lỗi, restart container.

## 5. Database

### Kết nối từ host
- Host: `localhost`
- Port: `5432` (theo `.env`)
- DB: `leave_management`
- User: `leave_admin`
- Password: theo `.env`

### Migration mới
Tạo file mới trong `backend/src/main/resources/db/migration/`:
```
V<N>__<mô_tả_ngắn>.sql
```
Ví dụ: `V5__add_request_attachment_url.sql`.

**KHÔNG sửa migration đã apply** (Flyway sẽ fail checksum).

### Reset hoàn toàn
```bash
docker compose down -v
docker compose up
```

## 6. Testing

### Backend
```bash
# Toàn bộ test
docker compose exec backend ./gradlew test

# 1 class
docker compose exec backend ./gradlew test --tests com.example.leave.user.UserServiceTest

# Với report HTML
docker compose exec backend ./gradlew test jacocoTestReport
# Report: backend/build/reports/jacoco/test/html/index.html
```

### Frontend
```bash
docker compose exec frontend npm run test
docker compose exec frontend npm run test:watch
```

## 7. Code quality

```bash
# Backend format
docker compose exec backend ./gradlew spotlessApply
docker compose exec backend ./gradlew spotlessCheck

# Frontend lint/format
docker compose exec frontend npm run lint
docker compose exec frontend npm run format
```

## 8. Debug

### Backend (IntelliJ)
1. Backend đã expose port `5005` cho remote debug.
2. IntelliJ → Run → Edit Configurations → `+` → Remote JVM Debug.
3. Host: `localhost`, Port: `5005`.
4. Đặt breakpoint, bấm Debug.

### Frontend
React DevTools + browser DevTools.

## 9. API documentation

Swagger UI tự sinh:
- http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs

## 10. Quy ước commit

Theo [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add leave request creation endpoint
fix: correct half-day calculation when crossing weekend
docs: update database schema doc
chore: bump spring boot to 3.3.5
refactor: extract holiday checking to domain service
test: add integration test for approval flow
ci: add gradle build cache
```

Body (tùy chọn) giải thích **why**, không lặp lại **what**.

## 11. Branch strategy (đơn giản cho personal project)

- `main`: branch ổn định, luôn deploy được.
- `feature/<tên-ngắn>`: tính năng mới.
- `fix/<tên-ngắn>`: sửa lỗi.
- Merge vào `main` qua PR (kể cả một mình — để có lịch sử rõ).

## 12. Troubleshooting

| Vấn đề | Cách xử lý |
|---|---|
| Port 8080/5173/5432 bị chiếm | Đổi port trong `.env` hoặc dừng app đang chiếm |
| Hot-reload không chạy | `docker compose restart backend` hoặc `frontend` |
| Flyway checksum mismatch | Bạn đã sửa migration cũ. Reset DB: `docker compose down -v` |
| `node_modules` permission error trên Windows | Container dùng named volume cho `node_modules`, không mount từ host |
| Backend start chậm | Lần đầu Gradle download deps. Có cache volume `gradle-cache` |
| OOM khi build | Tăng RAM Docker Desktop ≥ 4GB |

## 13. Cấu trúc port (tham khảo)

| Service | Port host | Port container |
|---|---|---|
| Frontend (Vite) | 5173 | 5173 |
| Backend (Spring Boot) | 8080 | 8080 |
| Backend debug (JDWP) | 5005 | 5005 |
| Postgres | 5432 | 5432 |
