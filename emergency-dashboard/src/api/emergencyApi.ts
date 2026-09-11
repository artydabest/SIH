import type { Emergency, EmergencyStatus } from "../types/emergency";

const API_URL: string = import.meta.env.VITE_API_URL ?? "http://localhost:3000";

export async function getEmergencies(): Promise<Emergency[]> {
  const response = await fetch(`${API_URL}/api/emergency`);

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
  const response = await fetch(`${API_URL}/api/emergency/${id}/status`, {
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

export { API_URL };
