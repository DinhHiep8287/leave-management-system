# Changelog

Tất cả thay đổi đáng chú ý của dự án sẽ được ghi lại ở đây.

Định dạng dựa trên [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
và dự án tuân theo [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — Week 2 Part 5: LeaveBalance CRUD
- `leavebalance/` package: `LeaveBalanceEntity` (unique `(user_id, leave_type_id, year)`, `remaining() = total + adjusted - used`), `LeaveBalanceRepository`, `LeaveBalanceService`, `LeaveBalanceController`.
- Endpoints: `GET /api/users/{id}/leave-balances?year=` (self | HR | ADMIN qua SpEL), `POST /api/leave-balances` (ADMIN upsert quota — giữ nguyên used/adjusted), `POST /api/leave-balances/initialize?year=` (ADMIN bulk init, idempotent), `PATCH /api/leave-balances/{id}/adjust` (HR | ADMIN).
- `bulkInitializeYear`: tạo row cho mỗi active user × active leave type có `requiresBalance=true`, dùng `default_quota_days` làm `total_days`; bỏ qua row đã tồn tại.
- `adjust`: cộng/trừ `adjusted_days`, chặn nếu remaining < 0, ghi `audit_log` (action `LEAVE_BALANCE_ADJUST`, old/new JSONB) qua `AuditLogWriter` (JdbcTemplate + `CAST(? AS jsonb)`).
- Tests: `LeaveBalanceRepositoryTest` (4), `LeaveBalanceServiceTest` (3 — bulk idempotent, adjust+audit, upsert giữ used/adjusted), `LeaveBalanceControllerTest` `@WebMvcTest` (7 — RBAC self vs HR/ADMIN), `LeaveBalanceE2ETest` (4 — bulk init→adjust→remaining + audit, RBAC, negative guard). 18 mới, tổng 78.

### Added — Week 2 Part 4: LeaveType CRUD
- `leavetype/` package: `LeaveTypeEntity` (`default_quota_days` NUMERIC(5,1)), `LeaveTypeRepository` (unique code, active/requiresBalance filters), `LeaveTypeService`, `LeaveTypeController` `/api/leave-types`.
- Endpoints: `POST`/`PUT /{id}`/`DELETE /{id}` (ADMIN), `GET /{id}` + `GET` (mọi user authenticated). `DELETE` mặc định soft-delete; `?hard=true` xoá cứng nhưng 409 nếu còn `leave_balances`/`leave_requests` reference.
- Validation: code regex + uppercase normalize + duplicate 409, `defaultQuotaDays` ∈ [0,366] với `@Digits(3,1)` và phải là bội số của 0.5.
- Tests: `LeaveTypeRepositoryTest` (3), `LeaveTypeControllerTest` `@WebMvcTest` (6), `LeaveTypeE2ETest` (4). 13 mới, tổng 60.

### Added — Week 2 Part 3: User CRUD
- `user/web/`: `UserCreateRequest`, `UserUpdateRequest`, `UpdateMeRequest`, `ChangePasswordRequest`, `ResetPasswordRequest`, `UserResponse` (không bao giờ trả `passwordHash`).
- `UserService`: `create` (ADMIN — hash password BCrypt), `update` (ADMIN full update), `updateSelf` (chỉ `fullName`), `changePassword` (self, verify old), `resetPassword` (ADMIN), `setActive` (ADMIN), `findById`, `list` (HR/ADMIN, filter by `q`/department/role/active).
- `UserController` `/api/users`: `POST` (ADMIN), `GET /{id}` (ADMIN/HR or self qua SpEL), `PUT /{id}` (ADMIN), `POST /{id}/reset-password` (ADMIN), `POST /{id}/activate|deactivate` (ADMIN), `GET` (ADMIN/HR list), `GET /me`, `PATCH /me`, `POST /me/password`.
- Khi đổi/reset password hoặc deactivate user → revoke toàn bộ refresh token của user đó (force re-login mọi device).
- `UserRepository.search` với JPQL filter theo `q`/department/role/active.
- Tests: `UserRepositoryTest` (5), `UserControllerTest` `@WebMvcTest` (8), `UserE2ETest` (4 scenarios — admin tạo→user login→deactivate→401, duplicate email→409, admin update role, change password revoke refresh). 17 mới, tổng 47.

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
