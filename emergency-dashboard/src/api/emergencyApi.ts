import type { Emergency, EmergencyStatus } from "../types/emergency";
import { apiFetch, API_URL } from "./client";

export { API_URL };

export async function getEmergencies(): Promise<Emergency[]> {
  const response = await apiFetch(`${API_URL}/api/emergency`);

  if (!response.ok) {
    throw new Error(`Failed to fetch emergencies (HTTP ${response.status})`);
  }

  const data: { success: boolean; emergencies: Emergency[] } =
    await response.json();

  return data.emergencies ?? [];
}

export async function updateEmergencyStatus(
  id: string,
  status: EmergencyStatus
): Promise<Emergency> {
  const response = await apiFetch(`${API_URL}/api/emergency/${id}/status`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ status }),
  });

  if (!response.ok) {
    throw new Error(`Failed to update emergency status (HTTP ${response.status})`);
  }

  const data: {
    success: boolean;
    emergency: Emergency;
  } = await response.json();

  return data.emergency;
}
