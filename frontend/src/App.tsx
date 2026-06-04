import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { AuthProvider } from "@/features/auth/auth-context";
import { LoginPage } from "@/features/auth/login-page";
import { ProtectedRoute } from "@/routes/protected-route";

// Code-split protected pages so the login bundle stays small (recharts, date-fns,
// and feature code load only when their route is visited).
const DashboardPage = lazy(() =>
  import("@/features/dashboard/dashboard-page").then((m) => ({ default: m.DashboardPage })),
);
const MyRequestsPage = lazy(() =>
  import("@/features/leave-requests/my-requests-page").then((m) => ({ default: m.MyRequestsPage })),
);
const SubmitLeaveRequestPage = lazy(() =>
  import("@/features/leave-requests/submit-page").then((m) => ({ default: m.SubmitLeaveRequestPage })),
);
const ApprovalInboxPage = lazy(() =>
  import("@/features/approvals/inbox-page").then((m) => ({ default: m.ApprovalInboxPage })),
);
const CalendarPage = lazy(() =>
  import("@/features/calendar/calendar-page").then((m) => ({ default: m.CalendarPage })),
);
const ReportsPage = lazy(() =>
  import("@/features/reports/reports-page").then((m) => ({ default: m.ReportsPage })),
);
const UsersPage = lazy(() =>
  import("@/features/users/users-page").then((m) => ({ default: m.UsersPage })),
);
const DepartmentsPage = lazy(() =>
  import("@/features/departments/departments-page").then((m) => ({ default: m.DepartmentsPage })),
);
const LeaveTypesPage = lazy(() =>
  import("@/features/leave-types/leave-types-page").then((m) => ({ default: m.LeaveTypesPage })),
);
const BalancesPage = lazy(() =>
  import("@/features/balances/balances-page").then((m) => ({ default: m.BalancesPage })),
);
const HolidaysPage = lazy(() =>
  import("@/features/holidays/holidays-page").then((m) => ({ default: m.HolidaysPage })),
);

function PageFallback() {
  return <div className="p-6 text-sm text-muted-foreground">Đang tải…</div>;
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route
            path="/"
            element={
              <Suspense fallback={<PageFallback />}>
                <DashboardPage />
              </Suspense>
            }
          />
          <Route
            path="/leave-requests"
            element={
              <Suspense fallback={<PageFallback />}>
                <MyRequestsPage />
              </Suspense>
            }
          />
          <Route
            path="/leave-requests/new"
            element={
              <Suspense fallback={<PageFallback />}>
                <SubmitLeaveRequestPage />
              </Suspense>
            }
          />
          <Route
            path="/approvals"
            element={
              <Suspense fallback={<PageFallback />}>
                <ApprovalInboxPage />
              </Suspense>
            }
          />
          <Route
            path="/calendar"
            element={
              <Suspense fallback={<PageFallback />}>
                <CalendarPage />
              </Suspense>
            }
          />
          <Route
            path="/reports"
            element={
              <Suspense fallback={<PageFallback />}>
                <ReportsPage />
              </Suspense>
            }
          />
          <Route
            path="/admin/users"
            element={
              <Suspense fallback={<PageFallback />}>
                <UsersPage />
              </Suspense>
            }
          />
          <Route
            path="/admin/departments"
            element={
              <Suspense fallback={<PageFallback />}>
                <DepartmentsPage />
              </Suspense>
            }
          />
          <Route
            path="/admin/leave-types"
            element={
              <Suspense fallback={<PageFallback />}>
                <LeaveTypesPage />
              </Suspense>
            }
          />
          <Route
            path="/admin/balances"
            element={
              <Suspense fallback={<PageFallback />}>
                <BalancesPage />
              </Suspense>
            }
          />
          <Route
            path="/admin/holidays"
            element={
              <Suspense fallback={<PageFallback />}>
                <HolidaysPage />
              </Suspense>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
