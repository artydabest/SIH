import type { SafeZone } from "../types/safezone";

const API_URL: string = import.meta.env.VITE_API_URL ?? "http://localhost:3000";

/** GET /api/safezones — shelters/hospitals/camps from the backend. */
export async function getSafeZones(): Promise<SafeZone[]> {
  const response = await fetch(`${API_URL}/api/safezones`);

  if (!response.ok) {
    throw new Error(`Failed to fetch safe zones (HTTP ${response.status})`);
  }

  const data: { success: boolean; safeZones: SafeZone[] } = await response.json();

  return data.safeZones ?? [];
}
