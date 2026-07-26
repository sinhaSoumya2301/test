import { useState } from "react";
import { useMarkNotificationRead, useNotifications } from "@/hooks/useNotifications";

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const { data } = useNotifications(false);
  const markRead = useMarkNotificationRead();

  const notifications = data?.content ?? [];
  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((prev) => !prev)}
        className="relative rounded p-2 text-gray-600 hover:bg-gray-100"
        aria-label="Notifications"
      >
        🔔
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-10 mt-2 w-80 rounded-md border border-gray-200 bg-white shadow-lg">
          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 && <p className="p-4 text-sm text-gray-500">No notifications yet.</p>}
            {notifications.map((n) => (
              <div
                key={n.id}
                className={`border-b border-gray-100 p-3 text-sm last:border-b-0 ${n.read ? "bg-white" : "bg-blue-50"}`}
              >
                <p className="text-gray-800">{n.message}</p>
                <div className="mt-1 flex items-center justify-between">
                  <span className="text-xs text-gray-400">{new Date(n.createdAt).toLocaleString()}</span>
                  {!n.read && (
                    <button
                      onClick={() => markRead.mutate(n.id)}
                      className="text-xs font-medium text-blue-600 hover:underline"
                    >
                      Mark read
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
