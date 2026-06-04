export type Role = "EMPLOYEE" | "MANAGER" | "HR" | "ADMIN";

export type User = {
  id: number;
  employeeCode: string;
  email: string;
  fullName: string;
  role: Role;
  departmentId: number | null;
  managerId: number | null;
  joinDate: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type UserCreateRequest = {
  employeeCode: string;
  email: string;
  fullName: string;
  password: string;
  role: Role;
  departmentId: number;
  managerId: number | null;
  joinDate: string;
};

export type UserUpdateRequest = Omit<UserCreateRequest, "password">;

export const ROLE_LABELS: Record<Role, string> = {
  EMPLOYEE: "Nhân viên",
  MANAGER: "Quản lý",
  HR: "Nhân sự",
  ADMIN: "Quản trị viên",
};

export const ROLES: Role[] = ["EMPLOYEE", "MANAGER", "HR", "ADMIN"];
