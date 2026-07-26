import type { ShiftResponse } from "@/types";

const STATUS_STYLES: Record<ShiftResponse["status"], string> = {
  SCHEDULED: "bg-gray-100 text-gray-700",
  SWAPPED: "bg-purple-100 text-purple-700",
  CANCELLED: "bg-gray-100 text-gray-400 line-through",
};

export function ShiftList({ shifts }: { shifts: ShiftResponse[] }) {
  if (shifts.length === 0) {
    return <p className="text-sm text-gray-500">No upcoming shifts in the next 60 days.</p>;
  }

  return (
    <ul className="divide-y divide-gray-100 rounded-md border border-gray-200">
      {shifts.map((shift) => (
        <li key={shift.id} className="flex items-center justify-between px-4 py-2.5 text-sm">
          <span className="text-gray-800">
            {shift.shiftDate} · {shift.startTime.slice(0, 5)}–{shift.endTime.slice(0, 5)}
          </span>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[shift.status]}`}>
            {shift.status}
          </span>
        </li>
      ))}
    </ul>
  );
}
