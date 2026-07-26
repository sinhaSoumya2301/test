import { useState } from "react";
import type { SwapRequestResponse } from "@/types";
import { useCancelSwapRequest, useManagerDecision, usePeerDecision } from "@/hooks/useSwapRequests";
import { apiErrorMessage } from "@/api/client";

const STATUS_STYLES: Record<SwapRequestResponse["status"], string> = {
  PENDING_PEER_APPROVAL: "bg-amber-100 text-amber-700",
  PENDING_MANAGER_APPROVAL: "bg-blue-100 text-blue-700",
  APPROVED: "bg-green-100 text-green-700",
  REJECTED_BY_PEER: "bg-red-100 text-red-700",
  REJECTED_BY_MANAGER: "bg-red-100 text-red-700",
  CANCELLED: "bg-gray-100 text-gray-500",
  EXPIRED: "bg-gray-100 text-gray-500",
};

interface SwapRequestCardProps {
  request: SwapRequestResponse;
  viewerId: string;
  /** "mine" = viewer is the requester/target; "team" = viewer is reviewing as manager. */
  context: "mine" | "team";
}

export function SwapRequestCard({ request, viewerId, context }: SwapRequestCardProps) {
  const [error, setError] = useState<string | null>(null);
  const peerDecision = usePeerDecision();
  const managerDecision = useManagerDecision();
  const cancel = useCancelSwapRequest();

  const isRequester = request.requesterId === viewerId;
  const isTarget = request.targetEmployeeId === viewerId;

  const canPeerDecide = context === "mine" && isTarget && request.status === "PENDING_PEER_APPROVAL";
  const canManagerDecide = context === "team" && request.status === "PENDING_MANAGER_APPROVAL";
  const canCancel =
    context === "mine" &&
    isRequester &&
    (request.status === "PENDING_PEER_APPROVAL" || request.status === "PENDING_MANAGER_APPROVAL");

  const runAction = async (action: Promise<unknown>) => {
    setError(null);
    try {
      await action;
    } catch (e) {
      setError(apiErrorMessage(e));
    }
  };

  return (
    <div className="rounded-md border border-gray-200 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-gray-900">
            {request.requesterName} → {request.targetEmployeeName}
          </p>
          <p className="mt-0.5 text-sm text-gray-600">{request.reason}</p>
          <p className="mt-1 text-xs text-gray-400">
            Requested {new Date(request.createdAt).toLocaleDateString()} · expires{" "}
            {new Date(request.expiresAt).toLocaleDateString()}
          </p>
        </div>
        <span className={`whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[request.status]}`}>
          {request.status.replaceAll("_", " ")}
        </span>
      </div>

      {request.approvals.length > 0 && (
        <ul className="mt-3 space-y-1 border-t border-gray-100 pt-2 text-xs text-gray-500">
          {request.approvals.map((approval) => (
            <li key={approval.stage}>
              {approval.stage === "PEER" ? "Colleague" : "Manager"} ({approval.approverName}):{" "}
              <span className={approval.decision === "APPROVED" ? "text-green-600" : "text-red-600"}>
                {approval.decision}
              </span>
              {approval.note ? ` — "${approval.note}"` : ""}
            </li>
          ))}
        </ul>
      )}

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      {(canPeerDecide || canManagerDecide || canCancel) && (
        <div className="mt-3 flex gap-2 border-t border-gray-100 pt-3">
          {canPeerDecide && (
            <>
              <button
                onClick={() => runAction(peerDecision.mutateAsync({ id: request.id, payload: { approve: true } }))}
                className="rounded bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700"
              >
                Accept
              </button>
              <button
                onClick={() => runAction(peerDecision.mutateAsync({ id: request.id, payload: { approve: false } }))}
                className="rounded bg-gray-200 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-300"
              >
                Decline
              </button>
            </>
          )}
          {canManagerDecide && (
            <>
              <button
                onClick={() => runAction(managerDecision.mutateAsync({ id: request.id, payload: { approve: true } }))}
                className="rounded bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700"
              >
                Approve
              </button>
              <button
                onClick={() => runAction(managerDecision.mutateAsync({ id: request.id, payload: { approve: false } }))}
                className="rounded bg-gray-200 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-300"
              >
                Reject
              </button>
            </>
          )}
          {canCancel && (
            <button
              onClick={() => runAction(cancel.mutateAsync(request.id))}
              className="rounded bg-gray-200 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-300"
            >
              Cancel request
            </button>
          )}
        </div>
      )}
    </div>
  );
}
