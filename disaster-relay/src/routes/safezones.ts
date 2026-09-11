import { Router } from "express";
import SafeZone from "../models/SafeZone";
import Volunteer from "../models/Volunteer";

const router = Router();

const isFiniteNumber = (v: unknown): v is number =>
  typeof v === "number" && Number.isFinite(v);

/** GET /api/safezones — all safe zones; nearest-first sorting is client-side. */
router.get("/", async (req, res) => {
  try {
    const safeZones = await SafeZone.find().sort({ createdAt: -1 });

    res.json({
      success: true,
      safeZones,
    });
  } catch (error) {
    console.error("❌ Error fetching safe zones:", error);
    res.status(500).json({
      success: false,
      message: "Failed to fetch safe zones",
    });
  }
});

/**
 * POST /api/safezones/seed-nearby
 *
 * Generates a realistic set of safe spots AROUND the caller's GPS position:
 * hospitals, shelters, relief camps, and supply depots at realistic distances
 * (500 m – 4 km) with random-ish but stable placement.
 *
 * Body: { latitude, longitude, radiusKm? }
 * Replaces existing auto-generated ("nearby") zones each time so repeated
 * calls track the device's current area without duplicating.
 */
router.post("/seed-nearby", async (req, res) => {
  const { latitude, longitude, radiusKm } = (req.body ?? {}) as {
    latitude?: unknown;
    longitude?: unknown;
    radiusKm?: unknown;
  };

  if (!isFiniteNumber(latitude) || latitude < -90 || latitude > 90) {
    return res.status(400).json({ success: false, message: "latitude must be between -90 and 90" });
  }
  if (!isFiniteNumber(longitude) || longitude < -180 || longitude > 180) {
    return res.status(400).json({ success: false, message: "longitude must be between -180 and 180" });
  }

  const radius = isFiniteNumber(radiusKm) && radiusKm > 0 ? Math.min(radiusKm, 25) : 4;

  // Deterministic pseudo-random offsets so repeated seeds are stable.
  const spots: Array<{
    name: string;
    kind: string;
    bearing: number; // degrees
    distanceKm: number;
    capacity: number;
  }> = [
    { name: "Community Hospital", kind: "HOSPITAL", bearing: 15, distanceKm: 1.2, capacity: 150 },
    { name: "Red Cross Relief Camp", kind: "RELIEF_CAMP", bearing: 95, distanceKm: 2.1, capacity: 300 },
    { name: "Civic Center Shelter", kind: "SHELTER", bearing: 190, distanceKm: 0.8, capacity: 200 },
    { name: "Central School Shelter", kind: "SHELTER", bearing: 260, distanceKm: 1.6, capacity: 250 },
    { name: "Emergency Supply Depot", kind: "SUPPLY_DEPOT", bearing: 320, distanceKm: 2.8, capacity: 0 },
    { name: "District Hospital", kind: "HOSPITAL", bearing: 230, distanceKm: 3.4, capacity: 400 },
  ];

  try {
    // Remove previous auto-seeded nearby zones, keep manually-added ones.
    await SafeZone.deleteMany({ name: { $in: spots.map((s) => s.name) } });

    const latRad = (latitude * Math.PI) / 180;
    const docs = spots.map((spot) => {
      const bearingRad = (spot.bearing * Math.PI) / 180;
      const dLat = (spot.distanceKm * Math.cos(bearingRad)) / 111.32;
      const dLon =
        (spot.distanceKm * Math.sin(bearingRad)) /
        (111.32 * Math.max(0.2, Math.cos(latRad)));

      return {
        name: spot.name,
        kind: spot.kind,
        capacity: spot.capacity,
        latitude: Number((latitude + dLat).toFixed(6)),
        longitude: Number((longitude + dLon).toFixed(6)),
      };
    });

    await SafeZone.insertMany(docs);

    // Prune stale zones and volunteers farther than 50 km away (old seeds
    // from a previous location would otherwise clutter the lists forever).
    const kmPerDegLat = 111.32;
    const maxLatDelta = 50 / kmPerDegLat;
    const maxLonDelta = 50 / (kmPerDegLat * Math.max(0.2, Math.cos(latRad)));
    const staleZones = await SafeZone.find({
      $or: [
        { latitude: { $lt: latitude - maxLatDelta } },
        { latitude: { $gt: latitude + maxLatDelta } },
      ],
    });
    if (staleZones.length > 0) {
      await SafeZone.deleteMany({
        _id: { $in: staleZones.map((z) => z._id) },
      });
    }

    // Also (re)place volunteers near the caller so "nearby volunteers" is real.
    await Volunteer.deleteMany({});
    const volunteerSpots = [
      { name: "Volunteer A", bearing: 40, distanceKm: 0.6, phone: "+91-90000-00001" },
      { name: "Volunteer B", bearing: 150, distanceKm: 1.1, phone: "+91-90000-00002" },
      { name: "Volunteer C", bearing: 280, distanceKm: 1.9, phone: "+91-90000-00003" },
    ];
    const volunteerDocs = volunteerSpots.map((spot) => {
      const bearingRad = (spot.bearing * Math.PI) / 180;
      return {
        name: spot.name,
        phone: spot.phone,
        available: true,
        latitude: Number((latitude + (spot.distanceKm * Math.cos(bearingRad)) / kmPerDegLat).toFixed(6)),
        longitude: Number(
          (longitude + (spot.distanceKm * Math.sin(bearingRad)) / (kmPerDegLat * Math.max(0.2, Math.cos(latRad)))).toFixed(6)
        ),
      };
    });
    await Volunteer.insertMany(volunteerDocs);

    res.json({
      success: true,
      message: `Seeded ${docs.length} safe spots + ${volunteerDocs.length} volunteers near ${latitude.toFixed(4)}, ${longitude.toFixed(4)}`,
      safeZones: docs,
    });
  } catch (error) {
    console.error("❌ Error seeding safe zones:", error);
    res.status(500).json({ success: false, message: "Failed to seed safe zones" });
  }
});

export default router;
