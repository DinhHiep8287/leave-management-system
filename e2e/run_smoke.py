"""
Playwright smoke test for the Leave Management System UI.

Drives the running dev stack (frontend at http://localhost:5173, backend at :8080)
through the key flows for each role, asserting screens render without runtime
errors. Captures screenshots into e2e/screenshots/ (light + dark for the dashboard).

Prereq: `docker compose up` (dev stack) + `pip install playwright && playwright install chromium`.
Run:     python e2e/run_smoke.py
"""

import datetime
import os
import sys

from playwright.sync_api import expect, sync_playwright

BASE = os.environ.get("E2E_BASE_URL", "http://localhost:5173")
# Screenshots double as the README/portfolio assets.
SHOTS = os.path.join(os.path.dirname(__file__), "..", "docs", "screenshots")
os.makedirs(SHOTS, exist_ok=True)

USERS = {
    "employee": ("eng.emp1@demo.local", "User@12345"),
    "manager": ("eng.manager@demo.local", "User@12345"),
    "hr": ("hr@demo.local", "User@12345"),
    "admin": ("admin@demo.local", "Admin@12345"),
}


def next_far_monday() -> datetime.date:
    d = datetime.date.today() + datetime.timedelta(days=80)
    while d.weekday() != 0:  # Monday
        d += datetime.timedelta(days=1)
    return d


def login(page, email: str, password: str) -> None:
    page.goto(BASE + "/login")
    page.get_by_label("Email").fill(email)
    page.get_by_label("Mật khẩu").fill(password)
    page.get_by_role("button", name="Đăng nhập").click()
    # Landing page may be the dashboard or the last protected route visited; just
    # assert we are authenticated, then navigate explicitly where needed.
    expect(page.get_by_role("button", name="Đăng xuất")).to_be_visible(timeout=10_000)
    page.wait_for_load_state("networkidle")


def logout(page) -> None:
    page.get_by_role("button", name="Đăng xuất").click()
    expect(page.get_by_role("button", name="Đăng nhập")).to_be_visible(timeout=10_000)


def nav(page, link_name: str, heading: str) -> None:
    page.get_by_role("link", name=link_name, exact=True).click()
    expect(page.get_by_role("heading", name=heading)).to_be_visible(timeout=10_000)
    page.wait_for_load_state("networkidle")


def shot(page, name: str) -> None:
    # networkidle is unreliable under Vite (HMR websocket); give queries a beat to render.
    page.wait_for_timeout(1200)
    page.screenshot(path=os.path.join(SHOTS, name + ".png"), full_page=True)


def submit_and_cancel(page) -> None:
    """Create an UNPAID request (no balance needed) then cancel it — idempotent."""
    start = next_far_monday()
    end = start + datetime.timedelta(days=2)  # Mon..Wed
    nav(page, "Nộp đơn", "Nộp đơn nghỉ phép")
    unpaid_value = page.eval_on_selector(
        "#leaveTypeId",
        "el => Array.from(el.options).find(o => o.text.includes('UNPAID'))?.value",
    )
    page.select_option("#leaveTypeId", value=unpaid_value)
    page.fill("#startDate", start.isoformat())
    page.fill("#endDate", end.isoformat())
    page.get_by_label("Lý do").fill("E2E smoke")
    page.get_by_role("button", name="Nộp đơn", exact=True).click()
    # Redirects to "my requests"
    expect(page.get_by_role("heading", name="Đơn của tôi")).to_be_visible(timeout=10_000)
    page.wait_for_load_state("networkidle")
    # Cancel the just-created pending request (first "Hủy" button), confirm dialog.
    page.get_by_role("button", name="Hủy", exact=True).first.click()
    page.get_by_role("button", name="Xác nhận hủy").click()
    page.wait_for_load_state("networkidle")


def step(msg: str) -> None:
    print("STEP:", msg, flush=True)


def run() -> None:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1366, "height": 900})

        # Employee: dashboard, submit + cancel, my requests, calendar, profile.
        step("login employee")
        login(page, *USERS["employee"])
        nav(page, "Tổng quan", "Tổng quan")
        shot(page, "dashboard-employee")
        submit_and_cancel(page)
        shot(page, "my-requests")
        nav(page, "Lịch nghỉ phép", "Lịch nghỉ phép")
        page.get_by_role("button", name="Tuần").click()
        page.get_by_role("button", name="Tháng").click()
        shot(page, "calendar")
        step("logout employee")
        logout(page)

        # Manager: approver inbox.
        step("login manager")
        login(page, *USERS["manager"])
        nav(page, "Cần duyệt", "Cần duyệt")
        shot(page, "approvals")
        logout(page)

        # HR: reports + org-wide dashboard section.
        step("login hr")
        login(page, *USERS["hr"])
        nav(page, "Tổng quan", "Tổng quan")
        expect(page.get_by_text("Toàn tổ chức")).to_be_visible(timeout=10_000)
        shot(page, "dashboard-hr")
        nav(page, "Báo cáo", "Báo cáo")
        shot(page, "reports")
        logout(page)

        # Admin: user management + dark mode.
        step("login admin")
        login(page, *USERS["admin"])
        nav(page, "Người dùng", "Người dùng")
        shot(page, "users")
        page.get_by_role("button", name="giao diện", exact=False).click()  # theme toggle
        page.wait_for_timeout(300)
        shot(page, "users-dark")
        logout(page)

        browser.close()
    print("E2E SMOKE OK")


if __name__ == "__main__":
    try:
        run()
    except Exception as e:  # noqa: BLE001
        msg = repr(e).encode("ascii", "replace").decode("ascii")
        print("E2E SMOKE FAILED:", msg[:800])
        sys.exit(1)
