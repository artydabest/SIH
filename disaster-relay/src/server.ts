import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import emergencyRouter from "./routes/emergency";
import { connectDatabase } from "./database";

dotenv.config();

const app = express();
const PORT = 3000;

// Middleware
app.use(cors());
app.use(express.json());

app.use("/api/emergency", emergencyRouter);

// Test route
app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    service: "disaster-relay-backend",
  });
});

// Connect to database, then start server
connectDatabase().then(() => {
  app.listen(PORT, () => {
    console.log(`🚨 Disaster Relay backend running on port ${PORT}`);
  });
});