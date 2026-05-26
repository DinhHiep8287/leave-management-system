# Changelog

Tất cả thay đổi đáng chú ý của dự án sẽ được ghi lại ở đây.

Định dạng dựa trên [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
và dự án tuân theo [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
