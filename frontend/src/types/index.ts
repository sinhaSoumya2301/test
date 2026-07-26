// Mirrors backend/src/main/java/com/company/hr/model/dto — see SPEC.md "API Contract".

export type EmployeeRole = "EMPLOYEE" | "MANAGER" | "ADMIN";

export interface EmployeeResponse {
  id: string;
  employeeCode: string;
  fullName: string;
  email: string;
  role: EmployeeRole;
  departmentName: string | null;
  managerId: string | null;
  active: boolean;
}

export type ShiftStatus = "SCHEDULED" | "SWAPPED" | "CANCELLED";

export interface ShiftResponse {
  id: string;
  employeeId: string;
  employeeName: string;
  shiftDate: string;
  startTime: string;
  endTime: string;
  status: ShiftStatus;
}

export type SwapStatus =
  | "PENDING_PEER_APPROVAL"
  | "PENDING_MANAGER_APPROVAL"
  | "REJECTED_BY_PEER"
  | "REJECTED_BY_MANAGER"
  | "APPROVED"
  | "CANCELLED"
  | "EXPIRED";

export type ApprovalStage = "PEER" | "MANAGER";
export type ApprovalDecision = "APPROVED" | "REJECTED";

export interface ApprovalResponse {
  stage: ApprovalStage;
  approverId: string;
  approverName: string;
  decision: ApprovalDecision;
  note: string | null;
  decidedAt: string;
}

export interface SwapRequestResponse {
  id: string;
  status: SwapStatus;
  requesterId: string;
  requesterName: string;
  requesterShiftId: string;
  targetEmployeeId: string;
  targetEmployeeName: string;
  targetShiftId: string | null;
  reason: string;
  approvals: ApprovalResponse[];
  createdAt: string;
  expiresAt: string;
}

export interface CreateSwapRequestPayload {
  requesterShiftId: string;
  targetEmployeeId: string;
  targetShiftId?: string | null;
  reason: string;
}

export interface DecisionPayload {
  approve: boolean;
  comments?: string;
}

export type NotificationType =
  | "SWAP_REQUESTED"
  | "SWAP_PEER_ACCEPTED"
  | "SWAP_PEER_DECLINED"
  | "SWAP_MANAGER_APPROVED"
  | "SWAP_MANAGER_REJECTED"
  | "SWAP_CANCELLED"
  | "SWAP_EXPIRED";

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  relatedSwapRequestId: string | null;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
    traceId: string;
  };
}
