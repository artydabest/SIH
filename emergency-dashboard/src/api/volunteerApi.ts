import type { Volunteer } from "../types/volunteer";

const API_URL: string = import.meta.env.VITE_API_URL ?? "http://localhost:3000";

/** GET /api/volunteers — the volunteer roster from the backend. */
export async function getVolunteers(): Promise<Volunteer[]> {
  const response = await fetch(`${API_URL}/api/volunteers`);

  if (!response.ok) {
    throw new Error(`Failed to fetch volunteers (HTTP ${response.status})`);
  }

  const data: { success: boolean; volunteers: Volunteer[] } = await response.json();

  return data.volunteers ?? [];
}
