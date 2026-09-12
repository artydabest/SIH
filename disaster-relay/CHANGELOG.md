# Changelog — disaster-relay

## 2026-09-12 — API key + rate limiting (write endpoints)

### Added

- **`src/middleware/apiKey.ts`** — write requests (`POST`/`PATCH`/`DELETE`) require `X-API-Key` matching `API_KEY` from `.env`; wrong/missing key → `401`. All reads (`GET`/`HEAD`) stay open so dashboards and citizen apps never lose visibility over a key misconfiguration. When `API_KEY` is unset the guard is a no-op with a one-time console warning (dev mode).
- **`src/middleware/rateLimit.ts`** — general limiter (300 req/min/IP) plus a strict write limiter (120 writes/min/IP); read methods bypass it. Verified: 130 rapid writes → 120×`201` + 10×`429`; 50 rapid reads → all `200`.
- `GET /api/health` remains unauthenticated (liveness probes) and sits above the guard.

### Changed

- **`src/server.ts`** — guard + limiters mounted before the API routers; health route moved above the guard.
- **Consumers** — dashboard (`VITE_API_KEY` → `X-API-Key` via new `src/api/client.ts` wrapper) and Android (`ApiClient` sends the header; key editable in Settings) attach the key on every request.

### Verified live (API_KEY set)

- All reads without key: `200` (warnings, safezones, volunteers, safecircle, recommend, health)
- All writes without key: `401` (emergency POST, warning POST, checkin POST, status PATCH)
- Writes with correct key: `201`

---

## 2026-09-12 — End-to-end verification pass
## 2026-09-12 — End-to-end verification pass

### Fixed

- **`GET /api/safezones/recommend` crashed (HTTP 500) on legacy data** — `scoreShelter` read `facilities`/`suitableFor` off lean() documents, which skip schema defaults, so documents written before those fields existed had `undefined` arrays. Fields are now normalized before scoring (`src/services/shelterRecommendation.ts`). Verified live: the endpoint returns `EVACUATE` advice with scored shelters.

### Verified (live, against MongoDB Atlas)

- Health endpoint, emergency POST with confidence math (22 min stationary + 3 nearby + emergency mode → 90 → CRITICAL), GET newest-first, PATCH status workflow
- Input validation (bad latitude → precise 400)
- Warning issue via simulator API with instructions attached; geographic plausibility guard rejects tsunami-in-Bengaluru
- Check-in → Safe Circle status derivation (SAFE)
- Confidence unit test script (`npm run test:confidence`) matches spec

---
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
