import { useTeamSwapRequests } from "@/hooks/useSwapRequests";
import { SwapRequestCard } from "./SwapRequestCard";

export function TeamApprovals({ viewerId }: { viewerId: string }) {
  const { data } = useTeamSwapRequests();
  const requests = data?.content ?? [];
  const pending = requests.filter((r) => r.status === "PENDING_MANAGER_APPROVAL");
  const decided = requests.filter((r) => r.status !== "PENDING_MANAGER_APPROVAL");

  return (
    <section className="mt-10 border-t border-gray-200 pt-8">
      <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">
        Team requests awaiting your approval
      </h2>
      <div className="space-y-3">
        {pending.length === 0 && <p className="text-sm text-gray-500">Nothing pending your approval.</p>}
        {pending.map((request) => (
          <SwapRequestCard key={request.id} request={request} viewerId={viewerId} context="team" />
        ))}
      </div>

      {decided.length > 0 && (
        <>
          <h3 className="mb-2 mt-6 text-sm font-semibold uppercase tracking-wide text-gray-500">Recent team history</h3>
          <div className="space-y-3">
            {decided.map((request) => (
              <SwapRequestCard key={request.id} request={request} viewerId={viewerId} context="team" />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
