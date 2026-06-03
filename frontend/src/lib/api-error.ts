/** Pull the backend error message out of an axios error ({ error: { message } } envelope). */
export function apiErrorMessage(e: unknown, fallback = "Đã có lỗi xảy ra"): string {
  const msg = (e as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error
    ?.message;
  return msg ?? fallback;
}
