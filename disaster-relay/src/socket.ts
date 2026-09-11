import type { Server as HttpServer } from "http";
import { Server } from "socket.io";
import type { Document } from "mongoose";

interface EmergencyDoc extends Document {
  deviceId: string;
  latitude: number;
  longitude: number;
  status: string;
}

let io: Server | null = null;

/** Attach Socket.IO to the existing HTTP server so all transports work. */
export function initSocketIO(httpServer: HttpServer): Server {
  io = new Server(httpServer, {
    cors: {
      origin: "*", // dashboard + citizen app clients
      methods: ["GET", "POST"],
    },
  });

  const server = io;

  server.on("connection", (socket) => {
    console.log(`🔌 Client connected: ${socket.id} (${server.engine.clientsCount} online)`);

    socket.on("disconnect", () => {
      console.log(`🔌 Client disconnected: ${socket.id} (${server.engine.clientsCount} online)`);
    });
  });

  return io;
}

/** Broadcast a newly stored emergency to every connected dashboard. */
export function broadcastNewEmergency(emergency: EmergencyDoc): void {
  io?.emit("emergency:new", emergency);
}

/** Broadcast a status update so dashboards update without waiting for the next poll. */
export function broadcastStatusUpdate(emergency: EmergencyDoc): void {
  io?.emit("emergency:status", emergency);
}

/** Nudge dashboards that a friend check-in happened so they can refresh people. */
export function broadcastPeopleUpdate(): void {
  io?.emit("people:updated");
}

/** Broadcast a disaster alert with safety instructions to every client. */
export function broadcastAlert(alert: unknown): void {
  io?.emit("alert:new", alert);
}
