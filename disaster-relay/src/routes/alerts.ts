import { Router } from "express";
import Alert, { SAFETY_INSTRUCTIONS, type DisasterType } from "../models/Alert";
import { broadcastAlert } from "../socket";

const router = Router();

const VALID_TYPES: DisasterType[] = [
  "EARTHQUAKE",
  "FLOOD",
  "CYCLONE",
  "FIRE",
  "LANDSLIDE",
  "OTHER",
];

const isFiniteNumber = (v: unknown): v is number =>
  typeof v === "number" && Number.isFinite(v);

/** GET /api/alerts?active=1 — current alerts (newest first). */
router.get("/", async (req, res) => {
  try {
    const filter =
      req.query.active === "1" || req.query.active === "true"
        ? { active: true }
        : {};

    const alerts = await Alert.find(filter).sort({ createdAt: -1 }).limit(20);

    res.json({
      success: true,
      alerts,
    });
  } catch (error) {      console.error("Error fetching alerts:", error);
    res.status(500).json({ success: false, message: "Failed to fetch alerts" });
  }
});

/**
 * POST /api/alerts — trigger a disaster alert (drill or real).
 * Body: { type, message?, latitude?, longitude?, radiusKm? }
 * The backend attaches the per-disaster safety instructions and broadcasts
 * to every connected app and dashboard instantly.
 */
router.post("/", async (req, res) => {
  const { type, message, latitude, longitude, radiusKm } = (req.body ?? {}) as {
    type?: unknown;
    message?: unknown;
    latitude?: unknown;
    longitude?: unknown;
    radiusKm?: unknown;
  };

  if (typeof type !== "string" || !(VALID_TYPES as string[]).includes(type)) {
    return res.status(400).json({
      success: false,
      message: `type must be one of: ${VALID_TYPES.join(", ")}`,
    });
  }

  if (latitude !== undefined && (!isFiniteNumber(latitude) || latitude < -90 || latitude > 90)) {
    return res.status(400).json({ success: false, message: "latitude must be between -90 and 90" });
  }
  if (longitude !== undefined && (!isFiniteNumber(longitude) || longitude < -180 || longitude > 180)) {
    return res.status(400).json({ success: false, message: "longitude must be between -180 and 180" });
  }

  try {
    const doc: Record<string, unknown> = {
      type,
      message:
        typeof message === "string" && message.trim().length > 0
          ? message.trim()
          : `${type} alert issued for your area`,
      active: true,
    };
    if (isFiniteNumber(latitude)) doc.latitude = latitude;
    if (isFiniteNumber(longitude)) doc.longitude = longitude;
    if (isFiniteNumber(radiusKm) && radiusKm > 0) doc.radiusKm = radiusKm;

    const alert = await Alert.create(doc);

    const payload = {
      ...alert.toObject(),
      instructions: SAFETY_INSTRUCTIONS[type as DisasterType],
    };

    broadcastAlert(payload);      console.log(`ALERT ISSUED: ${type} → all connected clients`);

    res.status(201).json({
      success: true,
      message: "Alert broadcast to all clients",
      alert: payload,
    });
  } catch (error) {      console.error("Error creating alert:", error);
    res.status(500).json({ success: false, message: "Failed to create alert" });
  }
});

/** PATCH /api/alerts/:id/deactivate — all-clear. */
router.patch("/:id/deactivate", async (req, res) => {
  try {
    const alert = await Alert.findByIdAndUpdate(
      req.params.id,
      { active: false },
      { new: true }
    );

    if (!alert) {
      return res.status(404).json({ success: false, message: "Alert not found" });
    }

    res.json({ success: true, message: "Alert deactivated", alert });
  } catch (error) {      console.error("Error deactivating alert:", error);
    res.status(500).json({ success: false, message: "Failed to deactivate alert" });
  }
});

export default router;
