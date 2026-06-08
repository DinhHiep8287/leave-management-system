import { useQuery } from "@tanstack/react-query";

import { getMyDepartment } from "./api";

export function useMyDepartment() {
  return useQuery({ queryKey: ["my-department"], queryFn: getMyDepartment });
}
