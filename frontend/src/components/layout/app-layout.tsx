import { NavLink, Outlet } from "react-router-dom";

import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils";

type NavItem = { to: string; label: string; show: (role: string) => boolean };

const ALL = () => true;
const APPROVER = (role: string) => role === "MANAGER" || role === "HR" || role === "ADMIN";
const HR_ADMIN = (role: string) => role === "HR" || role === "ADMIN";

// Text-led navigation per docs/UI-GUIDELINES.md (no decorative icons).
const NAV: NavItem[] = [
  { to: "/", label: "Tổng quan", show: ALL },
  { to: "/leave-requests/new", label: "Nộp đơn", show: ALL },
  { to: "/leave-requests", label: "Đơn của tôi", show: ALL },
  { to: "/approvals", label: "Cần duyệt", show: APPROVER },
  { to: "/calendar", label: "Lịch nghỉ phép", show: ALL },
  { to: "/reports", label: "Báo cáo", show: HR_ADMIN },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const role = user?.role ?? "EMPLOYEE";
  const items = NAV.filter((item) => item.show(role));

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-border md:flex">
        <div className="flex h-14 items-center border-b border-border px-5">
          <span className="text-sm font-semibold tracking-tight">Quản lý nghỉ phép</span>
        </div>
        <nav className="flex-1 space-y-1 p-3">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
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
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-border px-6">
          <div className="text-sm text-muted-foreground md:hidden">
            <span className="font-semibold text-foreground">Quản lý nghỉ phép</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <ThemeToggle />
            <div className="text-right">
              <p className="text-sm font-medium leading-tight">{user?.fullName}</p>
              <p className="text-xs text-muted-foreground">{roleLabel(role)}</p>
            </div>
            <Button variant="outline" size="sm" onClick={() => void logout()}>
              Đăng xuất
            </Button>
          </div>
        </header>

        <main className="flex-1 overflow-x-hidden p-6">
          <Outlet />
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
