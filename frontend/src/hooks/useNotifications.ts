import { useMutation, useQuery, useQueryClient, type QueryKey } from "@tanstack/react-query";
import { fetchNotifications, markNotificationRead } from "@/api/notifications";
import type { NotificationResponse, PageResponse } from "@/types";

export function useNotifications(unreadOnly: boolean) {
  return useQuery({
    queryKey: ["notifications", { unreadOnly }],
    queryFn: () => fetchNotifications(unreadOnly),
    refetchInterval: 30_000,
  });
}

type NotificationsSnapshot = [QueryKey, PageResponse<NotificationResponse> | undefined][];

/**
 * Optimistically flips `read` in the cache immediately (SPEC.md "Testing Strategy" > Frontend
 * unit: "optimistic update + rollback on API failure") so the bell/list feel instant, then
 * rolls back to the pre-mutation snapshot if the request actually fails.
 */
export function useMarkNotificationRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => markNotificationRead(id),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: ["notifications"] });
      const previous: NotificationsSnapshot = queryClient.getQueriesData({ queryKey: ["notifications"] });

      queryClient.setQueriesData<PageResponse<NotificationResponse>>({ queryKey: ["notifications"] }, (old) => {
        if (!old) return old;
        return { ...old, content: old.content.map((n) => (n.id === id ? { ...n, read: true } : n)) };
      });

      return { previous };
    },
    onError: (_err, _id, context) => {
      context?.previous.forEach(([queryKey, data]) => {
        queryClient.setQueryData(queryKey, data);
      });
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });
}
