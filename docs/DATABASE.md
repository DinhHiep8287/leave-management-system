# Thiết kế Database

> Schema chi tiết sẽ được tạo bằng Flyway migration trong `backend/src/main/resources/db/migration/`. File này mô tả thiết kế logic.

## 1. ERD (mô tả văn bản)

```
departments ─┐
             │
             ▼
           users ──── manager_id (self-ref)
             │
             ├──< leave_balances >── leave_types
             │
             ├──< leave_requests >── leave_types
             │           │
             │           └──< approval_actions >── users (approver)
             │
             └──< audit_log
                  
holidays (standalone)
refresh_tokens ──── users
```

## 2. Bảng

### 2.1. `departments`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| code | VARCHAR(50) | UNIQUE, NOT NULL | Vd: ENG, HR, SALES |
| name | VARCHAR(200) | NOT NULL | |
| head_user_id | BIGINT | FK users(id) NULL | Trưởng phòng |
| is_active | BOOLEAN | NOT NULL DEFAULT true | |
| created_at, updated_at | TIMESTAMPTZ | | Audit |

### 2.2. `users`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| employee_code | VARCHAR(50) | UNIQUE, NOT NULL | Mã NV |
| email | VARCHAR(200) | UNIQUE, NOT NULL | |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt |
| full_name | VARCHAR(200) | NOT NULL | |
| role | VARCHAR(20) | NOT NULL CHECK in (EMPLOYEE, MANAGER, HR, ADMIN) | |
| department_id | BIGINT | FK departments(id) NOT NULL | |
| manager_id | BIGINT | FK users(id) NULL | Self-reference |
| join_date | DATE | NOT NULL | |
| is_active | BOOLEAN | NOT NULL DEFAULT true | |
| created_at, updated_at | TIMESTAMPTZ | | |

Index: `(department_id)`, `(manager_id)`, `(email)`.

### 2.3. `leave_types`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| code | VARCHAR(50) | UNIQUE, NOT NULL | ANNUAL, SICK, UNPAID, PERSONAL |
| name | VARCHAR(200) | NOT NULL | |
| description | TEXT | NULL | |
| default_quota_days | NUMERIC(5,1) | NOT NULL | Hỗ trợ nửa ngày |
| requires_balance | BOOLEAN | NOT NULL DEFAULT true | UNPAID = false |
| is_active | BOOLEAN | NOT NULL DEFAULT true | |
| created_at, updated_at | TIMESTAMPTZ | | |

### 2.4. `leave_balances`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK users(id) NOT NULL | |
| leave_type_id | BIGINT | FK leave_types(id) NOT NULL | |
| year | INT | NOT NULL | |
| total_days | NUMERIC(5,1) | NOT NULL | Quota gốc |
| used_days | NUMERIC(5,1) | NOT NULL DEFAULT 0 | |
| adjusted_days | NUMERIC(5,1) | NOT NULL DEFAULT 0 | HR điều chỉnh +/- |
| carried_over_days | NUMERIC(5,1) | NOT NULL DEFAULT 0 | Phép tồn chuyển từ năm trước (V5, v2.0.0) |
| created_at, updated_at | TIMESTAMPTZ | | |

`remaining = total_days + adjusted_days + carried_over_days - used_days` (computed ở app, không lưu).

UNIQUE `(user_id, leave_type_id, year)`.

### 2.5. `holidays`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| holiday_date | DATE | UNIQUE, NOT NULL | |
| name | VARCHAR(200) | NOT NULL | |
| description | TEXT | NULL | |
| created_at, updated_at | TIMESTAMPTZ | | |

### 2.6. `leave_requests`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK users(id) NOT NULL | Người xin nghỉ |
| leave_type_id | BIGINT | FK leave_types(id) NOT NULL | |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL | |
| start_half | VARCHAR(20) | NOT NULL CHECK in (FULL_DAY, MORNING, AFTERNOON) | |
| end_half | VARCHAR(20) | NOT NULL CHECK in (FULL_DAY, MORNING, AFTERNOON) | |
| total_days | NUMERIC(5,1) | NOT NULL CHECK > 0 | Tính sẵn |
| reason | TEXT | NOT NULL | |
| status | VARCHAR(20) | NOT NULL CHECK in (PENDING, APPROVED, REJECTED, CANCELLED) | |
| manager_id | BIGINT | FK users(id) NOT NULL | Snapshot lúc tạo đơn |
| created_at, updated_at | TIMESTAMPTZ | | |

Index: `(user_id)`, `(manager_id)`, `(status)`, `(start_date, end_date)`.

CHECK `end_date >= start_date`.

### 2.7. `approval_actions`

Lưu lịch sử mọi hành động trên đơn (audit chi tiết riêng cho leave_request).

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| leave_request_id | BIGINT | FK leave_requests(id) NOT NULL | |
| actor_id | BIGINT | FK users(id) NOT NULL | Người thực hiện |
| action | VARCHAR(20) | NOT NULL CHECK in (CREATED, UPDATED, APPROVED, REJECTED, CANCELLED, OVERRIDE) | |
| previous_status | VARCHAR(20) | NULL | |
| new_status | VARCHAR(20) | NULL | |
| comment | TEXT | NULL | Lý do reject/cancel |
| created_at | TIMESTAMPTZ | NOT NULL | |

### 2.8. `audit_log`

Audit chung cho toàn hệ thống (balance adjustment, role change, ...).

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| actor_id | BIGINT | FK users(id) NULL | NULL khi system |
| action | VARCHAR(50) | NOT NULL | Vd: BALANCE_ADJUSTED, ROLE_CHANGED |
| target_type | VARCHAR(50) | NOT NULL | Vd: LeaveBalance, User |
| target_id | BIGINT | NULL | |
| old_value | JSONB | NULL | |
| new_value | JSONB | NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |

### 2.9. `refresh_tokens`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK users(id) NOT NULL | |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | Hash của refresh token |
| expires_at | TIMESTAMPTZ | NOT NULL | |
| revoked_at | TIMESTAMPTZ | NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |

Index: `(user_id)`, `(token_hash)`.

## 3. Quy ước

- Tên bảng: `snake_case`, **số nhiều** (`users`, `leave_requests`).
- Tên cột: `snake_case`.
- Khóa chính: `id BIGSERIAL`.
- Audit fields: `created_at`, `updated_at` ở mọi bảng nghiệp vụ.
- Timestamp: `TIMESTAMPTZ` (timezone-aware).
- Soft delete: **không dùng** trong MVP (nếu cần — thêm `deleted_at` riêng từng bảng).
- Enum: lưu dưới dạng `VARCHAR` + CHECK constraint (đơn giản, dễ migrate).

## 4. Dữ liệu mẫu (seed)

Migration cuối cùng sẽ seed dữ liệu demo:

- 4 leave types mặc định (ANNUAL, SICK, UNPAID, PERSONAL)
- 1 Admin: `admin@example.com` / password mặc định (đổi khi đăng nhập)
- 1 HR: `hr@example.com`
- 2 Manager + 5 Employee
- 3 phòng ban: ENGINEERING, HR, SALES
- 10 ngày lễ Việt Nam 2026

### 2.x. `notifications` (V4, v2.0.0)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK users(id) NOT NULL, ON DELETE CASCADE | Người nhận |
| leave_request_id | BIGINT | FK leave_requests(id) NULL, ON DELETE CASCADE | |
| event_type | VARCHAR(30) | CHECK (CREATED/UPDATED/APPROVED/REJECTED/CANCELLED) | |
| message | TEXT | NOT NULL | Tiếng Việt, render thẳng |
| is_read | BOOLEAN | NOT NULL DEFAULT false | |
| created_at, read_at | TIMESTAMPTZ | | Không có updated_at/created_by |

INDEX `(user_id, is_read)`. Ghi trong **cùng transaction** với transition của đơn.

## 5. Migration plan (Flyway)

```
V1__create_baseline_schema.sql   # Schema 9 bảng baseline
V2__seed_reference_data.sql      # 3 phòng ban, 4 loại nghỉ, 10 ngày lễ VN 2026
V3__fix_sick_quota.sql           # SICK quota 3 -> 30 (REQUIREMENTS §3)
V4__notifications.sql            # Bảng notifications (v2.0.0)
V5__carry_over.sql               # Cột leave_balances.carried_over_days (v2.0.0)
```

> User demo KHÔNG seed qua migration — `DemoDataInitializer`/`DemoLeaveSeeder`
> (CommandLineRunner, chỉ profile `dev`, chỉ khi DB trống) đảm nhiệm.
