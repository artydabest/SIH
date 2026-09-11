import http from "http";
import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import emergencyRouter from "./routes/emergency";
import safezoneRouter from "./routes/safezones";
import volunteerRouter from "./routes/volunteers";
import peopleRouter from "./routes/people";
import alertRouter from "./routes/alerts";
import { connectDatabase } from "./database";
import { initSocketIO } from "./socket";
import { seedDatabase } from "./seed";

dotenv.config();

const PORT = process.env.PORT || 3000;

const app = express();
const httpServer = http.createServer(app);

// Middleware
app.use(cors());
app.use(express.json());

app.use("/api/emergency", emergencyRouter);
app.use("/api/safezones", safezoneRouter);
app.use("/api/volunteers", volunteerRouter);
app.use("/api/people", peopleRouter);
app.use("/api/alerts", alertRouter);

// Health check
app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    service: "disaster-relay-backend",
  });
});

// Connect to database, seed baseline data, then start server + sockets
connectDatabase()
  .then(() => seedDatabase())
  .then(() => {
    initSocketIO(httpServer);
    httpServer.listen(PORT, () => {
      console.log(`🚨 Disaster Relay backend running on port ${PORT}`);
    });
  });