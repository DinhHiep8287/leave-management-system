import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";

import { ErrorBoundary } from "@/components/error-boundary";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { NotificationBell } from "@/features/notifications/notification-bell";
import { cn } from "@/lib/utils";

type NavItem = { to: string; label: string; show: (role: string) => boolean };

const ALL = () => true;
const APPROVER = (role: string) => role === "MANAGER" || role === "HR" || role === "ADMIN";
const HR_ADMIN = (role: string) => role === "HR" || role === "ADMIN";
const ADMIN_ONLY = (role: string) => role === "ADMIN";

// Text-led navigation per docs/UI-GUIDELINES.md (no decorative icons).
const NAV: NavItem[] = [
  { to: "/", label: "Tổng quan", show: ALL },
  { to: "/leave-requests/new", label: "Nộp đơn", show: ALL },
  { to: "/leave-requests", label: "Đơn của tôi", show: ALL },
  { to: "/approvals", label: "Cần duyệt", show: APPROVER },
  { to: "/calendar", label: "Lịch nghỉ phép", show: ALL },
  { to: "/my-department", label: "Phòng ban của tôi", show: ALL },
  { to: "/reports", label: "Báo cáo", show: HR_ADMIN },
  { to: "/admin/users", label: "Người dùng", show: HR_ADMIN },
  { to: "/admin/departments", label: "Phòng ban", show: ADMIN_ONLY },
  { to: "/admin/leave-types", label: "Loại nghỉ phép", show: ADMIN_ONLY },
  { to: "/admin/balances", label: "Quỹ phép", show: HR_ADMIN },
  { to: "/admin/holidays", label: "Ngày lễ", show: HR_ADMIN },
];

function NavList({ items, onNavigate }: { items: NavItem[]; onNavigate?: () => void }) {
  return (
    <nav className="flex-1 space-y-1 p-3">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === "/"}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              "block rounded-md px-3 py-2 text-sm font-medium transition-colors",
              isActive
                ? "bg-primary/10 text-primary"
                : "text-muted-foreground hover:bg-secondary hover:text-foreground",
            )
          }
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}

export function AppLayout() {
  const { user, logout } = useAuth();
  const role = user?.role ?? "EMPLOYEE";
  const items = NAV.filter((item) => item.show(role));
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-border md:flex">
        <div className="flex h-14 items-center border-b border-border px-5">
          <span className="text-sm font-semibold tracking-tight">Quản lý nghỉ phép</span>
        </div>
        <NavList items={items} />
      </aside>

      {/* Mobile slide-out navigation */}
      {mobileNavOpen && (
        <div className="fixed inset-0 z-50 md:hidden" role="dialog" aria-label="Menu điều hướng">
          <button
            type="button"
            aria-label="Đóng menu"
            className="absolute inset-0 bg-black/40"
            onClick={() => setMobileNavOpen(false)}
          />
          <div className="absolute inset-y-0 left-0 flex w-64 flex-col border-r border-border bg-background shadow-lg">
            <div className="flex h-14 items-center justify-between border-b border-border px-5">
              <span className="text-sm font-semibold tracking-tight">Quản lý nghỉ phép</span>
              <button
                type="button"
                aria-label="Đóng menu"
                className="rounded-md px-2 py-1 text-sm text-muted-foreground hover:bg-secondary"
                onClick={() => setMobileNavOpen(false)}
              >
                Đóng
              </button>
            </div>
            <NavList items={items} onNavigate={() => setMobileNavOpen(false)} />
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-border px-4 md:px-6">
          <div className="flex items-center gap-2 md:hidden">
            <button
              type="button"
              aria-label="Mở menu điều hướng"
              className="rounded-md border border-border px-2.5 py-1.5 text-sm font-medium hover:bg-secondary"
              onClick={() => setMobileNavOpen(true)}
            >
              Menu
            </button>
            <span className="text-sm font-semibold text-foreground">Quản lý nghỉ phép</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <NotificationBell />
            <ThemeToggle />
            <Link to="/profile" className="rounded-md px-2 py-1 text-right hover:bg-secondary">
              <p className="text-sm font-medium leading-tight">{user?.fullName}</p>
              <p className="text-xs text-muted-foreground">{roleLabel(role)}</p>
            </Link>
            <Button variant="outline" size="sm" onClick={() => void logout()}>
              Đăng xuất
            </Button>
          </div>
        </header>

        <main className="flex-1 overflow-x-hidden p-4 md:p-6">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </main>
      </div>
    </div>
  );
}

function roleLabel(role: string): string {
  switch (role) {
    case "ADMIN":
      return "Quản trị viên";
    case "HR":
      return "Nhân sự";
    case "MANAGER":
      return "Quản lý";
    default:
      return "Nhân viên";
  }
}
