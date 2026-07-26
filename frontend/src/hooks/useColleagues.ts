import { useQuery } from "@tanstack/react-query";
import { fetchColleagues, fetchDirectReports } from "@/api/employees";

export function useColleagues() {
  return useQuery({ queryKey: ["employees", "colleagues"], queryFn: fetchColleagues });
}

export function useDirectReports() {
  return useQuery({ queryKey: ["employees", "team"], queryFn: fetchDirectReports });
}
