import type { Emergency } from "../types/emergency";

/**
 * DEMO DATA — clearly separated from real backend records.
 *
 * Rules (from the design brief):
 *  - Never silently mixed with real incidents (real data always wins).
 *  - Only shown when the backend is unreachable AND no real data exists.
 *  - Always labeled in the UI with a visible "DEMO DATA" chip.
 *  - Disable by setting VITE_ENABLE_MOCK_DATA=false in .env.local.
 */

const minutesAgo = (m: number): string =>
  new Date(Date.now() - m * 60_000).toISOString();

export const MOCK_EMERGENCIES: Emergency[] = [
  {
    _id: "mock-a381",
    deviceId: "A381",
    latitude: 12.9716,
    longitude: 77.5946,
    altitude: 924.5,
    stationaryMinutes: 11,
    nearbyDevices: 4,
    status: "NEW",
    createdAt: minutesAgo(1),
    updatedAt: minutesAgo(1),
  },
  {
    _id: "mock-f192",
    deviceId: "F192",
    latitude: 12.9352,
    longitude: 77.6245,
    altitude: 901.2,
    stationaryMinutes: 27,
    nearbyDevices: 2,
    status: "ACKNOWLEDGED",
    createdAt: minutesAgo(38),
    updatedAt: minutesAgo(12),
  },
  {
    _id: "mock-c821",
    deviceId: "C821",
    latitude: 12.9784,
    longitude: 77.6408,
    altitude: 918.7,
    stationaryMinutes: 64,
    nearbyDevices: 6,
    status: "RESPONDING",
    createdAt: minutesAgo(95),
    updatedAt: minutesAgo(20),
  },
  {
    _id: "mock-b104",
    deviceId: "B104",
    latitude: 12.9082,
    longitude: 77.6474,
    altitude: 895.1,
    stationaryMinutes: 143,
    nearbyDevices: 1,
    status: "RESOLVED",
    createdAt: minutesAgo(240),
    updatedAt: minutesAgo(55),
  },
];
