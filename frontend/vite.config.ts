import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

// Builds the SPA into Spring's static dir so the backend serves it.
// emptyOutDir:false preserves static/banners_logo (brand assets live there).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: false,
    assetsDir: "assets",
  },
  server: {
    // Local dev: proxy API calls to the running Spring app.
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
