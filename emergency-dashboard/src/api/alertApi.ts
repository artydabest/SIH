import type { DisasterAlert, DisasterType } from "../types/alert";

const API_URL: string = import.meta.env.VITE_API_URL ?? "http://localhost:3000";

/** POST /api/alerts — broadcast a disaster drill to every connected client. */
export async function triggerAlert(
  type: DisasterType,
  options: { message?: string; latitude?: number; longitude?: number; radiusKm?: number } = {}
): Promise<DisasterAlert> {
  const response = await fetch(`${API_URL}/api/alerts`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ type, ...options }),
  });

  if (!response.ok) {
    throw new Error(`Failed to trigger alert (HTTP ${response.status})`);
  }

  const data: { success: boolean; alert: DisasterAlert } = await response.json();
  return data.alert;
}

/** GET /api/alerts?active=1 — newest active alert, if any. */
export async function getActiveAlert(): Promise<DisasterAlert | null> {
  const response = await fetch(`${API_URL}/api/alerts?active=1`);

  if (!response.ok) {
    throw new Error(`Failed to fetch alerts (HTTP ${response.status})`);
  }

  const data: { success: boolean; alerts: DisasterAlert[] } = await response.json();
  const latest = data.alerts[0] ?? null;
  return latest && latest.active ? latest : null;
}
