# Changelog

Tất cả thay đổi đáng chú ý của dự án sẽ được ghi lại ở đây.

Định dạng dựa trên [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
và dự án tuân theo [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — Week 2 Part 2: Department CRUD
- `department/` package: `DepartmentEntity`, `DepartmentRepository` (unique code, case-insensitive search by code/name), `DepartmentService` (create/update/softDelete/findById/list), `DepartmentController` `/api/departments` với RBAC (`@PreAuthorize` ADMIN cho write).
- Code normalization uppercase + duplicate check → 409 CONFLICT.
- Validation: code regex `[A-Za-z0-9_-]+`, name 1..200.
- Pagination support qua `Pageable` + meta `{page,size,totalElements,totalPages}`.
- Tests: `DepartmentRepositoryTest` (3), `DepartmentControllerTest` `@WebMvcTest` (6) — dùng `MethodSecurityTestConfig` để bật `@EnableMethodSecurity` trong slice, `DepartmentE2ETest` (4) — full lifecycle + RBAC. 13 tests xanh, tổng 30.

### Added — Week 2 Part 1: JWT Auth foundation
- `auth/` package: `JwtService` (HS256, access 15m + refresh 7d, SHA-256 store hash), `AuthService` (login/refresh/logout với rotation), `JpaUserDetailsService`, `JwtAuthenticationFilter`, `UserPrincipal`, `RefreshTokenEntity`/repo, `AuthController` (`/auth/login`, `/refresh`, `/logout`, `/me`).
- `user/` package: `UserEntity` (đầy đủ field theo schema), `Role` enum, `UserRepository`.
- `SecurityConfig` wire JWT filter + `@EnableMethodSecurity` + JSON error response cho 401/403; `JpaAuditingConfig` đọc principal từ `SecurityContext` + `DateTimeProvider` cho `OffsetDateTime`.
- Migration `V2__seed_reference_data.sql`: 3 departments, 4 leave types, 10 VN public holidays 2026.
- `DemoDataInitializer` (profile=dev): seed 1 ADMIN + 1 HR + 2 MANAGER + 5 EMPLOYEE với password BCrypt(12) — không commit hash vào SQL.
- Frontend `features/auth/`: `AuthContext`, `LoginPage` (RHF + Zod), `ProtectedRoute`, axios interceptor có auto-refresh single-flight + 401 handler, in-memory access token + localStorage refresh token.
- Tests: `JwtServiceTest` (unit), `AuthControllerTest` (@WebMvcTest), `AuthFlowE2ETest` (@SpringBootTest, 4 scenarios). 17 tests xanh.

### Changed
- `build.gradle.kts`: thêm `httpclient5` (test), `junit-platform-launcher` (test), `Test` task có `maxHeapSize=1g` + override datasource sang `leave_management_test` qua `systemProperty`.
- `application-test.yml` trỏ tới DB test riêng.
- Tài liệu gotchas trong `CLAUDE.md` cập nhật: test workflow (stop dev backend + `compose run`), tạo DB test, Testcontainers không hoạt động trong container Windows.

## [0.1.0] - 2026-05-26 — Week 1: Foundation

### Added
- Tài liệu nền tảng: REQUIREMENTS, ARCHITECTURE, DATABASE, DEVELOPMENT.
- File cấu hình môi trường: `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`.
- Hướng dẫn cho Claude Code: `CLAUDE.md`.
- 4 agent skill cài project-local trong `.agents/skills/`.
- Allowlist `.claude/settings.json` cho lệnh `docker compose` read-only.
- Docker Compose dev stack (postgres + backend + frontend) với `.dockerignore`.
- Spring Boot 3.3 backend skeleton (`com.peih68.leave`) với `BaseEntity`, `GlobalExceptionHandler`, `ApiResponse` wrapper, `/api/health` endpoint, `SecurityConfig` stub, `CorsConfig`, `OpenApiConfig`, `JpaAuditingConfig`.
- Flyway V1 migration tạo baseline schema 9 bảng.
- Context-load test với Testcontainers Postgres.
- React + Vite + TypeScript + Tailwind + shadcn/ui frontend skeleton với trang health check.

### Notes
- Dev container backend chạy as root (image default) — workaround cho permission của named volume khi mount gradle cache. Xem `CLAUDE.md` mục "Lưu ý vận hành".
- Pull Docker Hub từ một số ISP Việt Nam có thể fail `httpReadSeeker: EOF`. Workaround: `mirror.gcr.io`. Xem `docs/DEVELOPMENT.md §12`.
