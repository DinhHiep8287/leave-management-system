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
| §12 | i18n (chừa chỗ tiếng Anh) | Chuỗi tiếng Việt hardcode | Chưa có khung i18n |
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

## Milestone v1.2 — Hoàn chỉnh đơn, lịch, dashboard, báo cáo

10. **Sửa đơn PENDING** (§5.5): BE `PUT /leave-requests/{id}` (chỉ người tạo, chỉ PENDING,
    tính lại số ngày + validate như tạo mới) + FE.
11. **Calendar đầy đủ** (§6): thêm **view tuần**; filter **loại nghỉ** + **theo người** (HR/Admin);
    sửa scope **employee = cả phòng ban**; tùy chọn ẩn/hiện đơn PENDING.
12. **Dashboard theo vai trò** (§10.3): widget HR/Admin (tổng nhân viên, đơn theo trạng thái,
    top phòng ban nghỉ nhiều/ít trong tháng) + quick links quản trị.
13. **Báo cáo nâng cao** (§11): thêm lọc **phòng ban** cho CSV đơn nghỉ; báo cáo **tổng ngày
    nghỉ theo loại theo tháng/quý** (endpoint + FE + CSV).

## Milestone v1.3 — Chất lượng & vận hành

14. **E2E frontend (Playwright)**: smoke login→nộp→duyệt, dùng skill `webapp-testing`.
15. **i18n scaffolding** (§12): tách chuỗi, khung `vi` mặc định + `en` để mở rộng.
16. **Bỏ N+1** ở list/inbox (`LeaveRequestService.toResponse`), security headers nginx.
17. **Deploy thật** (Railway/Fly.io + Neon) + observability (actuator metrics, JSON logging).

---

## v2 — Tính năng hoãn lại (REQUIREMENTS §13)

Chỉ làm khi có nhu cầu rõ, mỗi mục là một quyết định riêng:

- Thông báo email / in-app (chuông) khi có đơn cần duyệt / được duyệt.
- Workflow duyệt **nhiều cấp**.
- Carry over phép sang năm sau.
- Upload file (giấy bác sĩ…).
- Self-register · Quên mật khẩu tự reset qua email.
- Phân quyền chi tiết (permission-level thay cho RBAC đơn giản).

## Không làm — kể cả v2 (REQUIREMENTS §14)

- ❌ Tích hợp Google Calendar / Outlook (2-way sync).
- ❌ Multi-tenant / multi-company.
- ❌ SSO (Google Workspace / Microsoft 365).

## Nguyên tắc xuyên suốt

- Giữ ranh giới MVP: **không** thêm Kafka/Redis/microservices/CQRS (CLAUDE.md).
- Mỗi tính năng: BE (test) → FE (typecheck/lint/build + smoke) → commit theo Conventional Commits.
- Bám `docs/UI-GUIDELINES.md` cho mọi màn hình mới.
- Đổi spec/kiến trúc → cập nhật `REQUIREMENTS.md`/`ARCHITECTURE.md` tương ứng.
