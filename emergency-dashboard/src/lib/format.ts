import type { EmergencyStatus } from "../types/emergency";

/** Monospace CSS class for technical data (IDs, coords, timestamps). */
export const MONO = "mono";
export function formatCoord(value: number, digits = 4): string {
  return value.toFixed(digits);
}

export function formatCoords(lat: number, lon: number, digits = 4): string {
  return `${formatCoord(lat, digits)}, ${formatCoord(lon, digits)}`;
}

export function formatAltitude(
  altitude: number | null | undefined
): string {
  if (altitude == null || Number.isNaN(altitude)) return "—";
  return `${altitude} m`;
}

export function formatStationary(minutes: number): string {
  if (!Number.isFinite(minutes)) return "—";
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest > 0 ? `${hours} h ${rest} min` : `${hours} h`;
}

export function formatTimestampFull(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

/** "32 sec ago" / "5 min ago" style relative time. */
export function formatRelative(iso: string, now: number = Date.now()): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return "unknown";
  const seconds = Math.max(0, Math.round((now - then) / 1000));
  if (seconds < 5) return "just now";
  if (seconds < 60) return `${seconds} sec ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} h ${minutes % 60} min ago`;
  return `${Math.floor(hours / 24)} d ago`;
}

/** OpenStreetMap deep link for a coordinate. */
export function osmUrl(lat: number, lon: number, zoom = 16): string {
  return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=${zoom}/${lat}/${lon}`;
}

/** Urgency bucket used for sorting and severity chips (derived, honest). */
export function severityOf(
  status: EmergencyStatus
): "HIGH" | "ELEVATED" | "ACTIVE" | "CLOSED" {
  if (status === "RESOLVED") return "CLOSED";
  if (status === "NEW") return "HIGH";
  if (status === "ACKNOWLEDGED") return "ELEVATED";
  return "ACTIVE";
}
