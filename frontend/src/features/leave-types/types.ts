export type LeaveType = {
  id: number;
  code: string;
  name: string;
  description: string | null;
  defaultQuotaDays: number;
  requiresBalance: boolean;
  active: boolean;
};

export type LeaveTypeRequest = {
  code: string;
  name: string;
  description: string | null;
  defaultQuotaDays: number;
  requiresBalance: boolean;
  isActive: boolean;
};
