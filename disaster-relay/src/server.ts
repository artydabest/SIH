import http from "http";
import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import dns from "dns";

// Workaround for Node.js v24 DNS SRV bug on Windows (querySrv ECONNREFUSED).
// Force public DNS resolvers before any MongoDB SRV lookup.
dns.setServers(["1.1.1.1", "8.8.8.8", "9.9.9.9"]);
import emergencyRouter from "./routes/emergency";
import safezoneRouter from "./routes/safezones";
import volunteerRouter from "./routes/volunteers";
import peopleRouter from "./routes/people";
import alertRouter from "./routes/alerts";
import warningRouter from "./routes/warnings";
import safeCircleRouter from "./routes/safecircle";
import { connectDatabase } from "./database";
import { initSocketIO } from "./socket";
import { seedDatabase, seedSimulatedWarnings } from "./seed";
import { apiKeyGuard } from "./middleware/apiKey";
import { apiLimiter, writeLimiter } from "./middleware/rateLimit";

dotenv.config();

const PORT = process.env.PORT || 3000;

const app = express();
const httpServer = http.createServer(app);

app.use(cors());
app.use(express.json());
app.use(apiLimiter);

// Health check is unauthenticated on purpose (liveness probes).
app.get("/api/health", (_req, res) => {
  res.json({
    status: "ok",
    service: "disaster-relay-backend",
  });
});

// Write endpoints require the shared API key (no-op with a warning when
// unset) and are rate-limited so fake-report floods get throttled.
// Reads stay open: dashboards/citizen apps must never lose visibility
// because of a key misconfiguration.
app.use(apiKeyGuard);
app.use(writeLimiter);

app.use("/api/emergency", emergencyRouter);
app.use("/api/safezones", safezoneRouter);
app.use("/api/volunteers", volunteerRouter);
app.use("/api/people", peopleRouter);
app.use("/api/alerts", alertRouter);
app.use("/api/warnings", warningRouter);
app.use("/api/safecircle", safeCircleRouter);

// Connect to the database, seed baseline data, then start server + sockets.
connectDatabase()
  .then(() => seedDatabase())
  .then(() => seedSimulatedWarnings())
  .then(() => {
    initSocketIO(httpServer);
    httpServer.listen(PORT, () => {
      console.log(`Disaster Relay backend listening on port ${PORT}`);
    });
  });