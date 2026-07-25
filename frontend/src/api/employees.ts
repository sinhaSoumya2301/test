import { apiClient } from "./client";
import type { EmployeeResponse } from "@/types";

export async function fetchColleagues(): Promise<EmployeeResponse[]> {
  const response = await apiClient.get<EmployeeResponse[]>("/employees/colleagues");
  return response.data;
}

export async function fetchDirectReports(): Promise<EmployeeResponse[]> {
  const response = await apiClient.get<EmployeeResponse[]>("/employees/team");
  return response.data;
}
