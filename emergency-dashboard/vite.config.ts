import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  build: {
    // Keep the leafmap on its own chunk so it is loaded only when the
    // /map route is visited. React/router stay in the main app chunk.
    rollupOptions: {
      output: {
        manualChunks(id) {
          // Split out the heavy map libraries into their own chunk.
          if (id.includes("leaflet") || id.includes("react-leaflet")) {
            return "leafmap";
          }
          return undefined;
        },
      },
    },
  },
});
