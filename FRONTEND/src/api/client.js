import axios from "axios";

// In local dev this stays relative ("/api") and goes through the Vite proxy
// to localhost:8080 (see vite.config.js). In production the frontend
// (Vercel) and backend (Cloud Run) are on different domains, so the Vercel
// project needs VITE_API_BASE_URL set to the backend's full URL, e.g.
// https://ukhona-backend-xxxxx.run.app/api - see DOCS/DEPLOYMENT.md.
const baseURL = import.meta.env.VITE_API_BASE_URL || "/api";

const client = axios.create({
  baseURL,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem("ukp_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    // Spring Security returns 403 (not 401) for a missing/invalid/expired
    // token on any protected endpoint - 401 here is only ever the login
    // endpoint rejecting a wrong PIN. A stale token left over in
    // localStorage (e.g. after the backend restarts with a new JWT
    // secret) was crashing every dashboard with "Cannot read properties
    // of null" instead of sending the user back to log in.
    if (error.response?.status === 403 && window.location.pathname !== "/login") {
      localStorage.removeItem("ukp_token");
      localStorage.removeItem("ukp_user");
      window.location.href = "/login";
    }
    const message = error.response?.data?.message || error.message || "Something went wrong";
    return Promise.reject(new Error(message));
  }
);

export default client;
