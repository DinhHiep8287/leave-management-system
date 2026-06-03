import { Navigate, Route, Routes } from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { AuthProvider } from "@/features/auth/auth-context";
import { LoginPage } from "@/features/auth/login-page";
import { ApprovalInboxPage } from "@/features/approvals/inbox-page";
import { DashboardPage } from "@/features/dashboard/dashboard-page";
import { MyRequestsPage } from "@/features/leave-requests/my-requests-page";
import { SubmitLeaveRequestPage } from "@/features/leave-requests/submit-page";
import { ProtectedRoute } from "@/routes/protected-route";

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
          <Route path="/" element={<DashboardPage />} />
          <Route path="/leave-requests" element={<MyRequestsPage />} />
          <Route path="/leave-requests/new" element={<SubmitLeaveRequestPage />} />
          <Route path="/approvals" element={<ApprovalInboxPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
