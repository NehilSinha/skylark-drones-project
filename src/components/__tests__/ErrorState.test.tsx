import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ErrorState } from "@/components/ErrorState";
import { ApiError } from "@/lib/api";

describe("ErrorState", () => {
  it("shows the ApiError message and traceId", () => {
    render(<ErrorState error={new ApiError("Monday.com request failed", 502, "MONDAY_API_ERROR", "trace-123")} />);

    expect(screen.getByText("Monday.com request failed")).toBeInTheDocument();
    expect(screen.getByText(/trace-123/)).toBeInTheDocument();
  });

  it("falls back to a generic message for a non-ApiError", () => {
    render(<ErrorState error={new Error("network down")} />);

    expect(screen.getByText("network down")).toBeInTheDocument();
  });

  it("calls onRetry when the retry button is clicked", async () => {
    const onRetry = vi.fn();
    render(<ErrorState error={new Error("boom")} onRetry={onRetry} />);

    await userEvent.click(screen.getByRole("button", { name: /try again/i }));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
