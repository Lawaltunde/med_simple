import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MonthCalendar } from "./MonthCalendar";

describe("MonthCalendar", () => {
  it("calls onSelect with the clicked day", async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<MonthCalendar selected={null} onSelect={onSelect} />);

    const target = new Date();
    target.setDate(target.getDate() + 2);
    let btn = screen.queryByLabelText(target.toDateString());
    if (!btn) {
      await user.click(screen.getByLabelText("Next month"));
      btn = screen.getByLabelText(target.toDateString());
    }
    await user.click(btn!);

    expect(onSelect).toHaveBeenCalledOnce();
    const picked: Date = onSelect.mock.calls[0][0];
    expect(picked.getDate()).toBe(target.getDate());
  });

  it("disables days before minDate", () => {
    const today = new Date();
    render(<MonthCalendar selected={null} onSelect={vi.fn()} minDate={today} />);
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    // only present when yesterday is in the current month view
    const btn = screen.queryByLabelText(yesterday.toDateString());
    if (btn) expect(btn).toBeDisabled();
  });

  it("shows the AI window caption when a recommendation is given", () => {
    const from = new Date();
    const to = new Date();
    to.setDate(to.getDate() + 6);
    render(<MonthCalendar selected={null} onSelect={vi.fn()} recommended={{ from, to }} />);
    expect(screen.getByText(/AI-recommended follow-up window/i)).toBeInTheDocument();
  });
});
