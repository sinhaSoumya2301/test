import { useAuth } from "@/hooks/useAuth";
import { EmployeeDashboardPage } from "./EmployeeDashboardPage";
import { ManagerDashboardPage } from "./ManagerDashboardPage";

/** Picks the right dashboard by role — see SPEC.md LLD "Frontend component structure". */
export function DashboardRouter() {
  const { employee } = useAuth();
  if (!employee) return null;

  return employee.role === "MANAGER" || employee.role === "ADMIN" ? (
    <ManagerDashboardPage />
  ) : (
    <EmployeeDashboardPage />
  );
}
