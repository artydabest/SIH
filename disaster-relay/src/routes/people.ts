import { Router } from "express";
import Person from "../models/Person";
import { broadcastPeopleUpdate, broadcastSafetyUpdate } from "../socket";

const router = Router();

const isFiniteNumber = (value: unknown): value is number =>
  typeof value === "number" && Number.isFinite(value);

interface CheckinBody {
  deviceId?: unknown;
  name?: unknown;
  latitude?: unknown;
  longitude?: unknown;
  safe?: unknown;
}

/**
 * POST /api/people/checkin — upsert a device's presence.
 * Every teammate's app calls this with its current location; all dashboards
 * and apps see everyone via GET /api/people.
 */
router.post("/checkin", async (req, res) => {
  const { deviceId, name, latitude, longitude, safe } = (req.body ?? {}) as CheckinBody;

  if (typeof deviceId !== "string" || deviceId.trim().length === 0) {
    return res.status(400).json({ success: false, message: "deviceId is required" });
  }
  if (!isFiniteNumber(latitude) || latitude < -90 || latitude > 90) {
    return res.status(400).json({ success: false, message: "latitude must be between -90 and 90" });
  }
  if (!isFiniteNumber(longitude) || longitude < -180 || longitude > 180) {
    return res.status(400).json({ success: false, message: "longitude must be between -180 and 180" });
  }

  try {
    const person = await Person.findOneAndUpdate(
      { deviceId: deviceId.trim() },
      {
        deviceId: deviceId.trim(),
        name: typeof name === "string" && name.trim().length > 0 ? name.trim() : undefined,
        latitude,
        longitude,
        safe: safe === false ? false : true,
        lastSeenAt: new Date(),
      },
      { new: true, upsert: true, setDefaultsOnInsert: true }
    );

    broadcastPeopleUpdate();
    broadcastSafetyUpdate();

    res.json({
      success: true,
      message: "Checked in",
      person,
    });
  } catch (error) {      console.error("Error checking in:", error);
    res.status(500).json({ success: false, message: "Failed to check in" });
  }
});

/** GET /api/people — everyone's latest known position and safety status. */
router.get("/", async (req, res) => {
  try {
    const people = await Person.find().sort({ lastSeenAt: -1 });

    res.json({
      success: true,
      people,
    });
  } catch (error) {      console.error("Error fetching people:", error);
    res.status(500).json({ success: false, message: "Failed to fetch people" });
  }
});

export default router;
