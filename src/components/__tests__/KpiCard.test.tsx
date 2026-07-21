import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { KpiCard } from "@/components/KpiCard";

describe("KpiCard", () => {
  it("renders the label, value, and hint", () => {
    render(<KpiCard label="Open Pipeline Value" value="₹4,89,360" hint="Weighted: ₹3,91,488" />);

    expect(screen.getByText("Open Pipeline Value")).toBeInTheDocument();
    expect(screen.getByText("₹4,89,360")).toBeInTheDocument();
    expect(screen.getByText("Weighted: ₹3,91,488")).toBeInTheDocument();
  });
});
