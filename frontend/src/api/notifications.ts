import { apiClient } from "./client";
import type { NotificationResponse, PageResponse } from "@/types";

export async function fetchNotifications(unreadOnly: boolean): Promise<PageResponse<NotificationResponse>> {
  const response = await apiClient.get<PageResponse<NotificationResponse>>("/notifications", {
    params: { unread: unreadOnly },
  });
  return response.data;
}

export async function markNotificationRead(id: string): Promise<void> {
  await apiClient.post(`/notifications/${id}/read`);
}
