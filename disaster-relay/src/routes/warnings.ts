import { Router } from "express";
import Warning, { type WarningDoc } from "../models/Warning";
import {
  DISASTER_INSTRUCTIONS,
  SIMULATED_REGIONS,
  findRegion,
  type DisasterInstructions,
  type WarningDisasterType,
  type WarningSeverity,
} from "../data/disasterCatalog";
import { broadcastNewWarning, broadcastWarningUpdate } from "../socket";

const router = Router();

const VALID_TYPES = Object.keys(DISASTER_INSTRUCTIONS) as WarningDisasterType[];
const VALID_SEVERITIES: WarningSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

const isFiniteNumber = (v: unknown): v is number =>
  typeof v === "number" && Number.isFinite(v);

/** Serialize a WarningDoc for clients (adds a computed `active` flag). */
export function serializeWarning(warning: WarningDoc) {
  return {
    ...warning.toObject(),
    active: warning.isActive(),
  };
}

/**
 * POST /api/warnings — issue a simulated meteorological warning.
 * Body: { type, severity, regionId, title?, description?, expectedStartInMin?, durationMin? }
 * The source is always the clearly-labeled simulator, never a real agency.
 * Instructions are attached server-side from the disaster catalog.
 */
router.post("/", async (req, res) => {
  const body = (req.body ?? {}) as Record<string, unknown>;

  const { type, severity, regionId, title, description } = body;

  if (typeof type !== "string" || !(VALID_TYPES as string[]).includes(type)) {
    return res.status(400).json({
      success: false,
      message: `type must be one of: ${VALID_TYPES.join(", ")}`,
    });
  }
  if (typeof severity !== "string" || !(VALID_SEVERITIES as string[]).includes(severity)) {
    return res.status(400).json({
      success: false,
      message: `severity must be one of: ${VALID_SEVERITIES.join(", ")}`,
    });
  }
  const region = typeof regionId === "string" ? findRegion(regionId) : undefined;
  if (!region) {
    return res.status(400).json({
      success: false,
      message: `regionId must be one of: ${SIMULATED_REGIONS.map((r) => r.id).join(", ")}`,
    });
  }
  // Geographic plausibility guard: e.g. tsunami warnings for landlocked
  // Bengaluru are rejected so demo data never reads as nonsense.
  if (!region.plausibleTypes.includes(type as WarningDisasterType)) {
    return res.status(400).json({
      success: false,
      message: `${type} warnings are not plausible for ${region.name}. Plausible types: ${region.plausibleTypes.join(", ")}`,
    });
  }
  if (title !== undefined && (typeof title !== "string" || title.trim().length === 0)) {
    return res.status(400).json({ success: false, message: "title must be a non-empty string when provided" });
  }
  if (
    description !== undefined &&
    (typeof description !== "string" || description.trim().length === 0)
  ) {
    return res.status(400).json({ success: false, message: "description must be a non-empty string when provided" });
  }

  // Advance-warning window: how long until the hazard is expected to start.
  const expectedStartInMin = isFiniteNumber(body.expectedStartInMin)
    ? Math.max(0, Math.min(body.expectedStartInMin, 72 * 60))
    : 0;
  // Hazard duration in minutes (defaults to 6 h).
  const durationMin = isFiniteNumber(body.durationMin)
    ? Math.max(15, Math.min(body.durationMin, 72 * 60))
    : 6 * 60;

  const now = Date.now();
  const expectedStartAt = new Date(now + expectedStartInMin * 60_000);
  const expectedEndAt = new Date(expectedStartAt.getTime() + durationMin * 60_000);

  const instructions: DisasterInstructions =
    DISASTER_INSTRUCTIONS[type as WarningDisasterType];

  // Validation above guarantees these are catalog members.
  const warningType = type as WarningDisasterType;
  const warningSeverity = severity as WarningSeverity;

  try {
    const warning = await Warning.create({
      source: "Meteorological Alert Simulator (DEMO)",
      type: warningType,
      severity: warningSeverity,
      regionId: region.id,
      regionName: region.name,
      latitude: region.latitude,
      longitude: region.longitude,
      radiusKm: region.radiusKm,
      title:
        typeof title === "string" && title.trim().length > 0
          ? title.trim()
          : `${TYPE_LABEL[warningType]} ${SEVERITY_LABEL[warningSeverity]} — ${region.name}`,
      description:
        typeof description === "string" && description.trim().length > 0
          ? description.trim()
          : DEFAULT_DESCRIPTIONS[warningType] ??
            `A ${TYPE_LABEL[warningType].toLowerCase()} event is expected in ${region.name}.`,
      issuedAt: new Date(now),
      expectedStartAt,
      expectedEndAt,
      instructions,
      status: "ACTIVE",
    });

    broadcastNewWarning(serializeWarning(warning));

    res.status(201).json({
      success: true,
      message: "Warning issued and broadcast to connected clients",
      warning: serializeWarning(warning),
    });
  } catch (error) {
    console.error("Error issuing warning:", error);
    res.status(500).json({ success: false, message: "Failed to issue warning" });
  }
});

/** GET /api/warnings?active=1&limit=50 — warnings newest first; auto-expires stale ones. */
router.get("/", async (req, res) => {
  try {
    const limit = Math.min(Number(req.query.limit) || 50, 100);

    await expireStaleWarnings();

    const filter: Record<string, unknown> =
      req.query.active === "1" || req.query.active === "true"
        ? { status: "ACTIVE", expectedEndAt: { $gt: new Date() } }
        : {};

    const warnings = await Warning.find(filter)
      .sort({ issuedAt: -1 })
      .limit(limit);

    res.json({
      success: true,
      warnings: warnings.map(serializeWarning),
    });
  } catch (error) {
    console.error("Error fetching warnings:", error);
    res.status(500).json({ success: false, message: "Failed to fetch warnings" });
  }
});

/** GET /api/warnings/regions — the simulated region catalog for pickers/maps. */
router.get("/regions", (req, res) => {
  res.json({ success: true, regions: SIMULATED_REGIONS });
});

/** GET /api/warnings/instructions/:type — per-disaster instruction set. */
router.get("/instructions/:type", (req, res) => {
  const { type } = req.params;
  const instructions = DISASTER_INSTRUCTIONS[type as WarningDisasterType];
  if (!instructions) {
    return res.status(404).json({
      success: false,
      message: `Unknown disaster type. Valid types: ${VALID_TYPES.join(", ")}`,
    });
  }
  res.json({ success: true, type, instructions });
});

/** PATCH /api/warnings/:id/cancel — withdraw a warning (demo control). */
router.patch("/:id/cancel", async (req, res) => {
  try {
    const warning = await Warning.findByIdAndUpdate(
      req.params.id,
      { status: "CANCELLED" },
      { new: true }
    );
    if (!warning) {
      return res.status(404).json({ success: false, message: "Warning not found" });
    }
    broadcastWarningUpdate(serializeWarning(warning));
    res.json({ success: true, message: "Warning cancelled", warning: serializeWarning(warning) });
  } catch (error) {
    console.error("Error cancelling warning:", error);
    res.status(500).json({ success: false, message: "Failed to cancel warning" });
  }
});

/** Flip warnings past their expected end to EXPIRED (idempotent, cheap). */
export async function expireStaleWarnings(): Promise<void> {
  await Warning.updateMany(
    { status: "ACTIVE", expectedEndAt: { $lte: new Date() } },
    { status: "EXPIRED" }
  );
}

const SEVERITY_LABEL: Record<WarningSeverity, string> = {
  LOW: "Advisory",
  MEDIUM: "Watch",
  HIGH: "Warning",
  CRITICAL: "Emergency",
};

const TYPE_LABEL: Record<WarningDisasterType, string> = {
  CYCLONE: "Cyclone",
  FLOOD: "Flood",
  HEAVY_RAINFALL: "Heavy Rainfall",
  THUNDERSTORM: "Thunderstorm",
  HEATWAVE: "Heatwave",
  LIGHTNING: "Lightning",
  LANDSLIDE: "Landslide",
  EARTHQUAKE: "Earthquake",
  TSUNAMI: "Tsunami",
};

const DEFAULT_DESCRIPTIONS: Partial<Record<WarningDisasterType, string>> = {
  HEAVY_RAINFALL:
    "Persistent heavy rainfall is expected, with waterlogging likely in low-lying areas and reduced visibility on roads.",
  THUNDERSTORM:
    "Thunderstorms with gusty winds are expected. Power interruptions and localised tree falls are possible.",
  LIGHTNING:
    "Frequent cloud-to-ground lightning is expected. Keep away from open ground and tall isolated structures.",
  HEATWAVE:
    "Heatwave conditions are expected with above-normal temperatures. Limit outdoor exposure during peak hours.",
  FLOOD:
    "Flooding of low-lying areas is expected. Roads and underpasses may become impassable.",
  CYCLONE:
    "A cyclonic circulation is expected to affect the coast with squally winds and heavy rain.",
  LANDSLIDE:
    "Slope movement is possible after intense rainfall. Unstable roads and debris flows may block routes.",
  EARTHQUAKE:
    "Seismic activity has been simulated in this region for the demo. Follow drop, cover, hold on guidance.",
  TSUNAMI:
    "An undersea disturbance has been simulated for the demo. Coastal inundation is possible; move inland.",
};

export default router;
