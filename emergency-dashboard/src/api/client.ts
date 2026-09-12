/**
 * Shared fetch wrapper for backend calls.
 * - Attaches X-API-Key when the dashboard is configured with one
 *   (set VITE_API_KEY; the backend reads API_KEY).
 * - Single place to surface HTTP failures consistently.
 */
const API_URL: string = import.meta.env.VITE_API_URL ?? "http://localhost:3000";

const API_KEY: string | undefined = import.meta.env.VITE_API_KEY || undefined;

export function apiHeaders(extra: Record<string, string> = {}): Record<string, string> {
  return {
    ...(API_KEY ? { "X-API-Key": API_KEY } : {}),
    ...extra,
  };
}

export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  return fetch(input, {
    ...init,
    headers: apiHeaders(init.headers as Record<string, string> | undefined),
  });
}

export { API_URL };
