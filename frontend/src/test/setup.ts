// Vitest global setup: register jest-dom matchers (toBeInTheDocument, etc.) and
// reset the DOM between tests.
import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// jsdom does not implement the Blob-URL object-URL methods. Components that preview media
// (PhotoCapture, VoiceRecorder, the .ics download) call these — including from React unmount
// cleanup, which runs AFTER a test's local vi.unstubAllGlobals(). Define them on the real URL so
// the methods survive teardown and never become "not a function" mid-cleanup.
if (typeof URL.createObjectURL !== "function") {
  URL.createObjectURL = () => "blob:test";
}
if (typeof URL.revokeObjectURL !== "function") {
  URL.revokeObjectURL = () => {};
}

// jsdom does not implement scrollIntoView; the chat auto-scrolls to the latest message.
if (typeof Element.prototype.scrollIntoView !== "function") {
  Element.prototype.scrollIntoView = () => {};
}

afterEach(() => {
  cleanup();
});
