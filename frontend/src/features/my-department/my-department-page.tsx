import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ErrorState } from "@/components/error-state";
import { ROLE_LABELS } from "@/features/users/types";

import { useMyDepartment } from "./hooks";

export function MyDepartmentPage() {
  const { data, isLoading, isError, refetch } = useMyDepartment();

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Phòng ban của tôi</h1>
        <p className="text-sm text-muted-foreground">
          Thông tin phòng ban và các thành viên đang hoạt động.
        </p>
      </header>

      {isError && <ErrorState onRetry={() => void refetch()} />}

      {!isError && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              {data ? `${data.name} (${data.code})` : "Đang tải…"}
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Trưởng phòng: {data?.headName ?? "Chưa gán"}
            </p>
          </CardHeader>
          <CardContent>
            <div className="rounded-lg border border-border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Họ tên</TableHead>
                    <TableHead>Vai trò</TableHead>
                    <TableHead>Email</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isLoading && (
                    <TableRow>
                      <TableCell colSpan={3} className="text-center text-muted-foreground">
                        Đang tải…
                      </TableCell>
                    </TableRow>
                  )}
                  {!isLoading && (data?.members.length ?? 0) === 0 && (
                    <TableRow>
                      <TableCell colSpan={3} className="text-center text-muted-foreground">
                        Phòng ban chưa có thành viên.
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.members.map((m) => (
                    <TableRow key={m.id}>
                      <TableCell className="font-medium">
                        {m.fullName}
                        {m.isHead && (
                          <span className="ml-2 rounded-md bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                            Trưởng phòng
                          </span>
                        )}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{ROLE_LABELS[m.role]}</TableCell>
                      <TableCell className="text-muted-foreground">{m.email}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
