import { useEffect, useState } from "react";

/**
 * Returns a Date that re-renders every `intervalMs`. Used for
 * "last sync X sec ago" and relative timestamps. Cleans up on unmount.
 */
export function useNow(intervalMs = 1000): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), intervalMs);
    return () => clearInterval(timer);
  }, [intervalMs]);
  return now;
}
