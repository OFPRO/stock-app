import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

describe("UI Components", () => {
  it("renders a Button with text", () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText("Click me")).toBeInTheDocument();
  });

  it("renders a Badge with text", () => {
    render(<Badge>New</Badge>);
    expect(screen.getByText("New")).toBeInTheDocument();
  });

  it("renders a disabled Button", () => {
    render(<Button disabled>Disabled</Button>);
    const btn = screen.getByText("Disabled") as HTMLButtonElement;
    expect(btn).toBeDisabled();
  });

  it("renders Button with variant", () => {
    render(<Button variant="destructive">Delete</Button>);
    const btn = screen.getByText("Delete");
    expect(btn).toBeInTheDocument();
    expect(btn.className).toContain("destructive");
  });
});
