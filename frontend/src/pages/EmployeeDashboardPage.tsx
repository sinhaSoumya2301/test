import { useAuth } from "@/hooks/useAuth";
import { NavBar } from "@/components/NavBar";
import { MyShiftsAndSwaps } from "@/components/MyShiftsAndSwaps";

export function EmployeeDashboardPage() {
  const { employee } = useAuth();
  if (!employee) return null;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavBar />
      <main className="mx-auto max-w-4xl px-4 py-8">
        <MyShiftsAndSwaps viewerId={employee.id} />
      </main>
    </div>
  );
}
