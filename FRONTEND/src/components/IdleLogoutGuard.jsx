import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

// Money app: auto sign out after a minute of no interaction, or after being
// away (backgrounded tab/app) for a minute or more - whichever happens
// first. Renders nothing; just watches activity while a user is signed in.
const IDLE_TIMEOUT_MS = 60 * 1000;
const ACTIVITY_EVENTS = ["mousedown", "mousemove", "keydown", "touchstart", "wheel", "scroll"];

export default function IdleLogoutGuard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const timeoutRef = useRef(null);
  const hiddenAtRef = useRef(null);

  useEffect(() => {
    if (!user) return undefined;

    function doLogout() {
      logout();
      navigate("/login");
    }

    function resetTimer() {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(doLogout, IDLE_TIMEOUT_MS);
    }

    function handleVisibilityChange() {
      if (document.hidden) {
        hiddenAtRef.current = Date.now();
        return;
      }
      if (hiddenAtRef.current === null) return;
      const awayMs = Date.now() - hiddenAtRef.current;
      hiddenAtRef.current = null;
      if (awayMs >= IDLE_TIMEOUT_MS) {
        doLogout();
      } else {
        resetTimer();
      }
    }

    ACTIVITY_EVENTS.forEach((evt) => window.addEventListener(evt, resetTimer, { passive: true }));
    document.addEventListener("visibilitychange", handleVisibilityChange);
    resetTimer();

    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      ACTIVITY_EVENTS.forEach((evt) => window.removeEventListener(evt, resetTimer));
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  return null;
}
