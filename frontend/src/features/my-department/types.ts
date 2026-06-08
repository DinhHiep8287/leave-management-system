import type { Role } from "@/features/users/types";

export type DepartmentMember = {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  isHead: boolean;
};

export type MyDepartment = {
  id: number;
  code: string;
  name: string;
  headUserId: number | null;
  headName: string | null;
  members: DepartmentMember[];
};
