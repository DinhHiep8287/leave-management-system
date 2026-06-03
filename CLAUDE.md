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
- **Thiết kế UI theo `docs/UI-GUIDELINES.md`**: font Be Vietnam Pro, accent teal, text-led (icon tối thiểu), không "AI tells" (không Inter/tím-AI/em-dash/emoji). Áp dụng khi build mọi màn hình FE.

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
- **Tuần 2**: Auth (JWT) + User CRUD + Department + LeaveType/LeaveBalance CRUD + tests. ✅ **Done** (78 backend tests).
- **Tuần 3**: LeaveRequest + tính ngày (weekend + holiday) + approval workflow + audit. ✅ **Done** (131 backend tests).
- **Tuần 4**: Lịch tổng hợp + dashboard + báo cáo CSV + frontend đầy đủ + production config + deploy guide. ✅ **Done** (155 backend tests; FE: build/lint xanh).

## Lưu ý vận hành (gotchas đã gặp)

- **Dev container backend chạy as root** (mặc định image `gradle:8.10-jdk21-alpine`). Lý do: named volume mount cho gradle cache được Docker init với owner `root` → user `gradle` không exec được JDK. **Đừng thêm `USER gradle`** trừ khi đồng thời fix init volume (entrypoint chown + gosu, hoặc bind mount, hoặc bỏ cache volume — hiện đang dùng cách bỏ cache).
- **Pull Docker Hub từ ISP Việt Nam** có thể đứt giữa stream khi tải blob lớn (`httpReadSeeker: EOF` từ Cloudfront). Workaround: dùng mirror `mirror.gcr.io` — xem `docs/DEVELOPMENT.md §12`.
- **Chạy backend test cần stop dev backend trước** rồi dùng `docker compose run --rm --no-deps backend ./gradlew test`. Lý do: dev container đang chạy `bootRun` đã chiếm ~500MB RAM; chạy thêm gradle test fork JVM con sẽ OOM (container memory 4GB của Docker Desktop). `docker compose run` spawn container mới không chạy `bootRun`.
- **Test dùng DB riêng `leave_management_test`** (tạo qua `docker compose exec postgres createdb -U leave_admin leave_management_test`). Lý do: docker-compose env `SPRING_DATASOURCE_URL` ghi đè `application-test.yml`. Fix bằng `systemProperty(...)` trong `build.gradle.kts > tasks.withType<Test>` — Java system property thắng env var trong Spring config hierarchy. Đừng đổi env này trong compose.
- **Testcontainers không hoạt động bên trong container** trên Docker Desktop Windows (mount `/var/run/docker.sock` thì Docker Desktop trả ServerVersion rỗng → Testcontainers reject). Đã bỏ Testcontainers, dùng DB compose `leave_management_test`. Nếu muốn lại Testcontainers, chạy tests từ host (cần Java 21 local) hoặc dùng `tecnativa/docker-socket-proxy` sidecar.
- **Spring Data Auditing với `OffsetDateTime`** cần `DateTimeProvider` bean tường minh — default chỉ trả `LocalDateTime`/`Instant`. Xem `JpaAuditingConfig#auditingDateTimeProvider`.
- **Gradle 9.x** yêu cầu `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` tường minh, khác Gradle 8.
- **TestRestTemplate + JDK HttpURLConnection** ném `HttpRetryException: cannot retry due to server authentication, in streaming mode` khi nhận 401 với body POST. Đã thêm `org.apache.httpcomponents.client5:httpclient5` vào `testImplementation` để Spring Boot auto-detect và dùng HttpClient5.
- **Docker Desktop tự khởi động lại dev backend** (`restart: unless-stopped`) sau khi daemon restart → container `leave-backend` chạy `bootRun` giữ lock `/app/.gradle`. Trước khi chạy `docker compose run ... gradlew test` phải `docker compose stop backend`. Nếu gặp "Timeout waiting to lock Build Output Cleanup Cache" hoặc "Cannot create directory .gradle": `docker rm -f` các container `*backend-run*` còn sót và stop dev backend.
- **@WebMvcTest với `@PreAuthorize` SpEL `#id == principal.id`**: request-post-processor auth (`SecurityMockMvcRequestPostProcessors.authentication`) KHÔNG nạp được vào `SecurityContext` khi `addFilters=false` (cần filter chain). Dùng annotation `@WithMockPrincipal` (kiểu `@WithSecurityContext`, xem `src/test/.../config/`) để inject `UserPrincipal` có id cụ thể qua TestExecutionListener. `@WithMockUser` thường chỉ cho role, không cho principal id.
- **Test `@Transactional` + raw JdbcTemplate UPDATE rồi service đọc lại qua JPA**: Hibernate chưa flush INSERT khi JDBC chạy (UPDATE 0 row) và first-level cache giữ entity cũ. Phải `em.flush()` trước JDBC update và `em.clear()` sau để service re-read thấy state mới.
- **CORS preflight `OPTIONS` bị Spring Security trả 401** nếu chỉ đăng ký standalone `CorsFilter` bean (chạy sau security chain). Trình duyệt block mọi request có header tùy chỉnh (`Authorization`) → FE login fail dù backend curl OK. Phải expose `CorsConfigurationSource` bean + `http.cors(Customizer.withDefaults())` trong `SecurityConfig`. `MockMvc`/`TestRestTemplate` KHÔNG mô phỏng preflight → loại lỗi này chỉ lộ khi test trên browser.
- **JPQL `(:param IS NULL OR …)` với param `LocalDate` null** → Postgres ném `could not determine data type of parameter` (bind null không kèm kiểu). Pattern này OK với `Long`/enum/`String` nhưng vỡ với `LocalDate`. Cách xử lý: dùng derived query (`findByUserIdAndStartDateBetween…`) với cận cụ thể (`LocalDate.of(1900,1,1)`/`(9999,12,31)`) và lọc các filter optional khác (vd status) ở tầng service. Xem `LeaveRequestRepository`.
- **@WebMvcTest + SpEL bean reference `@beanName.method(...)` trong `@PreAuthorize`**: bean được `@Component("beanName")` KHÔNG được scan trong slice. Phải `@MockBean(name = "beanName")` (đúng tên) — nếu chỉ `@MockBean` theo type, tên bean sinh tự động không khớp `@beanName` → SpEL ném exception → request trả **500** (không phải 403). Xem `LeaveRequestControllerTest`.
- **E2E `@SpringBootTest(RANDOM_PORT)` commit thật (không `@Transactional` rollback)**. Mọi E2E test phải tự dọn bảng trong `@BeforeEach` theo thứ tự FK-safe (con trước cha). Bảng mới `leave_requests`/`approval_actions` tham chiếu `users` → nếu để lại rows đã commit, `DELETE FROM users` của E2E khác sẽ vỡ FK → cả lớp đó fail. `LeaveRequestE2ETest` thêm `@AfterEach` dọn sạch để không "đầu độc" suite khác. **Lưu ý**: rows commit từ một lần chạy test đơn lẻ (trước khi có cleanup) còn nằm lại trong `leave_management_test` giữa các lần chạy → nếu gặp lỗi FK lạ khi chạy full suite, dọn thủ công: `docker compose exec postgres psql -U leave_admin -d leave_management_test -c "DELETE FROM approval_actions; DELETE FROM leave_requests; DELETE FROM leave_balances; DELETE FROM audit_log; DELETE FROM refresh_tokens; DELETE FROM users;"`.
- **Build prod image ghi đè image dev cùng tên**: `docker compose -f docker-compose.prod.yml build backend` tạo image `leave-management-system-backend:latest` (ENTRYPOINT `java -jar /app/app.jar`) — **cùng tên** với image dev (service `backend` của `docker-compose.yml`). Sau đó `docker compose run --rm backend ./gradlew test` dùng nhầm image prod → entrypoint nuốt args, lỗi `Unable to access jarfile /app/app.jar` (volume `./backend:/app` đã che mất jar). Khắc phục: `docker compose build backend` (rebuild image dev từ `Dockerfile.dev`) trước khi chạy test.
