import express from "express";
import Emergency from "../models/Emergency";
import { calculateConfidence } from "../utils/confidence";

const router = express.Router();

router.post("/", async (req, res) => {
  try {
    const { stationaryMinutes, nearbyDevices, emergencyMode } = req.body;

const { score, level } = calculateConfidence({
  stationaryMinutes,
  nearbyDevices,
  emergencyMode: emergencyMode ?? false,
});

const emergency = await Emergency.create({
  ...req.body,
  confidence: score,
  confidenceLevel: level,
});

    console.log("EMERGENCY STORED:");
    console.log(emergency);

    res.status(201).json({
      success: true,
      message: "Emergency stored",
      emergency,
    });
  } catch (error) {
    console.error("Error creating emergency:", error);

    res.status(400).json({
      success: false,
      message: "Invalid emergency data",
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
    console.error("❌ Error fetching emergencies:", error);

    res.status(500).json({
      success: false,
      message: "Failed to fetch emergencies",
    });
  }
});
router.patch("/:id/status", async (req, res) => {
  try {
    const { status } = req.body;

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

    res.json({
      success: true,
      message: "Emergency status updated",
      emergency,
    });
  } catch (error) {
    console.error("❌ Error updating emergency status:", error);

    res.status(400).json({
      success: false,
      message: "Invalid status update",
    });
  }
});
export default router;