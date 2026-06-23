# Hướng dẫn phát triển

## Hỗ trợ coding agent

Repo hỗ trợ đồng thời Claude Code và Codex:

- Claude Code đọc chỉ dẫn dự án từ [`CLAUDE.md`](../CLAUDE.md).
- Codex đọc chỉ dẫn dự án từ [`AGENTS.md`](../AGENTS.md).
- Khi thay đổi tech stack, kiến trúc, lệnh phát triển hoặc gotcha vận hành, cập nhật đồng thời hai file để tránh chỉ dẫn bị lệch.
- Skills dùng chung nằm trong `.agents/skills/`; cache và thiết lập riêng theo máy không được commit.

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
# DB test bị xóa theo — tạo lại trước khi chạy backend test:
docker compose exec postgres createdb -U leave_admin leave_management_test
```

### Dữ liệu demo (profile dev)
Khi DB **trống**, `DemoDataInitializer` + `DemoLeaveSeeder` tự seed: 19 user (3 phòng ban,
xem tài khoản trong README), quỹ phép năm hiện tại + năm trước, ~63 đơn nghỉ trải 5 tháng
qua tới +30 ngày tới (đủ trạng thái, có người đang nghỉ hôm nay ở mỗi phòng), 2 điều chỉnh
quỹ có audit. Ngày tính **tương đối theo hôm nay** nên reset lúc nào dữ liệu cũng "tươi".
DB đã có user thì seeder bỏ qua — muốn seed lại phải reset hoàn toàn (ở trên).

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

### Frontend (Vitest)
```bash
# Unit tests (jsdom + Testing Library; file *.test.ts(x) cạnh mã nguồn)
docker compose exec frontend pnpm test
docker compose exec frontend pnpm test:watch

# Quality gate đầy đủ (giống CI)
docker compose exec frontend sh -lc "pnpm typecheck && pnpm lint && pnpm test && pnpm build"
```

### E2E (Playwright)
Xem `e2e/README.md` — chạy local với dev stack đang bật, và tự động trong CI (job `e2e`).

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

### Backend
Remote debug đang **tắt** ở MVP (giữ container chạy đơn giản). Khi cần bật, thêm vào service `backend` trong `docker-compose.yml`:

```yaml
environment:
  JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ports:
  - "5005:5005"
```

Sau đó IntelliJ → Run → Edit Configurations → Remote JVM Debug → Host `localhost`, Port `5005`.

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
| Backend start chậm | Lần đầu Gradle download deps (~vài phút). |
| OOM khi build | Tăng RAM Docker Desktop ≥ 4GB |
| `docker pull` fail với `httpReadSeeker: EOF` từ Cloudfront | ISP Việt Nam đôi khi drop connection khi tải blob lớn từ Docker Hub CDN. Workaround: pull qua mirror Google `docker pull mirror.gcr.io/library/<image>:<tag>` rồi `docker tag` lại tên gốc. Hoặc thêm `"registry-mirrors": ["https://mirror.gcr.io"]` vào `~/.docker/daemon.json` rồi restart Docker Desktop. |

## 13. Cấu trúc port (tham khảo)

| Service | Port host | Port container |
|---|---|---|
| Frontend (Vite) | 5173 | 5173 |
| Backend (Spring Boot) | 8080 | 8080 |
| Postgres | 5432 | 5432 |
