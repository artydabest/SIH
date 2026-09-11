import { Router } from "express";
import Volunteer from "../models/Volunteer";

const router = Router();

/** GET /api/volunteers — all volunteers; availability filtering is client-side. */
router.get("/", async (req, res) => {
  try {
    const volunteers = await Volunteer.find().sort({ createdAt: -1 });

    res.json({
      success: true,
      volunteers,
    });
  } catch (error) {
    console.error("❌ Error fetching volunteers:", error);
    res.status(500).json({
      success: false,
      message: "Failed to fetch volunteers",
    });
  }
});

export default router;
