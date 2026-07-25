import { useAuth } from "@/hooks/useAuth";
import { NavBar } from "@/components/NavBar";
import { MyShiftsAndSwaps } from "@/components/MyShiftsAndSwaps";
import { TeamApprovals } from "@/components/TeamApprovals";

/** A manager has their own shifts/requests (MyShiftsAndSwaps) plus their team's approvals queue. */
export function ManagerDashboardPage() {
  const { employee } = useAuth();
  if (!employee) return null;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavBar />
      <main className="mx-auto max-w-4xl px-4 py-8">
        <MyShiftsAndSwaps viewerId={employee.id} />
        <TeamApprovals viewerId={employee.id} />
      </main>
    </div>
  );
}
