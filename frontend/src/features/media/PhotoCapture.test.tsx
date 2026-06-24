import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PhotoCapture } from "./PhotoCapture";

beforeEach(() => {
  vi.stubGlobal("URL", { ...URL, createObjectURL: () => "blob:fake", revokeObjectURL: vi.fn() });
});
afterEach(() => vi.unstubAllGlobals());

function fileInput(): HTMLInputElement {
  return document.querySelector('input[type="file"]') as HTMLInputElement;
}

describe("PhotoCapture", () => {
  it("accepts a valid image, previews it, and emits the file", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<PhotoCapture onChange={onChange} />);

    const file = new File([new Uint8Array([1, 2, 3])], "rash.png", { type: "image/png" });
    await user.upload(fileInput(), file);

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(file));
    expect(screen.getByAltText(/selected symptom photo/i)).toBeInTheDocument();
  });

  it("rejects a non-image file with an error and emits nothing", async () => {
    const onChange = vi.fn();
    // applyAccept:false (a setup option) so userEvent doesn't pre-filter on the input's accept=
    // attribute — we want the non-image to reach the component so its own validation produces the error.
    const user = userEvent.setup({ applyAccept: false });
    render(<PhotoCapture onChange={onChange} />);

    const pdf = new File(["x"], "doc.pdf", { type: "application/pdf" });
    await user.upload(fileInput(), pdf);

    expect(await screen.findByRole("alert")).toHaveTextContent(/jpeg, png, or webp/i);
    expect(onChange).not.toHaveBeenCalled();
  });
});
