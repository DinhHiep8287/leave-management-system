import { api } from "@/lib/api";

import type { MyDepartment } from "./types";

type Envelope<T> = { data: T };

export async function getMyDepartment(): Promise<MyDepartment> {
  const res = await api.get<Envelope<MyDepartment>>("/departments/mine");
  return res.data.data;
}
