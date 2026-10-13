import { defineConfig } from 'vite';

// In development the panel runs on :5173 and forwards API calls to the backend.
const target = process.env.VITE_DEV_API ?? 'http://localhost:8080';

export default defineConfig({
  server: {
    proxy: {
      '/api': target,
      '/login': target,
      '/register': target,
      '/setup-status': target,
      '/me': target,
      '/ws': { target, ws: true },
    },
  },
});
