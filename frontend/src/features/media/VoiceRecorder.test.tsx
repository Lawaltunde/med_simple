import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { VoiceRecorder } from "./VoiceRecorder";

// jsdom has no MediaRecorder / getUserMedia — stand them in so the recorder can drive its flow.
class FakeMediaRecorder {
  static isTypeSupported = () => true;
  ondataavailable: ((e: { data: Blob }) => void) | null = null;
  onstop: (() => void) | null = null;
  state = "inactive";
  constructor(public stream: MediaStream, public options: { mimeType: string }) {}
  start() {
    this.state = "recording";
  }
  stop() {
    this.state = "inactive";
    this.ondataavailable?.({ data: new Blob([new Uint8Array([1, 2, 3])], { type: this.options.mimeType }) });
    this.onstop?.();
  }
}

const stopTrack = vi.fn();

beforeEach(() => {
  vi.stubGlobal("MediaRecorder", FakeMediaRecorder);
  vi.stubGlobal("navigator", {
    ...navigator,
    mediaDevices: {
      getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: stopTrack }] }),
    },
  });
  // jsdom doesn't implement these on Blob URLs.
  vi.stubGlobal("URL", { ...URL, createObjectURL: () => "blob:fake", revokeObjectURL: vi.fn() });
  stopTrack.mockReset();
});

afterEach(() => vi.unstubAllGlobals());

describe("VoiceRecorder", () => {
  it("records, stops, emits a blob, and releases the mic", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<VoiceRecorder onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: /tap to record/i }));
    expect(await screen.findByText(/recording/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /recording/i }));

    await waitFor(() => expect(onChange).toHaveBeenCalled());
    const blob = onChange.mock.calls.at(-1)![0] as Blob;
    expect(blob).toBeInstanceOf(Blob);
    expect(blob.size).toBeGreaterThan(0);
    // The mic stream is released as soon as recording stops.
    expect(stopTrack).toHaveBeenCalled();
    // Playback control appears after recording.
    expect(screen.getByTestId("voice-playback")).toBeInTheDocument();
  });

  it("surfaces a friendly error when mic access is blocked", async () => {
    (navigator.mediaDevices.getUserMedia as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error("denied"));
    const user = userEvent.setup();
    render(<VoiceRecorder onChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: /tap to record/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/microphone access was blocked/i);
  });
});
