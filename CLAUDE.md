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

- **Tuần 1**: Foundation — Docker Compose + Auth + User CRUD + DB schema.
- **Tuần 2**: Core domain — LeaveType, LeaveBalance, Holiday + dashboard nền.
- **Tuần 3**: Nghiệp vụ chính — LeaveRequest + Approval workflow + tính ngày.
- **Tuần 4**: Lịch tổng hợp + báo cáo + polish + deploy guide.
