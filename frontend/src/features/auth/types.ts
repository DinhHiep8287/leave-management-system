export type TokenPair = {
  accessToken: string;
  refreshToken: string;
  accessExpiresInSeconds: number;
};

export type Me = {
  id: number;
  email: string;
  fullName: string;
  role: "EMPLOYEE" | "MANAGER" | "HR" | "ADMIN";
  departmentId: number | null;
  managerId: number | null;
  active: boolean;
};
