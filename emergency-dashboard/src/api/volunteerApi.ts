import type { Volunteer } from "../types/volunteer";
import { apiFetch, API_URL } from "./client";

/** GET /api/volunteers — the volunteer roster from the backend. */
export async function getVolunteers(): Promise<Volunteer[]> {
  const response = await apiFetch(`${API_URL}/api/volunteers`);

  if (!response.ok) {
    throw new Error(`Failed to fetch volunteers (HTTP ${response.status})`);
  }

  const data: { success: boolean; volunteers: Volunteer[] } = await response.json();

  return data.volunteers ?? [];
}
