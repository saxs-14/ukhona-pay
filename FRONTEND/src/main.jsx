import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'

// The service worker updates in the background (skipWaiting + clientsClaim -
// see vite.config.js) as soon as a new deploy is live, but an already-open
// tab keeps running the JS it already loaded into memory until an actual
// document reload happens - client-side route changes never re-fetch it.
// Without this, a returning user can click around indefinitely and never
// see a fix that's already live on the server. `controllerchange` fires
// exactly when the new service worker takes over, so this reloads once,
// right then, instead of leaving them stuck on stale code.
if ('serviceWorker' in navigator) {
  let reloadedForUpdate = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (reloadedForUpdate) return;
    reloadedForUpdate = true;
    window.location.reload();
  });
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
