import { useQuery } from "@tanstack/react-query";
import { fetchMyShifts } from "@/api/shifts";

/** Upcoming shifts for the logged-in employee, today through +60 days. */
export function useMyShifts() {
  const from = new Date().toISOString().slice(0, 10);
  const to = new Date(Date.now() + 60 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

  return useQuery({
    queryKey: ["shifts", "me", from, to],
    queryFn: () => fetchMyShifts(from, to),
  });
}
