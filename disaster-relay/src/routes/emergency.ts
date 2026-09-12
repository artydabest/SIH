import express from "express";
import Emergency from "../models/Emergency";
import { calculateConfidence } from "../utils/confidence";
import { broadcastNewEmergency, broadcastStatusUpdate } from "../socket";

const router = express.Router();

interface CreateEmergencyBody {
  deviceId?: unknown;
  latitude?: unknown;
  longitude?: unknown;
  altitude?: unknown;
  stationaryMinutes?: unknown;
  nearbyDevices?: unknown;
  emergencyMode?: unknown;
}

const isFiniteNumber = (value: unknown): value is number =>
  typeof value === "number" && Number.isFinite(value);

const asOptionalNumber = (value: unknown): number | undefined =>
  isFiniteNumber(value) ? value : undefined;

/** Validate before hitting Mongo so clients get precise 400s instead of cast errors. */
function parseEmergencyBody(
  body: CreateEmergencyBody
): { ok: true; data: Record<string, unknown> } | { ok: false; error: string } {
  const { deviceId, latitude, longitude, stationaryMinutes, nearbyDevices } = body;

  if (typeof deviceId !== "string" || deviceId.trim().length === 0) {
    return { ok: false, error: "deviceId (non-empty string) is required" };
  }
  if (!isFiniteNumber(latitude) || latitude < -90 || latitude > 90) {
    return { ok: false, error: "latitude must be a number between -90 and 90" };
  }
  if (!isFiniteNumber(longitude) || longitude < -180 || longitude > 180) {
    return { ok: false, error: "longitude must be a number between -180 and 180" };
  }
  if (!isFiniteNumber(stationaryMinutes) || stationaryMinutes < 0) {
    return { ok: false, error: "stationaryMinutes must be a non-negative number" };
  }
  if (
    !isFiniteNumber(nearbyDevices) ||
    nearbyDevices < 0 ||
    !Number.isInteger(nearbyDevices)
  ) {
    return { ok: false, error: "nearbyDevices must be a non-negative integer" };
  }

  const altitude = asOptionalNumber(body.altitude);
  if (body.altitude !== undefined && altitude === undefined) {
    return { ok: false, error: "altitude must be a number when provided" };
  }

  return {
    ok: true,
    data: {
      deviceId: deviceId.trim(),
      latitude,
      longitude,
      stationaryMinutes,
      nearbyDevices,
      emergencyMode: body.emergencyMode === true,
      ...(altitude !== undefined ? { altitude } : {}),
    },
  };
}

router.post("/", async (req, res) => {
  const parsed = parseEmergencyBody(req.body ?? {});

  if (!parsed.ok) {
    return res.status(400).json({
      success: false,
      message: parsed.error,
    });
  }

  try {
    const { score, level } = calculateConfidence({
      stationaryMinutes: parsed.data.stationaryMinutes as number,
      nearbyDevices: parsed.data.nearbyDevices as number,
      emergencyMode: parsed.data.emergencyMode as boolean,
    });

    const emergency = await Emergency.create({
      ...parsed.data,
      confidence: score,
      confidenceLevel: level,
    });

    console.log("EMERGENCY STORED:", emergency.deviceId, `(${level})`);

    // Push to all dashboards instantly instead of waiting for their poll cycle.
    broadcastNewEmergency(emergency);

    res.status(201).json({
      success: true,
      message: "Emergency stored",
      emergency,
    });
  } catch (error) {
    console.error("Error creating emergency:", error);
    res.status(500).json({
      success: false,
      message: "Failed to store emergency",
    });
  }
});
router.get("/", async (req, res) => {
  try {
    const emergencies = await Emergency.find().sort({ createdAt: -1 });

    res.json({
      success: true,
      emergencies,
    });
  } catch (error) {
    console.error("Error fetching emergencies:", error);

    res.status(500).json({
      success: false,
      message: "Failed to fetch emergencies",
    });
  }
});
const VALID_STATUSES = ["NEW", "ACKNOWLEDGED", "RESPONDING", "RESOLVED"] as const;

router.patch("/:id/status", async (req, res) => {
  const { status } = req.body ?? {};

  if (
    typeof status !== "string" ||
    !(VALID_STATUSES as readonly string[]).includes(status)
  ) {
    return res.status(400).json({
      success: false,
      message: `status must be one of: ${VALID_STATUSES.join(", ")}`,
    });
  }

  try {
    const emergency = await Emergency.findByIdAndUpdate(
      req.params.id,
      { status },
      { new: true }
    );

    if (!emergency) {
      return res.status(404).json({
        success: false,
        message: "Emergency not found",
      });
    }

    broadcastStatusUpdate(emergency);

    res.json({
      success: true,
      message: "Emergency status updated",
      emergency,
    });
  } catch (error) {
    console.error("Error updating emergency status:", error);

    res.status(400).json({
      success: false,
      message: "Invalid status update",
    });
  }
});
export default router;