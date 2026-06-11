import { apiErrorMessage } from "./api-error";

describe("apiErrorMessage", () => {
  it("extracts the backend error message from the axios envelope", () => {
    const e = { response: { data: { error: { message: "Số dư không đủ" } } } };
    expect(apiErrorMessage(e)).toBe("Số dư không đủ");
  });

  it("falls back to the default message when the envelope is missing", () => {
    expect(apiErrorMessage(new Error("network"))).toBe("Đã có lỗi xảy ra");
    expect(apiErrorMessage(undefined)).toBe("Đã có lỗi xảy ra");
    expect(apiErrorMessage({ response: { data: {} } })).toBe("Đã có lỗi xảy ra");
  });

  it("uses a custom fallback when provided", () => {
    expect(apiErrorMessage(null, "Tạo thất bại")).toBe("Tạo thất bại");
  });
});
