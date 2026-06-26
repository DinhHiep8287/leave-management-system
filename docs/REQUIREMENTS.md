# Yêu cầu nghiệp vụ — Leave Management System

## 1. Tổng quan

Hệ thống quản lý nghỉ phép mô phỏng quy trình phê duyệt đơn nghỉ phép trong doanh nghiệp Việt Nam. Phục vụ 3 nhóm người dùng: **Nhân viên**, **Quản lý trực tiếp**, **Phòng nhân sự (HR)**, và có vai trò **Admin** cho cấu hình hệ thống.

## 2. Vai trò và quyền hạn

| Vai trò | Mô tả | Quyền chính |
|---|---|---|
| **Employee** | Nhân viên thông thường | Tạo/sửa/hủy đơn của mình; xem số ngày phép; xem lịch nghỉ team |
| **Manager** | Trưởng nhóm/phòng | Tất cả quyền Employee + duyệt/từ chối đơn của cấp dưới trực tiếp |
| **HR** | Phòng nhân sự | Xem toàn bộ đơn/lịch/balance; can thiệp override khi cần; quản lý ngày lễ; điều chỉnh balance |
| **Admin** | Quản trị hệ thống | Tất cả quyền HR + quản lý user/role/loại nghỉ phép/cấu hình |

**Nguyên tắc**: mỗi nhân viên có **đúng 1 manager trực tiếp** (`manager_id`). Manager duyệt đơn của tất cả nhân viên trỏ tới mình.

## 3. Loại nghỉ phép (Leave Type)

Cấu hình bởi Admin. Mặc định khởi tạo 4 loại:

| Mã | Tên | Quota mặc định/năm | Trừ vào balance? |
|---|---|---|---|
| `ANNUAL` | Phép năm | 12 ngày | Có |
| `SICK` | Nghỉ ốm | 30 ngày | Có |
| `UNPAID` | Nghỉ không lương | Không giới hạn | Không |
| `PERSONAL` | Việc riêng | 3 ngày | Có |

Mỗi loại có cấu hình:
- `code`, `name`, `description`
- `default_quota_days` (mặc định khi khởi tạo balance đầu năm)
- `requires_balance` (true → trừ balance, false → không giới hạn)
- `is_active`

## 4. Số ngày phép (Leave Balance)

- Tính theo **năm dương lịch** (1/1 — 31/12).
- **Carry-over** (từ v2.0.0): phép còn dư của năm cũ được chuyển sang năm mới, **tối đa `cap` ngày**
  (mặc định 5, ADMIN nhập khi chạy). Thao tác thủ công bởi ADMIN đầu năm (idempotent — chạy lại
  không cộng dồn). `remaining = total + adjusted + carried_over - used`.
- Admin có thể điều chỉnh thủ công.
- Khi đơn được **APPROVED** → trừ vào balance. Khi đơn bị **CANCELLED** sau approve → hoàn lại.
- Mỗi nhân viên có 1 record balance/(loại nghỉ phép, năm).

## 5. Đơn nghỉ phép (Leave Request)

### 5.1. Thông tin đơn

- Loại nghỉ phép
- Ngày bắt đầu — Ngày kết thúc (có thể cùng ngày)
- Phần ngày bắt đầu: `FULL_DAY` | `MORNING` | `AFTERNOON`
- Phần ngày kết thúc: `FULL_DAY` | `MORNING` | `AFTERNOON`
- Lý do (text, bắt buộc)
- Người duyệt (manager trực tiếp — tự suy ra)
- Tổng số ngày tính được (tự động tính, không cho nhập tay)

### 5.2. Quy tắc tính số ngày

1. Lặp qua từng ngày trong khoảng `[start_date, end_date]`.
2. Bỏ qua **Thứ Bảy, Chủ Nhật**.
3. Bỏ qua các ngày trong bảng `holidays`.
4. Mỗi ngày tính `1.0` ngày, **trừ**:
   - Ngày đầu nếu là `MORNING` hoặc `AFTERNOON` → `0.5`
   - Ngày cuối nếu là `MORNING` hoặc `AFTERNOON` → `0.5`
   - Nếu start = end và có nửa ngày → `0.5`
5. Validate: tổng ngày tính ra **> 0**.

### 5.3. Validate khi tạo đơn

- Ngày bắt đầu **không trong quá khứ** (so với ngày hiện tại).
- Ngày kết thúc **>= ngày bắt đầu**.
- Khoảng ngày **không trùng** với đơn `PENDING` hoặc `APPROVED` đang có của cùng nhân viên.
- Nếu loại nghỉ `requires_balance = true`: số ngày tính ra **<= balance còn lại**.
- Loại nghỉ phép phải đang `is_active`.

### 5.4. Trạng thái đơn (`LeaveRequestStatus`)

```
PENDING ──approve──> APPROVED ──cancel(by employee/HR)──> CANCELLED
   │                     │
   │                     └──cancel before start_date
   └──reject────> REJECTED
   │
   └──cancel(by employee while pending)──> CANCELLED
```

### 5.5. Hành động cho phép

| Hành động | Ai | Khi nào |
|---|---|---|
| Tạo đơn | Employee, Manager, HR | Bất kỳ lúc nào (đơn của chính mình) |
| Sửa đơn | Người tạo | Chỉ khi `PENDING` |
| Hủy đơn | Người tạo | Khi `PENDING`, hoặc `APPROVED` nhưng chưa tới ngày `start_date` |
| Duyệt/từ chối | Manager trực tiếp, HR (override) | Khi `PENDING` |
| Override APPROVED | HR | Trường hợp đặc biệt |

## 6. Lịch tổng hợp (Calendar)

- View tháng (mặc định) và tuần.
- Chỉ hiển thị đơn **APPROVED**.
- Filter:
  - Theo phòng ban
  - Theo loại nghỉ phép
  - Theo người (cho HR/Admin)
- Hover/click ngày → xem chi tiết ai nghỉ.
- Phân quyền view:
  - Employee: thấy lịch của phòng ban mình.
  - Manager: thấy lịch của các phòng ban có cấp dưới.
  - HR/Admin: thấy toàn bộ.

## 7. Ngày lễ (Holiday)

- Admin/HR nhập danh sách ngày lễ theo năm.
- Mỗi ngày lễ: `date`, `name`, `description` (tùy chọn).
- Áp dụng cho toàn công ty (MVP không phân biệt theo vùng/chi nhánh).
- Dùng trong tính số ngày nghỉ thực tế.

## 8. Phòng ban (Department)

- Cấu trúc phẳng (MVP, không hierarchy).
- Mỗi nhân viên thuộc 1 phòng ban.
- Mỗi phòng ban có thể có 1 trưởng phòng (`head_user_id`) — không bắt buộc trùng với `manager_id` của nhân viên.

## 9. Authentication & Authorization

- **Auth**: JWT (access token 15 phút + refresh token 7 ngày).
- **Đăng ký**: chỉ Admin tạo được tài khoản mới (MVP — không có self-register).
- **Đổi mật khẩu**: nhân viên đổi được mật khẩu của chính mình.
- **Reset mật khẩu**: Admin reset thủ công (MVP).
- **Phân quyền**: theo `role` (RBAC đơn giản), kết hợp kiểm tra phạm vi dữ liệu ở tầng nghiệp vụ.

## 10. Dashboard

### 10.1. Employee
- Số ngày phép còn lại theo từng loại.
- Đơn của tôi: số đang PENDING / APPROVED sắp tới / lịch sử.
- Lịch nghỉ team trong tháng.

### 10.2. Manager
- Tất cả của Employee.
- Đơn cần duyệt (PENDING) — danh sách nổi bật.
- Team đang nghỉ hôm nay / tuần này.

### 10.3. HR / Admin
- Tổng số nhân viên, số đơn theo trạng thái.
- Top phòng ban nghỉ nhiều/ít trong tháng.
- Quick links: quản lý user, ngày lễ, balance.

## 11. Báo cáo (MVP — cơ bản)

- Xuất danh sách đơn theo phòng ban + khoảng thời gian → CSV.
- Báo cáo tổng số ngày nghỉ theo loại theo tháng/quý.

## 12. Phi chức năng (Non-functional)

- **Timezone**: Asia/Ho_Chi_Minh.
- **Ngôn ngữ UI**: Tiếng Việt (duy nhất — không làm i18n, xem §14).
- **Hiệu năng**: thiết kế cho công ty cỡ ≤ 500 nhân viên.
- **Audit**: mọi action quan trọng (approve/reject/cancel, thay đổi balance) → ghi log.
- **Bảo mật**: password BCrypt cost ≥ 12, JWT secret từ env, không log thông tin nhạy cảm.
- **Trình duyệt**: Chrome/Edge/Firefox bản mới nhất.

## 13. Không thuộc phạm vi MVP

Để rõ ràng — các tính năng **không** làm trong MVP. Một số đã được bổ sung ở v2.0.0:

> v2.0.0 là mốc deploy cuối của miniproject. Tính năng bổ sung sau mốc này chỉ chạy ở môi trường
> demo/test local, không triển khai lên Vercel/Railway/Neon.

- ✅ ~~In-app notification (chuông)~~ — đã làm ở v2.0.0.
- ✅ ~~Email notification~~ — đã làm ở v2.0.0 (Mailpit bật ở dev; production giữ tắt; SMS không làm).
- ✅ ~~Carry over phép sang năm sau~~ — đã làm ở v2.0.0 (xem §4).
- ✅ Upload file (giấy bác sĩ…) — **chỉ phục vụ demo/test local**. Metadata lưu
  PostgreSQL, file lưu Docker named volume; không dùng object storage và không deploy tính năng
  này lên hạ tầng production hiện tại. Tối đa 5 file/đơn, 5MB/file, chỉ PDF/JPG/PNG; chỉ sửa khi đơn `PENDING`.
- ✅ Báo cáo nâng cao — có preview tổng hợp theo tháng/quý, loại nghỉ và phòng ban; CSV tổng hợp có số đơn, CSV quỹ phép có filter phòng ban.
- ✅ Cải thiện notification — có filter tất cả/chưa đọc, nhãn trạng thái đọc, loại sự kiện, thời điểm đọc và điều hướng từ thông báo tới chi tiết đơn.
- ⏳ Chế độ xem lịch theo phòng ban — làm rõ phạm vi quan sát lịch cho Manager/HR.
- ⏳ Cải thiện UI/UX — tiếp tục hoàn thiện responsive, validation, empty/loading/error state.
- ❌ Mobile app native
- ❌ Self-register
- ❌ Quên mật khẩu tự reset qua email
- ❌ Báo cáo Excel phức tạp với biểu đồ (CSV hiện tại mở được bằng Excel)

## 14. Loại trừ vĩnh viễn (không nằm trong v2)

Các tính năng đã cân nhắc và **quyết định không làm**, kể cả tương lai:

- ❌ **i18n vi/en** — UI tiếng Việt duy nhất; không có nhu cầu tiếng Anh cho dự án này (quyết định 06/2026, trước đó dự kiến "chừa chỗ" ở §12).
- ❌ Tích hợp Google Calendar / Outlook (2-way sync) — OAuth + đồng bộ phức tạp, không tương xứng giá trị.
- ❌ Multi-tenant / multi-company — kiến trúc khác hoàn toàn, không hợp với mục tiêu portfolio.
- ❌ SSO (Google Workspace / Microsoft 365) — chỉ cần khi triển khai cho công ty thật, không cần cho portfolio.
