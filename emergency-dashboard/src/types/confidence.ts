import type { ConfidenceLevel } from "./emergency";

/** Display metadata for each confidence level — the single source of truth. */
export const CONFIDENCE_META: Record<
  ConfidenceLevel,
  { label: string; className: string; rank: number }
> = {
  LOW: { label: "LOW", className: "low", rank: 0 },
  MEDIUM: { label: "MEDIUM", className: "medium", rank: 1 },
  HIGH: { label: "HIGH", className: "high", rank: 2 },
  CRITICAL: { label: "CRITICAL", className: "critical", rank: 3 },
};

/** Safe accessor for records created before confidence scoring existed. */
export function confidenceMeta(
  level: ConfidenceLevel | undefined
): (typeof CONFIDENCE_META)[ConfidenceLevel] | null {
  if (!level) return null;
  return CONFIDENCE_META[level] ?? null;
}
