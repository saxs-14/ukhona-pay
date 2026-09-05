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
    const message = error.response?.data?.message || error.message || "Something went wrong";
    return Promise.reject(new Error(message));
  }
);

export default client;
