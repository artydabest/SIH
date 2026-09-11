# Changelog — disaster-relay

## 2026-09-11 — Live updates, safe zones, volunteers, validation

### Added

- **Socket.IO live layer (`src/socket.ts`)** — attached to the HTTP server; broadcasts `emergency:new` and `emergency:status` to all connected dashboards so they update instantly instead of waiting for the next poll.
- **`GET /api/safezones` (`src/routes/safezones.ts`, `src/models/SafeZone.ts`)** — read-only list of shelters/hospitals/camps for the citizen app; auto-seeded on first boot (`src/seed.ts`, idempotent).
- **`GET /api/volunteers` (`src/routes/volunteers.ts`, `src/models/Volunteer.ts`)** — read-only volunteer list (name, coordinates, availability, phone); also auto-seeded.
- Request validation on `POST /api/emergency` and `PATCH /api/emergency/:id/status` — precise `400` messages for bad latitude/longitude/deviceId/status instead of generic cast errors.

### Changed

- **`src/server.ts`** — wrapped in `http.createServer` for Socket.IO; mounted `/api/safezones` and `/api/volunteers`; `PORT` now comes from env; runs `seedDatabase()` after connecting.

## 2026-09-10 — Emergency reporting backend

### Added

- **`src/models/Emergency.ts`** — Mongoose model for emergency reports with the following fields:
  - `deviceId` (String, required)
  - `latitude` / `longitude` (Number, required)
  - `altitude` (Number, optional)
  - `stationaryMinutes` (Number, required)
  - `nearbyDevices` (Number, required)
  - `status` (enum: `NEW`, `ACKNOWLEDGED`, `RESPONDING`, `RESOLVED`; defaults to `NEW`)
  - Automatic `createdAt` / `updatedAt` timestamps enabled.

- **`src/routes/emergency.ts`** — Express router mounted at `/api/emergency` with three endpoints:
  - `POST /api/emergency` — stores a new emergency report, returns `201` with the saved document; invalid data returns `400`.
  - `GET /api/emergency` — returns all emergencies sorted newest first.
  - `PATCH /api/emergency/:id/status` — updates an emergency's status (`NEW` → `ACKNOWLEDGED` → `RESPONDING` → `RESOLVED`); returns `404` if the ID doesn't exist.

### Changed

- **`src/server.ts`** — Wired up the full Express app:
  - Added `cors` and `express.json()` middleware.
  - Mounted the emergency router at `/api/emergency`.
  - Added a health-check route: `GET /api/health` → `{ status: "ok", service: "disaster-relay-backend" }`.
  - Server now starts on port `3000` **only after** the database connection succeeds.

- **`src/database.ts`** — MongoDB connection helper using `MONGODB_URI` from `.env`; logs success/failure and exits the process on connection failure.

---

*Note: this changelog was written by Buffy based on the current state of the source files. The original session's exact history wasn't available, so it reflects the code as of the dates above.*
