import type { SafeZone } from "../types/safezone";
import { apiFetch, API_URL } from "./client";

/** GET /api/safezones — shelters/hospitals/camps from the backend. */
export async function getSafeZones(): Promise<SafeZone[]> {
  const response = await apiFetch(`${API_URL}/api/safezones`);

  if (!response.ok) {
    throw new Error(`Failed to fetch safe zones (HTTP ${response.status})`);
  }

  const data: { success: boolean; safeZones: SafeZone[] } = await response.json();

  return data.safeZones ?? [];
}
