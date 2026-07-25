import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import type { EmployeeRole } from "@/types";

export function ProtectedRoute({ allowedRoles }: { allowedRoles?: EmployeeRole[] }) {
  const { employee, isLoading } = useAuth();

  if (isLoading) {
    return <div className="flex h-screen items-center justify-center text-gray-500">Loading…</div>;
  }
  if (!employee) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && !allowedRoles.includes(employee.role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
