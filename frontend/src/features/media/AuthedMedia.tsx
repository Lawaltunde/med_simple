import { useEffect, useState } from "react";
import { tokenStore } from "@/lib/api";

interface AuthedMediaProps {
  // The in-platform mediaUrl, e.g. /api/v1/media/{id}. The GET is auth-gated, so a plain
  // <img src> can't load it (no Bearer header) — fetch it as a blob with the token instead.
  mediaUrl: string;
  inputType: "PHOTO" | "VOICE";
}

/**
 * Renders auth-gated patient media inline. Fetches the bytes with the Bearer token, turns them
 * into an object URL, and shows a photo or an audio player. Revokes the URL on cleanup so PHI
 * blobs don't linger in memory.
 */
export function AuthedMedia({ mediaUrl, inputType }: AuthedMediaProps) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let revoked = false;
    let url: string | null = null;

    fetch(mediaUrl, {
      credentials: "same-origin",
      headers: { Authorization: `Bearer ${tokenStore.get() ?? ""}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error(String(res.status));
        return res.blob();
      })
      .then((blob) => {
        if (revoked) return;
        url = URL.createObjectURL(blob);
        setObjectUrl(url);
      })
      .catch(() => setFailed(true));

    return () => {
      revoked = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [mediaUrl]);

  if (failed) {
    return <p className="text-xs text-muted">Media unavailable.</p>;
  }
  if (!objectUrl) {
    return <div className="h-24 w-full animate-pulse rounded-xl bg-canvas" aria-label="Loading media" />;
  }
  if (inputType === "PHOTO") {
    return (
      <img
        src={objectUrl}
        alt="Submitted symptom photo"
        className="max-h-64 rounded-xl border border-line object-contain"
      />
    );
  }
  return <audio src={objectUrl} controls className="w-full" />;
}
