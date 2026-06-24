import { tokenStore } from "@/lib/api";

// The shared `api` wrapper forces Content-Type: application/json, which breaks multipart
// uploads (the browser must set the multipart boundary itself). So this feature uses a
// dedicated authed fetch — the same pattern downloadIcs() uses for the .ics blob endpoint.

export interface UploadedMedia {
  mediaUrl: string;
  contentType: string;
}

/**
 * Upload a photo or voice Blob and get back the in-platform mediaUrl to attach to a
 * symptom submission. Sends the Bearer token; lets the browser set the multipart boundary.
 */
export async function uploadMedia(file: Blob, filename: string): Promise<UploadedMedia> {
  const form = new FormData();
  form.append("file", file, filename);

  const res = await fetch("/api/v1/media", {
    method: "POST",
    credentials: "same-origin",
    headers: { Authorization: `Bearer ${tokenStore.get() ?? ""}` },
    body: form,
  });

  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(body.message ?? "Upload failed. Please try again.");
  }
  return (await res.json()) as UploadedMedia;
}

// File extension for a recorded/selected blob, used only to name the upload.
export function filenameFor(contentType: string): string {
  const ext = contentType.includes("png")
    ? "png"
    : contentType.includes("webp")
      ? "webp"
      : contentType.includes("jpeg") || contentType.includes("jpg")
        ? "jpg"
        : contentType.includes("webm")
          ? "webm"
          : contentType.includes("mp4") || contentType.includes("m4a")
            ? "m4a"
            : contentType.includes("wav")
              ? "wav"
              : contentType.includes("mpeg") || contentType.includes("mp3")
                ? "mp3"
                : "bin";
  return `intake.${ext}`;
}
