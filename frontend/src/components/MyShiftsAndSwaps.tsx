import { useMyShifts } from "@/hooks/useShifts";
import { useMySwapRequests } from "@/hooks/useSwapRequests";
import { ShiftList } from "./ShiftList";
import { SwapRequestCard } from "./SwapRequestCard";
import { SwapRequestForm } from "./SwapRequestForm";

/** Shared between EmployeeDashboardPage and ManagerDashboardPage — a manager has shifts too. */
export function MyShiftsAndSwaps({ viewerId }: { viewerId: string }) {
  const { data: shifts } = useMyShifts();
  const { data: swapRequests } = useMySwapRequests();

  return (
    <div className="grid gap-8 md:grid-cols-2">
      <section>
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">My upcoming shifts</h2>
        <ShiftList shifts={shifts ?? []} />

        <h2 className="mb-2 mt-6 text-sm font-semibold uppercase tracking-wide text-gray-500">Request a swap</h2>
        <SwapRequestForm />
      </section>

      <section>
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">My swap requests</h2>
        <div className="space-y-3">
          {(swapRequests?.content ?? []).length === 0 && (
            <p className="text-sm text-gray-500">No swap requests yet.</p>
          )}
          {(swapRequests?.content ?? []).map((request) => (
            <SwapRequestCard key={request.id} request={request} viewerId={viewerId} context="mine" />
          ))}
        </div>
      </section>
    </div>
  );
}
