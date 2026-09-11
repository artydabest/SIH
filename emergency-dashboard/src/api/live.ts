import { io, type Socket } from "socket.io-client";
import type { Emergency } from "../types/emergency";
import type { DisasterAlert } from "../types/alert";

/**
 * Live updates from the backend over Socket.IO. REST polling in
 * EmergencyDataContext remains as a fallback for missed events.
 */
export type LiveEvent =
  | { type: "new"; emergency: Emergency }
  | { type: "status"; emergency: Emergency }
  | { type: "alert"; alert: DisasterAlert };

export function connectLive(
  url: string,
  onEvent: (event: LiveEvent) => void,
  onStatus: (connected: boolean) => void
): Socket {
  const socket = io(url, {
    transports: ["websocket", "polling"],
    reconnectionDelay: 2000,
  });

  socket.on("connect", () => onStatus(true));
  socket.on("disconnect", () => onStatus(false));
  socket.on("connect_error", () => onStatus(false));

  socket.on("emergency:new", (emergency: Emergency) => {
    onEvent({ type: "new", emergency });
  });

  socket.on("emergency:status", (emergency: Emergency) => {
    onEvent({ type: "status", emergency });
  });

  socket.on("alert:new", (alert: DisasterAlert) => {
    onEvent({ type: "alert", alert });
  });

  return socket;
}
