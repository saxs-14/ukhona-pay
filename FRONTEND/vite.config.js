import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Listen on the LAN too, so judges can browse the app from their own device.
    // Note: camera QR scanning will NOT work over a LAN IP (browsers only treat
    // "localhost" as a secure context without HTTPS) — test scanning via two tabs
    // on the demo laptop itself using localhost, see DOCS/DEMO_DAY_CHECKLIST.md.
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
