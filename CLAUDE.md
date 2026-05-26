# CLAUDE.md

Tài liệu hướng dẫn cho Claude Code khi làm việc trong repo này.

## Bối cảnh dự án

- **Tên**: Leave Management System
- **Loại**: Dự án cá nhân (portfolio), không phải sản phẩm thương mại.
- **Mục tiêu**: Xây dựng hệ thống quản lý nghỉ phép chỉn chu, học sâu Spring Boot + React, hoàn thành MVP trong ~4 tuần.
- **Phong cách user mong đợi**: Cẩn thận, chi tiết, đúng best practice — không cắt góc.

## Tech stack đã chốt

| Lớp | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16, Flyway migration |
| Build | Gradle (Kotlin DSL) qua Gradle Wrapper |
| Frontend | React 18 + TypeScript + Vite + Tailwind + shadcn/ui |
| State | TanStack Query (server), React Hook Form + Zod (form) |
| Hạ tầng | Docker + Docker Compose (toàn bộ stack chạy trong container) |
| CI | GitHub Actions |

## Quyết định kiến trúc quan trọng

1. **Dockerize toàn bộ từ đầu** — kể cả backend & frontend dev đều chạy trong container. Tận dụng Spring DevTools + Vite HMR có polling để hot-reload qua volume mount.
2. **Package by feature** (không phải by layer) — `auth/`, `user/`, `leaverequest/`… mỗi feature có controller/service/repository riêng.
3. **DTO tách rời Entity** — không bao giờ trả Entity qua API.
4. **Flyway từ ngày đầu** — không dùng `ddl-auto: create/update`. Schema thay đổi qua migration.
5. **Test có Testcontainers** — integration test dùng Postgres thật trong container.
6. **JWT auth** — access token + refresh token.

## Quy ước code

### Backend
- Tên package gốc: `com.peih68.leave`
- Class naming: `XxxController`, `XxxService`, `XxxRepository`, `XxxEntity`, `XxxDto`, `XxxMapper`.
- Database column: `snake_case`. Java field: `camelCase`. URL: `kebab-case`.
- Mọi entity kế thừa `BaseEntity` với `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`.
- Exception handler tập trung qua `@RestControllerAdvice`.
- Response wrapper nhất quán: `{ data, error, meta }`.

### Frontend
- Cấu trúc: `src/features/<feature>/` (components, hooks, api, types).
- Component: PascalCase. Hook: `useXxx`. File: kebab-case hoặc PascalCase theo loại.
- Mọi API call đi qua TanStack Query, không gọi axios trực tiếp trong component.
- Form bắt buộc validate qua Zod schema.

### Commit
- **Conventional Commits**: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`, `ci:`.
- Một commit = một việc rõ ràng, không gộp nhiều thay đổi không liên quan.

## Lệnh thường dùng

```bash
# Khởi động toàn bộ stack dev
docker compose up

# Build lại image sau khi đổi Dockerfile / dependencies
docker compose up --build

# Chạy test backend
docker compose exec backend ./gradlew test

# Chạy lint/format frontend
docker compose exec frontend npm run lint
docker compose exec frontend npm run format

# Reset database
docker compose down -v && docker compose up

# Vào shell container
docker compose exec backend bash
docker compose exec postgres psql -U leave_admin -d leave_management
```

## Lưu ý cho Claude

- **Đừng over-engineer**: không tự thêm Kafka, Redis, microservices, CQRS nếu user chưa yêu cầu.
- **Đừng cài tool ngoài Docker**: mọi thứ phải chạy được qua `docker compose`.
- **Đừng skip Flyway**: schema thay đổi → tạo migration mới, không sửa migration cũ đã apply.
- **Đừng commit `.env`** hay bất kỳ secret nào.
- **Trước khi tạo file mới**: kiểm tra cấu trúc đã có. Ưu tiên sửa file hiện hữu.
- **Tài liệu cập nhật**: khi đổi tech stack hoặc kiến trúc → cập nhật `docs/ARCHITECTURE.md` và file này.
- **User dùng tiếng Việt** cho thảo luận. Documentation tiếng Việt OK. Code comment hạn chế, nếu có thì tiếng Anh.

## Lộ trình 4 tuần

- **Tuần 1**: Foundation — Docker Compose + skeleton backend/frontend + DB schema baseline. ✅ **Done**.
- **Tuần 2**: Auth (JWT) + User CRUD + Department + LeaveType/LeaveBalance CRUD + tests.
- **Tuần 3**: LeaveRequest + tính ngày (weekend + holiday) + approval workflow + audit.
- **Tuần 4**: Lịch tổng hợp + dashboard + báo cáo CSV + polish + production Dockerfile + deploy guide.

## Lưu ý vận hành (gotchas đã gặp)

- **Dev container backend chạy as root** (mặc định image `gradle:8.10-jdk21-alpine`). Lý do: named volume mount cho gradle cache được Docker init với owner `root` → user `gradle` không exec được JDK. **Đừng thêm `USER gradle`** trừ khi đồng thời fix init volume (entrypoint chown + gosu, hoặc bind mount, hoặc bỏ cache volume — hiện đang dùng cách bỏ cache).
- **Pull Docker Hub từ ISP Việt Nam** có thể đứt giữa stream khi tải blob lớn (`httpReadSeeker: EOF` từ Cloudfront). Workaround: dùng mirror `mirror.gcr.io` — xem `docs/DEVELOPMENT.md §12`.
- **Chạy backend test cần stop dev backend trước** rồi dùng `docker compose run --rm --no-deps backend ./gradlew test`. Lý do: dev container đang chạy `bootRun` đã chiếm ~500MB RAM; chạy thêm gradle test fork JVM con sẽ OOM (container memory 4GB của Docker Desktop). `docker compose run` spawn container mới không chạy `bootRun`.
- **Test dùng DB riêng `leave_management_test`** (tạo qua `docker compose exec postgres createdb -U leave_admin leave_management_test`). Lý do: docker-compose env `SPRING_DATASOURCE_URL` ghi đè `application-test.yml`. Fix bằng `systemProperty(...)` trong `build.gradle.kts > tasks.withType<Test>` — Java system property thắng env var trong Spring config hierarchy. Đừng đổi env này trong compose.
- **Testcontainers không hoạt động bên trong container** trên Docker Desktop Windows (mount `/var/run/docker.sock` thì Docker Desktop trả ServerVersion rỗng → Testcontainers reject). Đã bỏ Testcontainers, dùng DB compose `leave_management_test`. Nếu muốn lại Testcontainers, chạy tests từ host (cần Java 21 local) hoặc dùng `tecnativa/docker-socket-proxy` sidecar.
- **Spring Data Auditing với `OffsetDateTime`** cần `DateTimeProvider` bean tường minh — default chỉ trả `LocalDateTime`/`Instant`. Xem `JpaAuditingConfig#auditingDateTimeProvider`.
- **Gradle 9.x** yêu cầu `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` tường minh, khác Gradle 8.
- **TestRestTemplate + JDK HttpURLConnection** ném `HttpRetryException: cannot retry due to server authentication, in streaming mode` khi nhận 401 với body POST. Đã thêm `org.apache.httpcomponents.client5:httpclient5` vào `testImplementation` để Spring Boot auto-detect và dùng HttpClient5.
