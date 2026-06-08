import type { Role } from "@/features/users/types";

/** Shape of GET /users/me — includes resolved department + manager names. */
export type MyProfile = {
  id: number;
  employeeCode: string;
  email: string;
  fullName: string;
  role: Role;
  departmentId: number | null;
  departmentName: string | null;
  managerId: number | null;
  managerName: string | null;
  joinDate: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};
