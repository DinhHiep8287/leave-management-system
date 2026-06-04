export type Department = {
  id: number;
  code: string;
  name: string;
  headUserId: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type DepartmentRequest = {
  code: string;
  name: string;
  headUserId: number | null;
  isActive: boolean;
};
