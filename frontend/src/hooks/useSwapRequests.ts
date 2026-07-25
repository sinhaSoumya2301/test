import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelSwapRequest,
  createSwapRequest,
  fetchMySwapRequests,
  fetchTeamSwapRequests,
  submitManagerDecision,
  submitPeerDecision,
} from "@/api/swaps";
import type { CreateSwapRequestPayload, DecisionPayload } from "@/types";

export function useMySwapRequests() {
  return useQuery({
    queryKey: ["swap-requests", "me"],
    queryFn: () => fetchMySwapRequests(),
  });
}

export function useTeamSwapRequests() {
  return useQuery({
    queryKey: ["swap-requests", "team"],
    queryFn: () => fetchTeamSwapRequests(),
  });
}

function useInvalidateSwapQueries() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["swap-requests"] });
    queryClient.invalidateQueries({ queryKey: ["shifts"] });
    queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };
}

export function useCreateSwapRequest() {
  const invalidate = useInvalidateSwapQueries();
  return useMutation({
    mutationFn: (payload: CreateSwapRequestPayload) => createSwapRequest(payload),
    onSuccess: invalidate,
  });
}

export function usePeerDecision() {
  const invalidate = useInvalidateSwapQueries();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: DecisionPayload }) => submitPeerDecision(id, payload),
    onSuccess: invalidate,
  });
}

export function useManagerDecision() {
  const invalidate = useInvalidateSwapQueries();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: DecisionPayload }) => submitManagerDecision(id, payload),
    onSuccess: invalidate,
  });
}

export function useCancelSwapRequest() {
  const invalidate = useInvalidateSwapQueries();
  return useMutation({
    mutationFn: (id: string) => cancelSwapRequest(id),
    onSuccess: invalidate,
  });
}
