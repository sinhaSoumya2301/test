import { apiClient } from "./client";
import type { EmployeeResponse } from "@/types";

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export async function login(email: string, password: string): Promise<AuthTokenResponse> {
  const response = await apiClient.post<AuthTokenResponse>("/auth/login", { email, password });
  return response.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post("/auth/logout", { refreshToken });
}

export async function fetchCurrentEmployee(): Promise<EmployeeResponse> {
  const response = await apiClient.get<EmployeeResponse>("/auth/me");
  return response.data;
}
