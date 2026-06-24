import { useRef, useState, useEffect } from "react";
import { Camera, X } from "lucide-react";
import { Button } from "@/components/ui/button";

// Content types the backend accepts for images (mirrors MediaContentType on the server).
const ACCEPTED = ["image/jpeg", "image/png", "image/webp"];
const MAX_BYTES = 15 * 1024 * 1024;

interface PhotoCaptureProps {
  // Called with the chosen image, or null when cleared.
  onChange: (file: File | null) => void;
  disabled?: boolean;
}

/**
 * Photo intake control: pick from gallery or capture with the device camera (the `capture`
 * attribute hints the camera on mobile), with a live preview. Validates type and size on the
 * client for a fast reject; the server validates again.
 */
export function PhotoCapture({ onChange, disabled }: PhotoCaptureProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Revoke the object URL when the preview changes or the component unmounts (no leaks).
  useEffect(() => {
    return () => {
      if (preview) URL.revokeObjectURL(preview);
    };
  }, [preview]);

  function handleFile(file: File | undefined) {
    setError(null);
    if (!file) return;
    if (!ACCEPTED.includes(file.type)) {
      setError("Please choose a JPEG, PNG, or WebP image.");
      return;
    }
    if (file.size > MAX_BYTES) {
      setError("That image is too large (max 15 MB).");
      return;
    }
    if (preview) URL.revokeObjectURL(preview);
    setPreview(URL.createObjectURL(file));
    onChange(file);
  }

  function clear() {
    if (preview) URL.revokeObjectURL(preview);
    setPreview(null);
    setError(null);
    onChange(null);
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <div className="space-y-3">
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED.join(",")}
        capture="environment"
        className="sr-only"
        aria-label="Choose a photo"
        disabled={disabled}
        onChange={(e) => handleFile(e.target.files?.[0])}
      />

      {preview ? (
        <div className="relative overflow-hidden rounded-2xl border border-line">
          <img src={preview} alt="Selected symptom photo" className="max-h-72 w-full object-contain bg-canvas" />
          <button
            type="button"
            onClick={clear}
            disabled={disabled}
            aria-label="Remove photo"
            className="absolute right-2 top-2 inline-flex size-8 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/80"
          >
            <X className="size-4" />
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={disabled}
          className="flex w-full flex-col items-center justify-center gap-2 rounded-2xl border border-dashed border-line bg-white/60 px-6 py-10 text-muted transition-colors hover:border-brand-300 hover:bg-brand-50 disabled:opacity-50"
        >
          <span className="inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Camera className="size-6" />
          </span>
          <span className="text-sm font-medium text-brand-900">Take or choose a photo</span>
          <span className="text-xs">A rash, cut, swelling, or anything you want a physician to see.</span>
        </button>
      )}

      {error && (
        <p role="alert" className="text-sm text-[--color-urgency-emergency]">
          {error}
        </p>
      )}

      {preview && (
        <Button type="button" variant="ghost" size="sm" onClick={clear} disabled={disabled}>
          Choose a different photo
        </Button>
      )}
    </div>
  );
}
