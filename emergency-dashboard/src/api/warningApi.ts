import type { SimWarning, SimRegion, WarningDisasterType } from "../types/warning";
import { apiFetch, API_URL } from "./client";

/** GET /api/warnings?active=1 — simulated warnings, newest first. */
export async function getWarnings(activeOnly = false): Promise<SimWarning[]> {
  const response = await apiFetch(
    `${API_URL}/api/warnings${activeOnly ? "?active=1" : ""}`
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch warnings (HTTP ${response.status})`);
  }

  const data: { success: boolean; warnings: SimWarning[] } = await response.json();
  return data.warnings ?? [];
}

/** GET /api/warnings/regions — simulated region catalog (for the simulator form). */
export async function getWarningRegions(): Promise<SimRegion[]> {
  const response = await apiFetch(`${API_URL}/api/warnings/regions`);

  if (!response.ok) {
    throw new Error(`Failed to fetch warning regions (HTTP ${response.status})`);
  }

  const data: { success: boolean; regions: SimRegion[] } = await response.json();
  return data.regions ?? [];
}

/**
 * POST /api/warnings — issue a simulated meteorological warning.
 * The backend validates type/region plausibility and attaches instructions.
 */
export async function issueWarning(input: {
  type: WarningDisasterType;
  severity: string;
  regionId: string;
  message?: string;
  expectedStartInMin?: number;
  durationMin?: number;
}): Promise<SimWarning> {
  const response = await apiFetch(`${API_URL}/api/warnings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });

  if (!response.ok) {
    // Surface the backend's precise validation message when available.
    let detail = `HTTP ${response.status}`;
    try {
      const body: { message?: string } = await response.json();
      if (body.message) detail = body.message;
    } catch {
      // non-JSON error body — keep the HTTP code
    }
    throw new Error(detail);
  }

  const data: { success: boolean; warning: SimWarning } = await response.json();
  return data.warning;
}

/** PATCH /api/warnings/:id/cancel — withdraw a simulated warning. */
export async function cancelWarning(id: string): Promise<void> {
  const response = await apiFetch(`${API_URL}/api/warnings/${id}/cancel`, {
    method: "PATCH",
  });

  if (!response.ok) {
    throw new Error(`Failed to cancel warning (HTTP ${response.status})`);
  }
}
