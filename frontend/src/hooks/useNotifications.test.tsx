import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useMarkNotificationRead } from "./useNotifications";
import * as notificationsApi from "@/api/notifications";
import type { NotificationResponse, PageResponse } from "@/types";

vi.mock("@/api/notifications");

const QUERY_KEY = ["notifications", { unreadOnly: false }];

function makePage(): PageResponse<NotificationResponse> {
  return {
    content: [
      {
        id: "n1",
        type: "SWAP_REQUESTED",
        relatedSwapRequestId: null,
        message: "hi",
        read: false,
        createdAt: new Date().toISOString(),
      },
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  };
}

function setup() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

// SPEC.md "Testing Strategy" > Frontend unit: optimistic update + rollback on API failure.
describe("useMarkNotificationRead", () => {
  beforeEach(() => vi.resetAllMocks());

  it("optimistically flips read=true before the request resolves", async () => {
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(QUERY_KEY, makePage());
    vi.mocked(notificationsApi.fetchNotifications).mockResolvedValue(makePage());
    let resolveRequest!: () => void;
    vi.mocked(notificationsApi.markNotificationRead).mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = () => resolve(undefined);
      }),
    );

    const { result } = renderHook(() => useMarkNotificationRead(), { wrapper });

    act(() => {
      result.current.mutate("n1");
    });

    await waitFor(() => {
      const cached = queryClient.getQueryData<PageResponse<NotificationResponse>>(QUERY_KEY);
      expect(cached?.content[0].read).toBe(true);
    });

    resolveRequest();
  });

  it("rolls back to the pre-mutation snapshot when the request fails", async () => {
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(QUERY_KEY, makePage());
    vi.mocked(notificationsApi.fetchNotifications).mockResolvedValue(makePage());
    vi.mocked(notificationsApi.markNotificationRead).mockRejectedValueOnce(new Error("boom"));

    const { result } = renderHook(() => useMarkNotificationRead(), { wrapper });

    act(() => {
      result.current.mutate("n1");
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    const cached = queryClient.getQueryData<PageResponse<NotificationResponse>>(QUERY_KEY);
    expect(cached?.content[0].read).toBe(false);
  });
});
