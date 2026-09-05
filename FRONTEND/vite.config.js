import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      // Never register a service worker in dev - see DOCS/DEMO_DAY_CHECKLIST.md
      // for the exact incident (a stale SW from an unrelated project silently
      // hijacked localhost:5173) that makes this non-negotiable here.
      devOptions: { enabled: false },
      manifest: {
        name: 'Ukhona Pay',
        short_name: 'Ukhona Pay',
        description: "Financial identity for taxi-rank traders",
        theme_color: '#c2410c',
        background_color: '#fdf8f3',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: '/favicon.png', sizes: '500x500', type: 'image/png', purpose: 'any' },
        ],
      },
      workbox: {
        // Precache the built app shell (JS/CSS/fonts/images) so the app loads
        // with no network at all.
        globPatterns: ['**/*.{js,css,html,woff,woff2,png,svg,ico}'],
        navigateFallbackDenylist: [/^\/api\//],
        runtimeCaching: [
          {
            // Money-affecting data must never be served stale - every /api
            // call always hits the network, offline just surfaces as a
            // normal request failure rather than a silently wrong balance.
            urlPattern: /^\/api\//,
            handler: 'NetworkOnly',
          },
        ],
      },
    }),
  ],
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
