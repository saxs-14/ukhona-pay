import { createContext, useContext, useEffect, useState } from "react";
import client from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem("ukp_user");
    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem("ukp_user", JSON.stringify(user));
    } else {
      localStorage.removeItem("ukp_user");
    }
  }, [user]);

  async function login(phoneNumber, pin) {
    const { data } = await client.post("/auth/login", { phoneNumber, pin });
    localStorage.setItem("ukp_token", data.token);
    setUser(data);
    return data;
  }

  async function signup(payload) {
    const { data } = await client.post("/auth/signup", payload);
    localStorage.setItem("ukp_token", data.token);
    setUser(data);
    return data;
  }

  function logout() {
    localStorage.removeItem("ukp_token");
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
