import type { DisasterAlert, DisasterType } from "../types/alert";
import { apiFetch, API_URL } from "./client";

/** POST /api/alerts — broadcast a disaster drill to every connected client. */
export async function triggerAlert(
  type: DisasterType,
  options: { message?: string; latitude?: number; longitude?: number; radiusKm?: number } = {}
): Promise<DisasterAlert> {
  const response = await apiFetch(`${API_URL}/api/alerts`, {
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
