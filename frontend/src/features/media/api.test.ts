import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { uploadMedia, filenameFor } from "./api";

vi.mock("@/lib/api", () => ({ tokenStore: { get: () => "tok-123" } }));

describe("media api — uploadMedia", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("POSTs multipart with the Bearer token and returns the mediaUrl", async () => {
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue(
      new Response(JSON.stringify({ mediaUrl: "/api/v1/media/abc", contentType: "image/png" }), { status: 201 })
    );

    const blob = new Blob([new Uint8Array([1, 2, 3])], { type: "image/png" });
    const result = await uploadMedia(blob, "intake.png");

    expect(result).toEqual({ mediaUrl: "/api/v1/media/abc", contentType: "image/png" });

    const [url, init] = mockFetch.mock.calls[0];
    expect(url).toBe("/api/v1/media");
    expect((init as RequestInit).method).toBe("POST");
    // Bearer token is attached; Content-Type is NOT set so the browser adds the boundary.
    const headers = (init as RequestInit).headers as Record<string, string>;
    expect(headers.Authorization).toBe("Bearer tok-123");
    expect(headers["Content-Type"]).toBeUndefined();
    // Body is FormData with the file field.
    const body = (init as RequestInit).body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect(body.get("file")).toBeInstanceOf(File);
  });

  it("throws the server message on a failed upload", async () => {
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue(
      new Response(JSON.stringify({ message: "Unsupported media type: application/pdf" }), { status: 415 })
    );

    await expect(uploadMedia(new Blob(["x"], { type: "application/pdf" }), "x.pdf")).rejects.toThrow(
      /Unsupported media type/
    );
  });
});

describe("filenameFor", () => {
  it("maps content types to extensions the backend accepts", () => {
    expect(filenameFor("image/png")).toBe("intake.png");
    expect(filenameFor("image/jpeg")).toBe("intake.jpg");
    expect(filenameFor("image/webp")).toBe("intake.webp");
    expect(filenameFor("audio/webm")).toBe("intake.webm");
    expect(filenameFor("audio/mp4")).toBe("intake.m4a");
    expect(filenameFor("audio/wav")).toBe("intake.wav");
    expect(filenameFor("audio/mpeg")).toBe("intake.mp3");
  });
});
