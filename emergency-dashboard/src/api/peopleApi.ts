import type { Person } from "../types/person";
import { apiFetch, API_URL } from "./client";

/** GET /api/people — latest known position + safety status for every checked-in device. */
export async function getPeople(): Promise<Person[]> {
  const response = await apiFetch(`${API_URL}/api/people`);

  if (!response.ok) {
    throw new Error(`Failed to fetch people (HTTP ${response.status})`);
  }

  const data: { success: boolean; people: Person[] } = await response.json();

  return data.people ?? [];
}
