# Roadmap phát triển (sau MVP Tuần 1–4)

Tài liệu này vạch lộ trình **sau khi hoàn thành MVP 4 tuần**, dựa trên đối chiếu giữa
[`docs/REQUIREMENTS.md`](REQUIREMENTS.md) và hiện trạng code. Mục tiêu: đưa sản phẩm từ
"đủ chạy demo" lên "đủ dùng & khả tín cho portfolio", **không over-engineer**.

## Hiện trạng (cuối Tuần 4)

- **Backend**: gần đủ spec. Auth/RBAC, User/Department/LeaveType/LeaveBalance CRUD,
  đơn nghỉ + tính ngày + duyệt + audit, calendar/dashboard/CSV endpoints. 155 test xanh.
  Đổi/đặt lại mật khẩu đã có (`POST /users/me/password`, `/users/{id}/reset-password`).
- **Frontend**: chỉ phủ luồng **self-service** (đăng nhập, nộp/xem/hủy đơn, inbox duyệt,
  lịch, dashboard, tải CSV). **Chưa có giao diện quản trị nào** (user/phòng ban/loại nghỉ/
  balance/ngày lễ đều backend-only).
- **Hạ tầng**: prod compose + profile prod + deploy guide sẵn sàng. **Chưa có CI** (dù
  README/ARCHITECTURE có nhắc GitHub Actions). Chưa deploy thật.

## Gap so với REQUIREMENTS.md

| # | Requirement | Hiện trạng | Khoảng hở |
|---|---|---|---|
| §5.5 | Sửa đơn khi `PENDING` | Chỉ có hủy | **Thiếu** edit (BE + FE) |
| §5.5 | Người tạo hủy đơn `APPROVED` nếu **chưa tới `start_date`** | Chỉ manager/HR/ADMIN hủy approved; không kiểm `start_date` | **Lệch** chính sách (chốt khác ở Tuần 3) |
| §6 | Calendar có view **tuần** + filter **loại nghỉ** + **theo người** | Chỉ view tháng, filter phòng ban | **Thiếu** week view + 2 filter |
| §6 | Employee thấy lịch **cả phòng ban mình** | Employee chỉ thấy đơn của chính mình | **Lệch** scope |
| §6 | Calendar chỉ hiện `APPROVED` | Hiện cả `APPROVED` + `PENDING` | **Lệch** (cân nhắc giữ pending là tuỳ chọn) |
| §7 | Admin/HR **nhập/sửa/xóa** ngày lễ | Chỉ `GET /holidays` | **Thiếu** CRUD (BE + FE) |
| §10.3 | Dashboard HR/Admin: headcount, đơn theo trạng thái, top phòng ban | Dashboard chung cho mọi vai trò | **Thiếu** widget HR/Admin |
| §11 | CSV đơn **theo phòng ban**; báo cáo **tổng ngày nghỉ theo loại theo tháng/quý** | CSV theo from/to/status, balance theo năm | **Thiếu** lọc phòng ban + báo cáo tổng hợp |
| §3 | `SICK` quota mặc định **30** | Seed V2 đang để **3** | **Lệch** dữ liệu seed |
| §9 | Đổi mật khẩu / quản lý user | Backend đủ | **Thiếu** UI |
| §12 | ~~i18n (chừa chỗ tiếng Anh)~~ | UI tiếng Việt duy nhất | **Đã loại vĩnh viễn khỏi phạm vi** (REQUIREMENTS §14) |
| — | "GitHub Actions" (README/ARCHITECTURE) | Chưa có `.github/workflows` | **Thiếu** CI |

---

## Milestone v1.0.x — Quick wins ✅ **Done**

Sửa lệch nhỏ, rủi ro thấp, làm trước:

1. **Fix seed `SICK` quota** 3 → 30 (§3): migration mới `V*__fix_sick_quota.sql`
   (không sửa migration đã apply).
2. **Chốt lại chính sách hủy** (§5.5): cho người tạo hủy đơn `APPROVED` **nếu `today < start_date`**
   (hoàn balance), giữ quyền manager/HR/ADMIN. Thêm guard `start_date` + test.
3. **CI GitHub Actions** (đóng sai lệch tài liệu↔thực tế): workflow PR chạy
   `gradle test` (Postgres service container) + FE `typecheck/lint/build`.

## Milestone v1.1 — Admin/HR console ✅ **Done**

Giao diện quản trị cho backend đã có sẵn — biến sản phẩm thành "dùng được thật":

4. **FE Quản lý người dùng** (§2, §9): danh sách + tìm kiếm/lọc, tạo/sửa, kích hoạt/khóa,
   đặt lại mật khẩu (ADMIN). Gán role/phòng ban/manager.
5. **FE Quản lý phòng ban** (§8): CRUD, gán trưởng phòng.
6. **FE Quản lý loại nghỉ phép** (§3): CRUD + bật/tắt `is_active`, `requires_balance`.
7. **FE Quản lý balance** (§4): khởi tạo balance đầu năm (`POST /leave-balances/initialize`),
   điều chỉnh thủ công có lý do.
8. **Ngày lễ (§7)**: **BE thêm CRUD** `POST/PUT/DELETE /holidays` (ADMIN/HR) + **FE** quản lý.
9. **FE Đổi mật khẩu / trang hồ sơ cá nhân** (§9).

## Milestone v1.2 — Hoàn chỉnh đơn, lịch, dashboard, báo cáo ✅ **Done**

10. **Sửa đơn PENDING** (§5.5): BE `PUT /leave-requests/{id}` (chỉ người tạo, chỉ PENDING,
    tính lại số ngày + validate như tạo mới) + FE.
11. **Calendar đầy đủ** (§6): thêm **view tuần**; filter **loại nghỉ** + **theo người** (HR/Admin);
    sửa scope **employee = cả phòng ban**; tùy chọn ẩn/hiện đơn PENDING.
12. **Dashboard theo vai trò** (§10.3): widget HR/Admin (tổng nhân viên, đơn theo trạng thái,
    top phòng ban nghỉ nhiều/ít trong tháng) + quick links quản trị.
13. **Báo cáo nâng cao** (§11): thêm lọc **phòng ban** cho CSV đơn nghỉ; báo cáo **tổng ngày
    nghỉ theo loại theo tháng/quý** (endpoint + FE + CSV).

## Milestone v1.3 — Chất lượng sản phẩm (no deploy) ✅ **Done**

14. ✅ **E2E frontend (Playwright)**: `e2e/run_smoke.py` phủ luồng theo vai trò + dark mode + screenshots.
15. ✅ **Bỏ N+1** ở list/inbox/history (`LeaveRequestService` batch lookups).
16. ✅ **UX/a11y**: ErrorBoundary + ErrorState (retry) cho các trang query.
17. ✅ **Hardening config (no deploy)**: nginx security headers; actuator metrics + JSON logging (profile prod).
18. ✅ **Docs**: README có ảnh minh họa (`docs/screenshots/`).

## Post-v1.3 — Phòng ban cho emp/manager ✅ **Done**

- **(A) FE Hồ sơ** hiển thị **Phòng ban** + **Quản lý** (read-only) — emp/manager trước đây không thấy phòng ban của mình.
- **(B) BE** `GET/PATCH /users/me` trả thêm `departmentName` + `managerName` (DTO `MeResponse`, resolve tên từ id; `UserResponse` dùng cho list/CRUD giữ nguyên để tránh N+1).
- **(C) Danh bạ phòng ban**: `GET /departments/mine` (self-scoped, không lộ phòng khác) + FE màn **"Phòng ban của tôi"** (nav mọi vai trò) liệt kê thành viên cùng phòng, head xếp đầu.

## MVP hoàn chỉnh ✅ **Done**

- **§10.2** dashboard thêm **"đang nghỉ tuần này"** (đếm người nghỉ trong tuần ISO theo phạm vi của người xem) — bổ sung cho "đang nghỉ hôm nay". *Đây là khoảng hở thật duy nhất còn lại.*
- **§5.3** (defense-in-depth): API vốn đã chặn `start_date` quá khứ qua `@FutureOrPresent` trên DTO; thêm guard tương đương ở tầng service `submit`/`update` + test, để chặn cả khi gọi service trực tiếp.

→ Toàn bộ REQUIREMENTS MVP đã đáp ứng. (i18n ban đầu thuộc §12 đã được **loại vĩnh viễn khỏi
phạm vi** theo quyết định 06/2026 — UI tiếng Việt duy nhất, xem REQUIREMENTS §14.)

## Seed dữ liệu demo tự nhiên ✅ **Done**

- `DemoLeaveSeeder` (profile dev, DB trống mới chạy): 19 user (10 tên tiếng Việt mới), quỹ phép
  2 năm, ~63 đơn nghỉ tương đối theo ngày hiện tại (đủ trạng thái, nửa ngày, mỗi phòng 1 người
  đang nghỉ hôm nay), 2 điều chỉnh quỹ có audit. Bất biến giữ nguyên như luồng thật
  (LeaveDayCalculator, applyUsedDelta, không chồng lấn, state machine §5.4).

---

# Kế hoạch các bản nâng cấp sắp tới

> **Quy ước phiên bản**: các mục "v1.0.x–v1.3" phía trên là **tên đợt làm việc lịch sử**, tất cả
> nằm trong git tag **v1.0.0** (mốc MVP). Từ đây trở đi, mỗi bản nâng cấp dưới đây ứng với
> **một git tag semver thật** (v1.1.0, v1.2.0, …), tag sau khi CI xanh.
>
> **Mốc v1.0.0 đã tag** (2026-06-11, CI xanh) — các bản dưới đây bắt đầu từ đó.

## v1.1.0 — Hoàn thiện kiểm thử & CI ✅ **Done**

1. ✅ **FE unit test (Vitest + Testing Library)**: 24 test — `lib/format`, `lib/api-error`,
   `lib/utils`, Zod schema đơn nghỉ (tách `schema.ts` dùng chung submit/edit), `ErrorState`.
   Script `pnpm test`.
2. ✅ **Playwright vào CI**: job `e2e` dựng full stack `docker compose up -d --build`, poll
   health, chạy `run_smoke.py`; fail → dump logs + artifact screenshots. Seeder dev đổ dữ
   liệu vào DB trống của CI.
3. ✅ **CI**: bước `pnpm test` trong job frontend. (JaCoCo coverage: bỏ qua — chưa cần.)
4. ✅ **README badges**: CI status + License MIT.

## v1.2.0 — Deploy thật + vận hành (~1 tuần)

Mục tiêu: sản phẩm có URL công khai cho portfolio. Guide + prod compose đã sẵn
(`docs/DEPLOYMENT.md`), chỉ còn thực thi.

1. **Chọn 1 phương án** (quyết định riêng khi bắt đầu): VPS đơn chạy `docker-compose.prod.yml`
   (rẻ, đúng guide) HOẶC tách dịch vụ Railway/Fly.io + Neon + Vercel (free-tier).
2. **TLS/HTTPS**: bật `Strict-Transport-Security` đã comment sẵn trong `frontend/nginx.conf`;
   Let's Encrypt (Caddy/certbot) hoặc TLS của platform.
3. **Vận hành tối thiểu**: tạo admin prod đầu tiên (theo guide, prod không seed), backup
   `pg_dump` định kỳ, uptime check (UptimeRobot), xem JSON log + `/actuator/health`.
4. **README**: link demo live + tài khoản demo riêng cho prod (không dùng mật khẩu dev).

**Hoàn thành khi**: URL công khai login được, health OK, backup chạy, README có link.

## v2.0.0 — Tính năng mở rộng (REQUIREMENTS §13) (~3-4 tuần)

Mỗi mục là một quyết định riêng khi bắt đầu — thứ tự đề xuất theo giá trị/độ rủi ro:

1. **In-app notification (chuông)**: bảng `notifications` (migration mới), phát sự kiện khi
   submit/approve/reject/cancel (`@TransactionalEventListener`), `GET /notifications` +
   mark-read; FE chuông + badge số chưa đọc, polling 30s (chưa cần WebSocket).
2. **Email notification**: `spring-boot-starter-mail` + **Mailpit** container cho dev;
   template duyệt/từ chối/cần duyệt; gửi async, không chặn transaction nghiệp vụ.
3. **Carry-over phép** (§13): cột `carried_over_days` + endpoint/job đầu năm chuyển
   `remaining` năm cũ (cap N ngày, N cấu hình); cập nhật `remaining()` + REQUIREMENTS.
4. **Upload file đính kèm** (giấy bác sĩ…): bảng `attachments`, lưu local volume
   (chưa cần S3), giới hạn type/size, endpoint upload/download có RBAC participant.
5. **Duyệt nhiều cấp** (để cuối / có thể sang v2.1): đổi state machine — rủi ro cao nhất,
   chỉ làm khi 1-4 xong và thật sự cần.

**Mỗi feature đi trọn chu trình**: BE + test → FE → bổ sung e2e smoke → docs.

## Nguyên tắc docs cho mọi bản nâng cấp (bắt buộc)

- `CHANGELOG.md`: entry dưới `[Unreleased]` ngay khi làm; khi tag → tách thành `[x.y.z] - ngày`.
- `docs/ROADMAP.md`: đánh dấu ✅ từng mục khi xong.
- `docs/REQUIREMENTS.md`: cập nhật nếu spec đổi (vd carry-over đảo ngược §4).
- `docs/ARCHITECTURE.md` + `docs/DATABASE.md`: cập nhật khi thêm bảng/luồng (notifications, attachments).
- `CLAUDE.md`: thêm gotcha vận hành mới gặp; cập nhật lộ trình.
- Tag semver chỉ đặt khi CI xanh.

## Không làm — kể cả v2 (REQUIREMENTS §14)

- ❌ **i18n vi/en** — UI tiếng Việt duy nhất (quyết định 06/2026, loại vĩnh viễn).
- ❌ Tích hợp Google Calendar / Outlook (2-way sync).
- ❌ Multi-tenant / multi-company.
- ❌ SSO (Google Workspace / Microsoft 365).

## Nguyên tắc xuyên suốt

- Giữ ranh giới MVP: **không** thêm Kafka/Redis/microservices/CQRS (CLAUDE.md).
- Mỗi tính năng: BE (test) → FE (typecheck/lint/build + smoke) → commit theo Conventional Commits.
- Bám `docs/UI-GUIDELINES.md` cho mọi màn hình mới.
- Đổi spec/kiến trúc → cập nhật `REQUIREMENTS.md`/`ARCHITECTURE.md` tương ứng.
