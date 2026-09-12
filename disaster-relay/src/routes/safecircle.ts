import { Router } from "express";
import SafeCircleMember from "../models/SafeCircleMember";
import Person from "../models/Person";
import { broadcastSafetyUpdate } from "../socket";

const router = Router();

/** Simple membership row returned to clients. */
interface CircleRow {
  memberDeviceId: string;
  memberName: string | null;
  addedAt: string;
  lastSeenAt: string | null;
  latitude: number | null;
  longitude: number | null;
  /** Derived safety status: SAFE / UNKNOWN / POSSIBLE_EMERGENCY. */
  safetyStatus: "SAFE" | "UNKNOWN" | "POSSIBLE_EMERGENCY";
}

/**
 * POST /api/safecircle/:ownerDeviceId/members
 * Body: { memberDeviceId, memberName? }
 * Adds a friend to the owner's circle. Idempotent (upsert on device pair).
 */
router.post("/:ownerDeviceId/members", async (req, res) => {
  const { ownerDeviceId } = req.params;
  const { memberDeviceId, memberName } = (req.body ?? {}) as {
    memberDeviceId?: unknown;
    memberName?: unknown;
  };

  if (typeof memberDeviceId !== "string" || memberDeviceId.trim().length === 0) {
    return res.status(400).json({ success: false, message: "memberDeviceId is required" });
  }
  if (memberDeviceId.trim() === ownerDeviceId.trim()) {
    return res.status(400).json({ success: false, message: "Cannot add yourself to your Safe Circle" });
  }

  try {
    const member = await SafeCircleMember.findOneAndUpdate(
      { ownerDeviceId: ownerDeviceId.trim(), memberDeviceId: memberDeviceId.trim() },
      {
        ownerDeviceId: ownerDeviceId.trim(),
        memberDeviceId: memberDeviceId.trim(),
        ...(typeof memberName === "string" && memberName.trim().length > 0
          ? { memberName: memberName.trim() }
          : {}),
      },
      { new: true, upsert: true, setDefaultsOnInsert: true }
    );

    broadcastSafetyUpdate();

    res.status(201).json({
      success: true,
      message: "Member added to Safe Circle",
      member,
    });
  } catch (error) {
    console.error("Error adding safe circle member:", error);
    res.status(500).json({ success: false, message: "Failed to add member" });
  }
});

/**
 * GET /api/safecircle/:ownerDeviceId — the owner's circle with live status.
 * Status derives from the member's Person record: safe=true → SAFE,
 * safe=false → POSSIBLE_EMERGENCY, no record/recent ping → UNKNOWN.
 */
router.get("/:ownerDeviceId", async (req, res) => {
  const { ownerDeviceId } = req.params;

  try {
    const members = await SafeCircleMember.find({
      ownerDeviceId: ownerDeviceId.trim(),
    }).sort({ addedAt: 1 });

    const memberIds = members.map((m) => m.memberDeviceId);
    const people = memberIds.length
      ? await Person.find({ deviceId: { $in: memberIds } })
      : [];
    const byDevice = new Map(people.map((p) => [p.deviceId, p]));

    // A member who hasn't pinged in 30 minutes shows as UNKNOWN, not SAFE.
    const STALE_MS = 30 * 60 * 1000;

    const rows: CircleRow[] = members.map((m) => {
      const person = byDevice.get(m.memberDeviceId);
      let safetyStatus: CircleRow["safetyStatus"] = "UNKNOWN";
      if (person) {
        const fresh = Date.now() - person.lastSeenAt.getTime() < STALE_MS;
        if (fresh) {
          safetyStatus = person.safe ? "SAFE" : "POSSIBLE_EMERGENCY";
        }
      }
      return {
        memberDeviceId: m.memberDeviceId,
        memberName: m.memberName || person?.name || null,
        addedAt: m.addedAt.toISOString(),
        lastSeenAt: person?.lastSeenAt?.toISOString() ?? null,
        latitude: person?.latitude ?? null,
        longitude: person?.longitude ?? null,
        safetyStatus,
      };
    });

    res.json({ success: true, members: rows });
  } catch (error) {
    console.error("Error fetching safe circle:", error);
    res.status(500).json({ success: false, message: "Failed to fetch Safe Circle" });
  }
});

/** DELETE /api/safecircle/:ownerDeviceId/members/:memberDeviceId */
router.delete("/:ownerDeviceId/members/:memberDeviceId", async (req, res) => {
  const { ownerDeviceId, memberDeviceId } = req.params;

  try {
    const result = await SafeCircleMember.findOneAndDelete({
      ownerDeviceId: ownerDeviceId.trim(),
      memberDeviceId: memberDeviceId.trim(),
    });

    if (!result) {
      return res.status(404).json({ success: false, message: "Member not found in your Safe Circle" });
    }

    broadcastSafetyUpdate();

    res.json({ success: true, message: "Member removed from Safe Circle" });
  } catch (error) {
    console.error("Error removing safe circle member:", error);
    res.status(500).json({ success: false, message: "Failed to remove member" });
  }
});

export default router;
