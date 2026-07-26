import { apiClient } from "./client";
import type { ShiftResponse } from "@/types";

export async function fetchMyShifts(from: string, to: string): Promise<ShiftResponse[]> {
  const response = await apiClient.get<ShiftResponse[]>("/shifts/me", { params: { from, to } });
  return response.data;
}
