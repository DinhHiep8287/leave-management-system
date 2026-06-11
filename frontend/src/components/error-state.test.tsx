import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import { ErrorState } from "./error-state";

describe("ErrorState", () => {
  it("renders the default message", () => {
    render(<ErrorState />);
    expect(screen.getByText("Không tải được dữ liệu. Vui lòng thử lại.")).toBeInTheDocument();
  });

  it("renders a custom message", () => {
    render(<ErrorState message="Không tải được danh sách" />);
    expect(screen.getByText("Không tải được danh sách")).toBeInTheDocument();
  });

  it("hides the retry button when onRetry is not provided", () => {
    render(<ErrorState />);
    expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
  });

  it("calls onRetry when the retry button is clicked", async () => {
    const onRetry = vi.fn();
    render(<ErrorState onRetry={onRetry} />);
    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
