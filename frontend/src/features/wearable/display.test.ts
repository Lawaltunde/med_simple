import { describe, it, expect } from "vitest";
import { formatValue } from "./display";

describe("formatValue", () => {
  it("renders heart rate in bpm", () => {
    expect(formatValue("HEART_RATE", 72.4)).toBe("72 bpm");
    expect(formatValue("RESTING_HEART_RATE", 61)).toBe("61 bpm");
  });

  it("renders sleep minutes as hours", () => {
    expect(formatValue("SLEEP_MINUTES", 432)).toBe("7.2 h");
  });

  it("renders steps with thousands separators", () => {
    expect(formatValue("STEPS", 6000)).toBe("6,000");
  });

  it("renders SpO2 as a percentage", () => {
    expect(formatValue("SPO2", 97)).toBe("97%");
  });

  it("renders a dash for missing values", () => {
    expect(formatValue("HEART_RATE", null)).toBe("—");
  });
});
