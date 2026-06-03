/** Format an ISO date (or date-time) as dd/MM/yyyy. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  const parts = iso.slice(0, 10).split("-");
  if (parts.length !== 3) return iso;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

/** Format an ISO date-time as a short Vietnamese date + time. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "";
  const dt = new Date(iso);
  if (Number.isNaN(dt.getTime())) return iso;
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(dt);
}
