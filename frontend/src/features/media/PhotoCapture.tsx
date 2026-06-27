import { useRef, useState, useEffect } from "react";
import { Camera, ImagePlus, X, Aperture } from "lucide-react";
import { Button } from "@/components/ui/button";

// Content types the backend accepts for images (mirrors MediaContentType on the server).
const ACCEPTED = ["image/jpeg", "image/png", "image/webp"];
const MAX_BYTES = 15 * 1024 * 1024;

interface PhotoCaptureProps {
  // Called with the chosen image, or null when cleared.
  onChange: (file: File | null) => void;
  disabled?: boolean;
}

type Mode = "idle" | "camera" | "ready";

/**
 * Photo intake control with two ways in: take a photo with the device camera (live preview +
 * shutter, via getUserMedia — works on desktop and mobile) or choose one from the gallery. Both
 * land on the same preview. Validates type and size on the client; the server validates again.
 */
export function PhotoCapture({ onChange, disabled }: PhotoCaptureProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [mode, setMode] = useState<Mode>("idle");
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function stopCamera() {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  }

  // Clean up the camera stream and the preview object URL on unmount.
  useEffect(() => {
    return () => {
      stopCamera();
      if (preview) URL.revokeObjectURL(preview);
    };
  }, [preview]);

  function setImage(file: File) {
    setError(null);
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
    setMode("ready");
    onChange(file);
  }

  async function startCamera() {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
        audio: false,
      });
      streamRef.current = stream;
      setMode("camera");
      // The video element renders once mode === "camera"; attach the stream on the next tick.
      requestAnimationFrame(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          void videoRef.current.play();
        }
      });
    } catch {
      stopCamera();
      setError("Camera unavailable. Allow camera access, or choose a photo from your gallery.");
      setMode("idle");
    }
  }

  function capture() {
    const video = videoRef.current;
    if (!video || !video.videoWidth) return;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d")?.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        if (blob) setImage(new File([blob], "photo.jpg", { type: "image/jpeg" }));
        stopCamera();
      },
      "image/jpeg",
      0.92,
    );
  }

  function cancelCamera() {
    stopCamera();
    setMode("idle");
  }

  function clear() {
    if (preview) URL.revokeObjectURL(preview);
    setPreview(null);
    setError(null);
    setMode("idle");
    onChange(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  return (
    <div className="space-y-3">
      <input
        ref={fileInputRef}
        type="file"
        accept={ACCEPTED.join(",")}
        className="sr-only"
        aria-label="Choose a photo from your gallery"
        disabled={disabled}
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) setImage(f);
        }}
      />

      {mode === "ready" && preview ? (
        <div className="relative overflow-hidden rounded-2xl border border-line">
          <img src={preview} alt="Selected symptom photo" className="max-h-72 w-full bg-canvas object-contain" />
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
      ) : mode === "camera" ? (
        <div className="space-y-3">
          <div className="overflow-hidden rounded-2xl border border-line bg-black">
            <video ref={videoRef} className="max-h-72 w-full object-contain" autoPlay playsInline muted />
          </div>
          <div className="flex items-center justify-center gap-3">
            <Button type="button" onClick={capture} disabled={disabled}>
              <Aperture className="size-4" /> Capture
            </Button>
            <Button type="button" variant="ghost" onClick={cancelCamera} disabled={disabled}>
              Cancel
            </Button>
          </div>
        </div>
      ) : (
        <div className="rounded-2xl border border-dashed border-line bg-white/60 px-6 py-8 text-center">
          <span className="mx-auto mb-3 inline-flex size-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Camera className="size-6" />
          </span>
          <p className="text-sm font-medium text-brand-900">Add a photo</p>
          <p className="mb-4 text-xs text-muted">A rash, cut, swelling, or anything you want a physician to see.</p>
          <div className="flex flex-wrap items-center justify-center gap-2">
            <Button type="button" onClick={startCamera} disabled={disabled}>
              <Camera className="size-4" /> Take photo
            </Button>
            <Button type="button" variant="outline" onClick={() => fileInputRef.current?.click()} disabled={disabled}>
              <ImagePlus className="size-4" /> Choose from gallery
            </Button>
          </div>
        </div>
      )}

      {error && (
        <p role="alert" className="text-sm text-[--color-urgency-emergency]">
          {error}
        </p>
      )}

      {mode === "ready" && (
        <Button type="button" variant="ghost" size="sm" onClick={clear} disabled={disabled}>
          Retake or choose a different photo
        </Button>
      )}
    </div>
  );
}
