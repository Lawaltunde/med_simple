/// <reference types="vitest/config" />
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

// Builds the SPA into Spring's static dir so the backend serves it.
// Brand assets are the source-of-truth in public/ and get copied into the build,
// so emptyOutDir:true keeps static/ a clean, fully-generated output.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true,
    assetsDir: "assets",
  },
  server: {
    // Local dev: proxy API calls to the running Spring app.
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
});
