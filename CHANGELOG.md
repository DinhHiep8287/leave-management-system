# Changelog

Tất cả thay đổi đáng chú ý của dự án sẽ được ghi lại ở đây.

Định dạng dựa trên [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
và dự án tuân theo [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — v1.1.0: Hoàn thiện kiểm thử & CI
- **FE unit tests (Vitest + Testing Library)**: 24 test cho `formatDate`/`formatDateTime`, `apiErrorMessage`, `cn`, Zod schema đơn nghỉ (tách `features/leave-requests/schema.ts` dùng chung cho submit + edit — trước đó trùng lặp 2 nơi), `ErrorState` (render + retry). Script `pnpm test`; cấu hình trong `vite.config.ts` (jsdom + globals).
- **CI**: job frontend thêm bước `pnpm test`; **job `e2e` mới** dựng full stack bằng `docker compose up -d --build` (seeder dev đổ dữ liệu vào DB trống của CI), poll `/api/health` + `:5173`, chạy `e2e/run_smoke.py` (Playwright/chromium); khi fail dump logs + upload screenshots làm artifact.
- **README badges**: CI status + License MIT.

## [1.0.0] - 2026-06-11 — MVP hoàn chỉnh

Mốc MVP: auth JWT, CRUD đầy đủ, quy trình đơn nghỉ + duyệt, lịch tháng/tuần, dashboard theo
vai trò, báo cáo CSV, console quản trị, cấu hình production, E2E Playwright, seed demo.
Gồm toàn bộ các đợt v1.0.x → v1.3 (lịch sử bên dưới). CI xanh tại thời điểm tag.

### Removed — Loại i18n vĩnh viễn khỏi phạm vi
- **i18n vi/en bị loại bỏ hoàn toàn** (kể cả tương lai): UI tiếng Việt duy nhất theo quyết định của chủ dự án (06/2026). REQUIREMENTS §12 sửa lại, i18n chuyển vào §14 (loại trừ vĩnh viễn); ROADMAP gỡ bản v1.2.0-i18n và đánh số lại (deploy → v1.2.0). Hệ quả: MVP đáp ứng **toàn bộ** REQUIREMENTS, không còn ngoại lệ.

### Added — Chuẩn bị mốc v1.0.0
- Controller-slice test cho `GET /departments/mine` + `GET /users/me` (lấp 2 khoảng trống `@WebMvcTest` cuối).
- `e2e/run_smoke.py` tự **xóa cứng** đơn smoke đã hủy sau mỗi lần chạy (best-effort qua psql) — DB dev không tích rác.
- `docs/ROADMAP.md`: kế hoạch chi tiết các bản nâng cấp **v1.1.0 (test/CI) → v1.2.0 (deploy thật) → v2.0.0 (notification/email/carry-over/attachment)** kèm quy ước semver-tag và nguyên tắc docs bắt buộc.

### Added — Seed dữ liệu demo tự nhiên (profile dev)
- **`DemoLeaveSeeder`** (mới, gọi từ `DemoDataInitializer`, chỉ chạy khi DB trống): seed 10 nhân viên tên tiếng Việt (ENG +4, SALES +4, HR +2 — tổng 19 user) + quỹ phép năm hiện tại & năm trước + **~63 đơn nghỉ** trải 5 tháng qua tới +30 ngày tới (đủ APPROVED/PENDING/REJECTED/CANCELLED, nửa ngày, lý do tiếng Việt) + 2 điều chỉnh quỹ có audit.
- **Bất biến được giữ như luồng thật**: `total_days` tính bằng `LeaveDayCalculator` + holidays thật; `used_days` đi qua `LeaveBalanceService.applyUsedDelta` (không bao giờ âm); mỗi user một tuần-slot riêng (không chồng lấn); `approval_actions` đúng state machine §5.4. Ngày **tương đối theo hôm nay** nên dữ liệu không bao giờ cũ; mỗi phòng có 1 người đang nghỉ hôm nay.
- Đơn seed tương lai ≤ +30 ngày để không phá Playwright smoke (smoke nộp đơn ở +80 ngày và bấm nút Hủy đầu tiên). `e2e/run_smoke.py`: `shot()` chờ 1.2s trước khi chụp (networkidle không tin cậy với Vite HMR websocket).
- Reset để seed lại: `docker compose down -v && docker compose up` (nhớ tạo lại DB test: `docker compose exec postgres createdb -U leave_admin leave_management_test`).

### Added — Hoàn chỉnh MVP (đóng nốt khoảng hở REQUIREMENTS)
- **§10.2** dashboard thêm **"Đang nghỉ tuần này"** (`onLeaveThisWeekCount` — đếm distinct người nghỉ trong tuần ISO theo phạm vi người xem, bổ sung cho "hôm nay"). BE `DashboardService` + FE stat card. *(Đây là khoảng hở thật duy nhất còn lại.)*
- **§5.3** (defense-in-depth): API đã chặn `start_date` quá khứ sẵn qua `@FutureOrPresent` trên DTO; thêm guard `start_date >= today` ở tầng service `submit`/`update` để chặn cả khi gọi service trực tiếp (bỏ qua bean validation) + test `submitWithPastStartDateIsRejected`.
- Đến đây **toàn bộ REQUIREMENTS MVP đã đáp ứng, trừ i18n (§12)** (hoãn v2).

### Added — Phòng ban của tôi (danh bạ thành viên)
- **BE** `GET /departments/mine` (self-scoped theo principal, không nhận id → không lộ phòng khác): trả phòng ban của người gọi + danh sách thành viên đang hoạt động (head xếp đầu, rồi theo tên), kèm `headName`. DTO `MyDepartmentResponse`/`DepartmentMemberResponse`; tái dùng `UserRepository.findByDepartmentIdAndIsActiveTrue`. Test E2E: `DepartmentE2ETest` khẳng định thấy thành viên cùng phòng + **không** lẫn người phòng SALES.
- **FE** màn **"Phòng ban của tôi"** (`/my-department`, nav cho mọi vai trò): tên phòng + trưởng phòng + bảng thành viên (badge "Trưởng phòng"). Đáp ứng REQUIREMENTS §6 ở mức danh bạ phòng ban cho emp/manager.

### Added — Hồ sơ: phòng ban + quản lý của tôi
- `GET /users/me` (và `PATCH /users/me`) trả thêm `departmentName` + `managerName` (DTO mới `MeResponse` — resolve tên từ id, không đụng `UserResponse` dùng chung cho list/CRUD nên không gây N+1). FE trang **Hồ sơ cá nhân** hiển thị Phòng ban + Quản lý (read-only). Lý do: emp/manager trước đây không thấy phòng ban của mình vì `/me` chỉ trả `departmentId` (số) và profile không render. Xem `docs/REQUIREMENTS.md §9`.

### Changed — v1.3: Quality & polish (no deploy)
- **Bỏ N+1**: `LeaveRequestService` list/inbox/history gom truy vấn tên user + mã loại nghỉ vào map (thay findById từng dòng). Hành vi không đổi.
- **FE robustness**: `ErrorBoundary` cấp app + `ErrorState` (có nút thử lại) cho các trang dùng query (dashboard/đơn của tôi/cần duyệt/người dùng/lịch).
- **Hardening (config-only)**: nginx security headers (CSP/X-Frame-Options/…); actuator expose `metrics` ở prod; JSON logging (logback-spring.xml + logstash encoder) ở profile prod.
- **E2E Playwright** (`e2e/run_smoke.py`): smoke toàn bộ luồng theo vai trò (employee/manager/HR/admin) + dark mode, sinh screenshots cho README.
- **Docs**: README có ảnh minh họa; CHANGELOG/ROADMAP cập nhật. 177 backend tests.

### Added — v1.2: Requests, calendar, dashboard & reports completion
- **Sửa đơn PENDING** (§5.5): `PUT /leave-requests/{id}` (người tạo, chỉ PENDING, tính lại ngày + validate) + dialog sửa ở FE.
- **Calendar** (§6): employee xem cả phòng ban (manager: phòng ban + cấp dưới); filter loại nghỉ/phòng ban/theo người (HR/ADMIN); APPROVED-only mặc định + tùy chọn kèm pending. FE thêm **view tuần** + bộ lọc.
- **Dashboard HR/ADMIN** (§10.3): `GET /dashboard/admin-summary` (tổng nhân viên, đơn theo trạng thái, top phòng ban tháng này) + khu "toàn tổ chức" trên FE (chart + quick links).
- **Báo cáo** (§11): lọc phòng ban cho CSV đơn nghỉ; `GET /reports/leave-summary.csv?year=&groupBy=month|quarter` (tổng ngày duyệt theo loại theo tháng/quý). 177 backend tests.

### Added — v1.1: Admin/HR console (frontend)
- Quản lý **người dùng** (`/admin/users`, HR/ADMIN): tìm kiếm/lọc (vai trò/phòng ban/trạng thái), phân trang; ADMIN tạo/sửa, đặt lại mật khẩu, kích hoạt/khóa.
- Quản lý **phòng ban** + **loại nghỉ phép** (`/admin/...`, ADMIN): CRUD + soft delete; sửa loại nghỉ refresh luôn dropdown nộp đơn.
- Quản lý **quỹ phép** (`/admin/balances`): khởi tạo năm (ADMIN), điều chỉnh có lý do (HR/ADMIN); **ngày lễ** (`/admin/holidays`, HR/ADMIN) CRUD đồng bộ shading lịch.
- **Hồ sơ cá nhân** (`/profile`): sửa họ tên + đổi mật khẩu (mọi user).

### Added — v1.1 Part 1: Holiday CRUD (backend)
- `POST/PUT/DELETE /holidays` (ADMIN/HR) — REQUIREMENTS §7; unique-date 409, missing 404. `HolidayRequest` DTO + `existsByHolidayDate`. Tests: HolidayServiceTest (6), HolidayControllerTest (5). 167 backend tests.

### Fixed — v1.0.x: CI + spec alignment
- **CI**: `.github/workflows/ci.yml` (backend `gradle test` + Postgres service, frontend typecheck/lint/build). Sửa lỗi backend CI: `gradle-wrapper.jar` bị `.gitignore *.jar` loại → commit jar + negation `!**/gradle/wrapper/gradle-wrapper.jar`. Ép `TZ=Asia/Ho_Chi_Minh` cho test. E2E cleanup FK-safe (`E2ECleanup.wipeUsersFkSafe`) độc lập thứ tự chạy.
- **§3** seed `SICK` quota 3 → 30 (migration V3).
- **§5.5** người tạo hủy được đơn `APPROVED` khi chưa tới `start_date` (hoàn balance); sau ngày bắt đầu chỉ manager/HR/ADMIN.

### Added — Week 4 Part 7: Production config + deploy guide
- `application-prod.yml`: Hikari pool lớn hơn, graceful shutdown, tắt Swagger UI, expose actuator `health,info` (probes), `server.error.include-message: never`, log INFO.
- `docker-compose.prod.yml`: stack prod dùng image build sẵn (jar + nginx), backend không publish cổng (qua nginx `/api`), secret bắt buộc (`JWT_SECRET`/`POSTGRES_PASSWORD` fail-fast). `frontend/Dockerfile` nhận build-arg `VITE_API_BASE_URL` (mặc định `/api`); `nginx.conf` proxy `/api` → backend (cùng origin). `.env.prod.example`.
- `docs/DEPLOYMENT.md`: hướng dẫn build/run prod compose, sinh `JWT_SECRET`, tạo admin đầu tiên (prod không seed), health check, vận hành, gợi ý deploy tách dịch vụ (Neon/Railway/Fly.io/Vercel) — chỉ hướng dẫn.
- Polish: sửa script `pnpm typecheck` (`tsc -p tsconfig.json --noEmit`, hết lỗi TS6310 do `-b --noEmit`).

### Added — Week 4 Part 6: Dashboard (charts) + CSV downloads (frontend)
- `features/dashboard`: trang tổng quan thật — stat cards (chờ duyệt / đang nghỉ hôm nay / đơn chờ của tôi), biểu đồ cột recharts (quỹ phép đã dùng/còn lại theo loại), danh sách đang nghỉ hôm nay. `GET /dashboard/summary`.
- `features/reports` (HR/ADMIN): tải CSV đơn nghỉ + quỹ phép qua blob + object URL (kèm Authorization), route `/reports` gated theo role.

### Added — Week 4 Part 5: Team leave calendar (frontend)
- `features/calendar`: lưới tháng tự dựng bằng date-fns, hiển thị đơn đã duyệt/chờ duyệt trong phạm vi, tô cuối tuần/ngày lễ, điều hướng tháng, lọc phòng ban cho HR/ADMIN; click mở dialog chi tiết. Thêm `date-fns`, `recharts`.

### Added — Week 4 Part 3-4: Frontend leave-request + approval UI
- Nền tảng FE: app shell nav role-aware text-led, font Be Vietnam Pro, theme teal (light+dark), UI primitives kiểu shadcn (input/label/textarea/select/card/badge/table/dialog) + sonner toast. Theo `docs/UI-GUIDELINES.md`.
- `features/leave-requests`: form nộp đơn (RHF+Zod), "Đơn của tôi" (lọc năm/trạng thái, hủy đơn PENDING), dialog chi tiết + timeline lịch sử (dùng chung).
- `features/approvals`: inbox phân trang theo scope, dialog duyệt (ghi chú tùy chọn) / từ chối (bắt buộc lý do).

### Added — Week 4 Part 2: CSV report exports (backend)
- `report/`: `CsvWriter` (RFC-4180, BOM UTF-8 cho Excel tiếng Việt), `ReportService` (đơn nghỉ overlap kỳ + quỹ phép theo năm), `ReportController` `GET /api/reports/leave-requests.csv`, `/leave-balances.csv` (HR/ADMIN, `text/csv` + attachment). `LeaveBalanceRepository.findByYearOrderByUserIdAscLeaveTypeIdAsc`.
- Tests: `CsvWriterTest` (6), `ReportServiceTest` (3), `ReportControllerTest` (4). 13 mới, tổng 155.

### Added — Week 4 Part 1: Team calendar + dashboard endpoints (backend)
- `leaverequest`: `LeaveCalendarService` + `GET /api/calendar?from=&to=&departmentId=` (scope theo role: employee=mình, manager=reports+mình, HR/ADMIN=tất cả hoặc 1 phòng ban; giới hạn ≤92 ngày). Repo: `findOverlappingForUsers`/`findOverlapping` (JPQL param non-null) + count queries. `UserRepository.findByDepartmentIdAndIsActiveTrue`/`findByManagerIdAndIsActiveTrue`.
- `dashboard`: `DashboardService` + `GET /api/dashboard/summary` (pendingApprovalCount theo role, onLeaveToday, myPendingCount, myBalances).
- Tests: `LeaveCalendarServiceTest` (6), `DashboardServiceTest` (3), `LeaveCalendarControllerTest` (1), `DashboardControllerTest` (1). 11 mới, tổng 142.

### Added — Week 3 Part 4: End-to-end leave-request flow + docs
- `LeaveRequestE2ETest` (`@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`, JWT thật cho admin/manager1/manager2/employee): golden path (submit ANNUAL 5 ngày → inbox manager → approve → `used_days`/`remaining` đúng → history CREATED+APPROVED), reject không trừ balance, cancel-approved hoàn balance, manager khác team → 403, employee tự duyệt → 403, approve khi balance bị HR rút bớt → 409 `INSUFFICIENT_BALANCE`. 6 mới, tổng 131.
- `CLAUDE.md`: lộ trình Tuần 3 ✅; thêm gotchas — JPQL `:date IS NULL` (Postgres không suy được kiểu), `@WebMvcTest` cần `@MockBean(name=...)` cho SpEL bean reference (sai tên → 500), E2E commit thật cần `@AfterEach` dọn `leave_requests`/`approval_actions` để không vỡ FK `DELETE FROM users` của suite khác.

### Added — Week 3 Part 3: Approval workflow (approve / reject / cancel)
- `LeaveRequestService`: `approve` (PENDING→APPROVED, hard-check + trừ `used_days`), `reject` (PENDING→REJECTED, comment bắt buộc, không đụng balance), `cancel` (requester hủy đơn PENDING; manager/HR/ADMIN hủy đơn APPROVED → hoàn `used_days`; đơn terminal → `CONFLICT`), `history`. State machine `transition()` ghi `approval_actions` + `audit_log` (action `LEAVE_REQUEST_APPROVED/REJECTED/CANCELLED`, old/new status JSON) qua `AuditLogWriter`.
- `LeaveBalanceService.applyUsedDelta(userId, leaveTypeId, year, delta)`: cộng/trừ `used_days` với guard không âm và remaining ≥ 0 (`INSUFFICIENT_BALANCE`); tái dùng cho approve (+) và cancel-approved (−).
- `LeaveRequestController`: `POST /api/leave-requests/{id}/approve|reject` (manager team mình | HR/ADMIN), `POST /{id}/cancel` (requester | manager | HR/ADMIN), `GET /{id}/history` (participant | HR/ADMIN). DTOs `ApprovalDecisionRequest`, `ApprovalActionResponse`.
- Tests: `LeaveRequestApprovalServiceTest` (8 — approve trừ balance + audit + 2 action, reject không đụng balance, reject thiếu comment, cancel-approved hoàn balance, requester không hủy được approved, requester hủy pending, transition sai → CONFLICT, hard-check thiếu balance → INSUFFICIENT_BALANCE), `LeaveRequestApprovalControllerTest` `@WebMvcTest` (8 — RBAC approve/reject/cancel/history). 16 mới, tổng 125.

### Added — Week 3 Part 2: LeaveRequest submit + list + detail
- `leaverequest/` package: `LeaveRequestEntity` (map `leave_requests`), `LeaveRequestRepository` (overlap query `existsOverlap`, inbox queries theo manager/status, list theo user + khoảng start_date), DTOs `LeaveRequestCreateRequest`/`LeaveRequestResponse`.
- `ApprovalActionEntity` + `ApprovalAction` enum + `ApprovalActionRepository` — bảng `approval_actions` chỉ có `created_at` nên KHÔNG extend `BaseEntity` (dùng `@CreatedDate` + `AuditingEntityListener`). Tạo sớm ở Part 2 vì `submit` ghi action `CREATED`; Part 3 sẽ thêm APPROVED/REJECTED/CANCELLED.
- `LeaveRequestService.submit`: tính `total_days` qua `LeaveDayCalculator` + holidays trong khoảng; chặn end<start, đơn một-ngày nửa-buổi lệch half, khoảng toàn cuối tuần/lễ (→ `VALIDATION_ERROR`), thiếu manager (→ `VALIDATION_ERROR`), chồng lấn đơn PENDING/APPROVED (→ `CONFLICT`), soft-check số dư nếu `requiresBalance` (→ `INSUFFICIENT_BALANCE`); lưu `status=PENDING` + ghi `approval_actions` CREATED. Thêm `findById`, `listByUser`, `listForApprover` (MANAGER thấy team mình; HR/ADMIN thấy tất cả).
- `LeaveRequestController`: `POST /api/leave-requests` (tự nộp), `GET /api/leave-requests/{id}` (requester|manager|HR/ADMIN qua `@leaveRequestSecurity.isParticipant`), `GET /api/users/{id}/leave-requests?year=&status=` (self|HR/ADMIN), `GET /api/leave-requests?status=` (inbox, paged). `LeaveRequestSecurity` bean cho SpEL (`isRequester`/`isManagerOf`/`isParticipant`).
- `common/exception/ErrorCode`: thêm `INSUFFICIENT_BALANCE` (409).
- Tests: `LeaveRequestRepositoryTest` (4 — overlap detect/ignore-terminal, date window, manager+status), `LeaveRequestControllerTest` `@WebMvcTest` (8 — RBAC submit/detail/list/inbox; `@MockBean(name="leaveRequestSecurity")` để SpEL bean resolve), `LeaveRequestServiceTest` (5 — tính ngày+managerId+CREATED, thiếu manager, chồng lấn, thiếu số dư, toàn cuối tuần). 17 mới, tổng 109.

### Added — Week 3 Part 1: Holiday lookup + working-day calculator
- `holiday/` package: `HolidayEntity` (map `holidays`), `HolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc`, `HolidayService` (`listByYear`, `holidayDatesBetween` — dùng lại cho tính ngày nghỉ), `HolidayController` `GET /api/holidays?year=` (mọi user authenticated; mặc định năm hiện tại).
- `leaverequest/domain/`: enum `LeaveHalf` (FULL_DAY/MORNING/AFTERNOON), `LeaveStatus` (PENDING/APPROVED/REJECTED/CANCELLED) khớp CHECK constraint của `leave_requests`.
- `LeaveDayCalculator` (component thuần, không phụ thuộc Spring/DB): `calculate(start, end, startHalf, endHalf, Set<holidayDates>)` → số ngày tính phí, loại Thứ 7/CN + ngày lễ, hỗ trợ nửa ngày ở biên; trả về bội số 0.5, `0.0` nếu khoảng rơi hết vào cuối tuần/lễ.
- Tests: `LeaveDayCalculatorTest` (12, JUnit thuần — tuần đủ, bắc cầu cuối tuần, trùng lễ, nửa ngày đầu/cuối, một ngày nửa buổi, toàn cuối tuần, biên rơi cuối tuần, end<start), `HolidayRepositoryTest` (2, `@SpringBootTest` — đọc 10 ngày lễ seed 2026). 14 mới, tổng 92.

### Fixed
- CORS preflight (`OPTIONS`) bị Spring Security trả 401 → trình duyệt block các request có header `Authorization` (vd `GET /auth/me`), khiến FE login báo "Đăng nhập thất bại" dù backend OK. Đổi `CorsConfig` expose `CorsConfigurationSource` bean (thay vì standalone `CorsFilter` chạy sau security chain) + bật `http.cors()` trong `SecurityConfig`. `MockMvc`/`TestRestTemplate` không mô phỏng preflight nên 78 test vẫn xanh — chỉ lộ khi chạy trên browser.

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
