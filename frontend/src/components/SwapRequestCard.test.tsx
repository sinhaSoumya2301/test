import { describe, it, expect, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "@/test-utils";
import { SwapRequestCard } from "./SwapRequestCard";
import type { SwapRequestResponse } from "@/types";

vi.mock("@/hooks/useSwapRequests", () => ({
  usePeerDecision: () => ({ mutateAsync: vi.fn() }),
  useManagerDecision: () => ({ mutateAsync: vi.fn() }),
  useCancelSwapRequest: () => ({ mutateAsync: vi.fn() }),
}));

function makeRequest(overrides: Partial<SwapRequestResponse> = {}): SwapRequestResponse {
  return {
    id: "req-1",
    status: "PENDING_PEER_APPROVAL",
    requesterId: "requester-1",
    requesterName: "Alice",
    requesterShiftId: "shift-1",
    targetEmployeeId: "target-1",
    targetEmployeeName: "Bob",
    targetShiftId: null,
    reason: "Family event",
    approvals: [],
    createdAt: new Date().toISOString(),
    expiresAt: new Date().toISOString(),
    ...overrides,
  };
}

describe("SwapRequestCard", () => {
  it("shows Accept/Decline to the target while pending peer approval", () => {
    renderWithProviders(<SwapRequestCard request={makeRequest()} viewerId="target-1" context="mine" />);

    expect(screen.getByRole("button", { name: /accept/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /decline/i })).toBeInTheDocument();
  });

  it("does not show Accept/Decline to the requester, but shows Cancel", () => {
    renderWithProviders(<SwapRequestCard request={makeRequest()} viewerId="requester-1" context="mine" />);

    expect(screen.queryByRole("button", { name: /accept/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cancel request/i })).toBeInTheDocument();
  });

  it("shows Approve/Reject in team context when pending manager approval", () => {
    renderWithProviders(
      <SwapRequestCard
        request={makeRequest({ status: "PENDING_MANAGER_APPROVAL" })}
        viewerId="manager-1"
        context="team"
      />,
    );

    expect(screen.getByRole("button", { name: /approve/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /reject/i })).toBeInTheDocument();
  });

  it("does not show manager actions in 'mine' context even if pending manager approval", () => {
    renderWithProviders(
      <SwapRequestCard
        request={makeRequest({ status: "PENDING_MANAGER_APPROVAL" })}
        viewerId="requester-1"
        context="mine"
      />,
    );

    expect(screen.queryByRole("button", { name: /approve/i })).not.toBeInTheDocument();
  });

  it("shows no actions once a request has reached a terminal status", () => {
    renderWithProviders(
      <SwapRequestCard request={makeRequest({ status: "APPROVED" })} viewerId="requester-1" context="mine" />,
    );

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
