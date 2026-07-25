import { apiClient } from "./client";
import type { CreateSwapRequestPayload, DecisionPayload, PageResponse, SwapRequestResponse } from "@/types";

const IDEMPOTENCY_HEADER = "Idempotency-Key";

// A fresh key per mutating call — a retried request (e.g. a flaky network) reuses the same key
// only within axios's own retry, which we don't do; this mainly protects against double-clicks
// via React Query's mutation lifecycle, per SPEC.md "API Contract" > Idempotency.
function idempotencyHeaders() {
  return { [IDEMPOTENCY_HEADER]: crypto.randomUUID() };
}

export async function createSwapRequest(payload: CreateSwapRequestPayload): Promise<SwapRequestResponse> {
  const response = await apiClient.post<SwapRequestResponse>("/shift-swaps", payload, {
    headers: idempotencyHeaders(),
  });
  return response.data;
}

export async function fetchMySwapRequests(page = 0, size = 20): Promise<PageResponse<SwapRequestResponse>> {
  const response = await apiClient.get<PageResponse<SwapRequestResponse>>("/shift-swaps/me", {
    params: { page, size },
  });
  return response.data;
}

export async function fetchTeamSwapRequests(page = 0, size = 20): Promise<PageResponse<SwapRequestResponse>> {
  const response = await apiClient.get<PageResponse<SwapRequestResponse>>("/shift-swaps/team", {
    params: { page, size },
  });
  return response.data;
}

export async function submitPeerDecision(id: string, payload: DecisionPayload): Promise<SwapRequestResponse> {
  const response = await apiClient.post<SwapRequestResponse>(`/shift-swaps/${id}/peer-decision`, payload, {
    headers: idempotencyHeaders(),
  });
  return response.data;
}

export async function submitManagerDecision(id: string, payload: DecisionPayload): Promise<SwapRequestResponse> {
  const response = await apiClient.post<SwapRequestResponse>(`/shift-swaps/${id}/manager-decision`, payload, {
    headers: idempotencyHeaders(),
  });
  return response.data;
}

export async function cancelSwapRequest(id: string): Promise<SwapRequestResponse> {
  const response = await apiClient.post<SwapRequestResponse>(`/shift-swaps/${id}/cancel`, undefined, {
    headers: idempotencyHeaders(),
  });
  return response.data;
}
