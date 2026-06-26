# Kiến trúc hệ thống

## 1. Sơ đồ tổng thể

```
┌────────────────────────────────────────────────────────────────┐
│                        Browser (Client)                         │
│           React 18 + TypeScript + Vite + Tailwind               │
│                  TanStack Query · React Router                  │
└──────────────────────────────┬─────────────────────────────────┘
                               │ HTTPS / JSON
                               │ JWT Bearer
                               ▼
┌────────────────────────────────────────────────────────────────┐
│                  Backend (Spring Boot 3.3)                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Controller (REST API + Validation)                       │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │ Service (Business Logic)                                 │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │ Repository (Spring Data JPA)                             │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │ Domain (Entity, Value Object, Domain Service)            │   │
│  └─────────────────────────────────────────────────────────┘   │
│  Cross-cutting: Security · Exception Handler · Audit · Logging  │
└──────────────────────────────┬─────────────────────────────────┘
                               │ JDBC
                               ▼
┌────────────────────────────────────────────────────────────────┐
│                    PostgreSQL 16                                │
│              (schema versioned by Flyway)                       │
└────────────────────────────────────────────────────────────────┘
```

Toàn bộ chạy trong **Docker Compose**.

## 2. Quyết định kiến trúc (ADR ngắn)

### ADR-001: Monolith thay vì microservices
- **Quyết định**: Một backend duy nhất.
- **Lý do**: Quy mô <500 user, domain nhỏ, team 1 người. Microservices là over-engineering.

### ADR-002: Layered + Package by Feature
- **Quyết định**: Cấu trúc package theo nghiệp vụ (`auth/`, `user/`, `leaverequest/`), bên trong mỗi feature lại chia layer (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`).
- **Lý do**: Tìm code nhanh, dễ tách module khi cần, vẫn giữ được kỷ luật layer.

### ADR-003: DTO tách rời Entity
- **Quyết định**: Không trả Entity qua API. Dùng MapStruct để map.
- **Lý do**: Tránh leak field nội bộ, tránh lazy loading exception, dễ versioning API.

### ADR-004: Flyway thay vì JPA `ddl-auto`
- **Quyết định**: Toàn bộ schema do Flyway quản lý. `spring.jpa.hibernate.ddl-auto=validate`.
- **Lý do**: Production an toàn, audit được thay đổi schema, đồng bộ dev/prod.

### ADR-005: JWT thay vì session
- **Quyết định**: Access token (15') + Refresh token (7d) lưu DB.
- **Lý do**: Stateless, dễ scale, frontend SPA thuận tiện. Refresh token revoke được vì lưu DB.

### ADR-006: Dockerize toàn bộ từ đầu
- **Quyết định**: Cả backend và frontend dev đều chạy trong container.
- **Lý do**: Onboarding 1 lệnh, môi trường nhất quán, gần với production. Trade-off: hot-reload chậm hơn 1 chút — chấp nhận.

## 3. Cấu trúc thư mục (high-level)

```
leave-management-system/
├── .github/workflows/           # CI
├── docs/                        # Tài liệu
├── backend/
│   ├── Dockerfile               # Production multi-stage
│   ├── Dockerfile.dev           # Dev with hot-reload
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle/wrapper/
│   ├── gradlew, gradlew.bat
│   └── src/
│       ├── main/
│       │   ├── java/<base-package>/
│       │   │   ├── LeaveApplication.java
│       │   │   ├── config/      # SecurityConfig, OpenApiConfig, ...
│       │   │   ├── common/      # BaseEntity, exception, response
│       │   │   ├── auth/        # Login, JWT
│       │   │   ├── user/
│       │   │   ├── department/
│       │   │   ├── leavetype/
│       │   │   ├── leavebalance/
│       │   │   ├── leaverequest/
│       │   │   ├── holiday/
│       │   │   └── calendar/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/    # Flyway V1__init.sql, ...
│       └── test/
│           └── java/<base-package>/
├── frontend/
│   ├── Dockerfile
│   ├── Dockerfile.dev
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── tailwind.config.ts
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── routes/
│       ├── features/
│       │   ├── auth/
│       │   ├── leave-request/
│       │   ├── calendar/
│       │   ├── dashboard/
│       │   └── admin/
│       ├── components/ui/       # shadcn
│       ├── lib/                 # api client, utils
│       └── types/
├── docker-compose.yml           # Dev
├── docker-compose.prod.yml      # Production override
└── ...
```

## 4. Bảo mật

- **Password**: BCrypt cost ≥ 12.
- **JWT secret**: từ env, không hardcode.
- **CORS**: chỉ cho phép origin frontend đã biết.
- **Rate limit**: bucket4j cho endpoint login (sẽ thêm sau).
- **SQL injection**: dùng JPA/parameterized query.
- **XSS**: React tự escape; CSP header.
- **CSRF**: không cần (JWT bearer trong header, không dùng cookie).

## 5. Logging & Audit

- **Logging**: SLF4J + Logback, JSON format trong production.
- **Request log**: log mọi request với traceId.
- **Audit**: bảng `audit_log` ghi lại thay đổi quan trọng (approve, reject, cancel, balance adjustment) với `actor_id`, `action`, `target_type`, `target_id`, `old_value`, `new_value`, `timestamp`.

## 5b. Notification (v2.0.0)

- **In-app**: `NotificationService` ghi bảng `notifications` trong **cùng transaction** với
  transition của đơn (CREATED/UPDATED → manager; APPROVED/REJECTED → requester; CANCELLED →
  bên còn lại; không tự thông báo cho người thao tác). FE chuông polling 30 giây.
- **Email**: `LeaveRequestService` publish `LeaveRequestChangedEvent`;
  `EmailNotificationListener` (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) gửi
  SMTP — lỗi mail không bao giờ ảnh hưởng transaction nghiệp vụ. Cờ `app.mail.enabled`
  (mặc định false); dev bật sẵn với **Mailpit** (UI :8025).

## 5c. Attachment local-only

- **Metadata**: bảng `attachments` trong PostgreSQL, FK tới `leave_requests` và `users`.
- **File vật lý**: Docker named volume `backend-attachments`, mount vào `/app/uploads/attachments`.
- **Phạm vi**: chỉ bật ở dev/local qua `app.attachments.enabled=true`; production giữ tắt và frontend không hiển thị UI.
- **Giới hạn**: tối đa 5 file/đơn, 5MB/file, chỉ PDF/JPG/PNG.
- **Quyền**: requester upload/xóa khi đơn còn `PENDING`; requester, manager, HR/Admin được xem/tải.

## 6. Error handling

- Global `@RestControllerAdvice` → response chuẩn `ErrorResponse`.
- Error code map ra HTTP status cụ thể.
- Validation error trả về detailed field-level message.

## 7. Testing strategy

| Loại | Tool | Phạm vi |
|---|---|---|
| Unit | JUnit 5 + Mockito | Service, domain logic (tính ngày nghỉ, validate) |
| Integration | Spring Boot Test + Testcontainers | Repository, controller, full request flow với Postgres thật |
| Frontend unit | Vitest | Util, hook |
| Frontend component | React Testing Library | Component quan trọng |
| E2E | Playwright (optional, tuần 4) | Critical user flow |

Mục tiêu coverage: **70%+ cho backend**, không bắt buộc cho frontend.

## 8. Deployment (planning — tuần 4)

```
Production:
  - Backend image  → Railway / Fly.io
  - Frontend image → Vercel (static build) hoặc cùng Railway
  - Postgres       → Managed (Neon / Supabase)
  - Secrets        → Platform secret manager
  - Domain         → Optional
```

## 9. Roadmap mở rộng (sau MVP)

`v2.0.0` là mốc deploy cuối. Mọi thử nghiệm sau đó chỉ chạy local bằng Docker Compose, không mở
rộng hạ tầng Vercel/Railway/Neon.

**v2.0:**
- Email + in-app notification → tách module `notification/`.
- Carry-over phép → policy ở `leavebalance/` service, ADMIN chạy thủ công.
- Gói cải tiến UI/UX cho mobile, form nộp đơn và trạng thái tải/rỗng.

**Sau v2.0 (chỉ demo/test local):**
- Upload file đính kèm → module `attachment/`, metadata trong PostgreSQL, file trong Docker named volume.
- Các thử nghiệm khác không được kéo thêm dịch vụ production hoặc object storage ngoài.

**v3 (nice to have, chỉ local):**
- Mobile app → cùng API, thêm endpoint riêng nếu cần.

**Loại trừ vĩnh viễn**: Google Calendar sync, multi-tenant, SSO. Xem `REQUIREMENTS.md §14`.
