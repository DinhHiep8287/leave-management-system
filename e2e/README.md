# E2E smoke (Playwright)

`run_smoke.py` drives the **running dev stack** through the key flows for every role
(employee / manager / HR / admin) and asserts each screen renders without runtime errors.
It also captures screenshots into `docs/screenshots/` (used by the README).

## Prerequisites

```bash
# 1. Dev stack running (frontend :5173, backend :8080)
docker compose up -d

# 2. Playwright on the host (one-time)
pip install playwright
playwright install chromium
```

## Run

```bash
python e2e/run_smoke.py
# → prints STEP: ... lines, then "E2E SMOKE OK"
```

Override the frontend URL with `E2E_BASE_URL` if needed.

## What it covers

- **Employee**: login, submit an UNPAID request, see it in "Đơn của tôi", then cancel it
  (idempotent), calendar month/week toggle.
- **Manager**: approver inbox renders.
- **HR**: org-wide dashboard section + reports page.
- **Admin**: user management page; dark-mode toggle.

## Notes

- Demo accounts (dev profile): `eng.emp1@demo.local` / `eng.manager@demo.local` /
  `hr@demo.local` / `admin@demo.local` (passwords `User@12345`, admin `Admin@12345`).
- After the run, the script **hard-deletes its own cancelled smoke request** from the dev DB
  (via `docker compose exec postgres psql`) so repeated runs don't accumulate rows. If docker
  is unavailable it prints a warning and the smoke still passes.
- **Runs in CI** (job `e2e` in `.github/workflows/ci.yml`): the workflow brings up the full
  stack with `docker compose up -d --build` (the dev seeder populates the fresh CI database),
  polls `/api/health` and `:5173`, then runs this script. On failure it dumps container logs
  and uploads `docs/screenshots/` as an artifact.
