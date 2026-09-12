# Disaster Response Platform — SIH Prototype

A three-part disaster-response system: citizens report emergencies and receive
relevant, actionable warnings; responders triage incidents by evidence strength
and manage them through a status workflow.

> **Simulated data notice:** meteorological warnings, shelter occupancy, and
> volunteer positions in this prototype are **simulated demo data**, clearly
> labeled in every UI that shows them. Nothing here is connected to a real
> government meteorological service.

## Architecture

```
Android citizen app ──HTTP──▶ disaster-relay (Express + Socket.IO)
                                    │
                                    ├──▶ MongoDB (Atlas or local)
                                    │
Responder dashboard ◀──REST + Socket.IO──┘
Meteorological Alert Simulator (in dashboard) ──▶ POST /api/warnings
```

- **disaster-relay/** — Node.js + TypeScript + Express + Mongoose backend.
  Stores emergencies, people check-ins, safe zones, volunteers, Safe Circle
  memberships, and simulated warnings; computes the confidence score; broadcasts
  live updates over Socket.IO.
- **emergency-dashboard/** — React + TypeScript + Vite responder dashboard.
  Live incident map, confidence badges, status workflow, people/shelter layers,
  and the **Meteorological Alert Simulator** (demo warning control).
- **android-citizen-app/** — Android (Java, MapLibre + free OSM tiles).
  Home / Alerts / Safe Places / Map / Safe Circle tabs, SOS, I'M SAFE,
  warning banner with countdown and disaster-specific instructions.

## Core flows

**Emergency:** citizen SOS (or passive signal) → `POST /api/emergency` →
confidence engine scores evidence → stored → Socket.IO push → responder sees
incident instantly → `NEW → ACKNOWLEDGED → RESPONDING → RESOLVED`.

**Warning:** simulator issues warning → backend validates geographic
plausibility + attaches per-disaster instructions → stored → broadcast →
citizen app checks location relevance (`DIRECT` / `NEARBY` / informational) →
banner, notification, map zone, shelter guidance.

**Confidence engine** (transparent triage ranking, not a diagnosis — components
are visible to responders):

| Signal | Points |
|---|---|
| Stationary ≥20 / ≥10 / ≥5 min | +40 / +30 / +20 |
| Nearby devices ≥3 / 2 / 1 | +30 / +20 / +10 |
| Emergency mode on | +20 |

Score → level: `0–39 LOW · 40–69 MEDIUM · 70–89 HIGH · 90–100 CRITICAL`.
Confidence = evidence strength (LOW green → CRITICAL red); it is deliberately
visually distinct from responder status.

## Setup

### Backend

```bash
cd disaster-relay
npm install
```

Create `.env`:

```env
MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>/<db>   # or mongodb://127.0.0.1:27017/disaster-relay
API_KEY=change-me-to-a-long-random-string                 # omit to run with writes unauthenticated (dev mode)
PORT=3000
```

```bash
npm start              # tsx src/server.ts (seeds demo data on first boot)
npm run dev            # watch mode
npm run typecheck
npm run test:confidence   # confidence engine script
npm run test:shelter      # shelter scoring unit tests (15 assertions)
```

With `API_KEY` set: all **writes** (`POST`/`PATCH`/`DELETE`) require the
`X-API-Key` header → `401` otherwise; all **reads** stay open; writes are
rate-limited (120/min/IP). `GET /api/health` is always open.

### Dashboard

```bash
cd emergency-dashboard
npm install
```

Create `.env` (optional — defaults to `http://localhost:3000`):

```env
VITE_API_URL=http://localhost:3000
VITE_API_KEY=change-me-to-a-long-random-string   # must match the backend's API_KEY
```

```bash
npm run dev      # Vite dev server
npm run build    # typecheck + production bundle
```

### Android

1. Open `android-citizen-app/` in Android Studio (AGP 9.4, Gradle 9.6 — a recent JDK is required; use the studio-bundled JDK).
2. `local.properties` must point at **your** machine's SDK — the checked-in
   copy references the original dev machine's Windows path and will not work:
   ```properties
   sdk.dir=/Users/<you>/Library/Android/sdk
   ```
3. Build & install the debug APK.
4. Emulator: run `adb reverse tcp:3000 tcp:3000` once per boot, or use the
   host's LAN IP on a physical device.
5. In-app gear icon: set backend URL and (if configured) the API key — no
   rebuild needed per device.

## API overview

Base: `http://localhost:3000` · writes need `X-API-Key` when `API_KEY` is set.

| Method & path | Purpose |
|---|---|
| `GET /api/health` | Liveness (no auth) |
| `POST /api/emergency` | Report emergency; server computes confidence |
| `GET /api/emergency` | All incidents, newest first |
| `PATCH /api/emergency/:id/status` | `NEW → ACKNOWLEDGED → RESPONDING → RESOLVED` |
| `POST /api/people/checkin` | Location + safe/unsafe check-in |
| `GET /api/people` | Everyone's last known status |
| `GET /api/safezones` | Shelters / hospitals / camps |
| `GET /api/safezones/recommend?latitude=&longitude=` | Explained shelter advice (`EVACUATE` / `MONITOR_STAY_PUT` / `NO_ACTION` + reasons) |
| `POST /api/safezones/seed-nearby` | Seed safe spots + volunteers around a position |
| `GET /api/volunteers` | Volunteer roster |
| `POST /api/alerts` · `GET /api/alerts` | Legacy drill alerts (instructions attached server-side) |
| `POST /api/warnings` | Issue simulated warning (type/severity/region validated for plausibility) |
| `GET /api/warnings` | Warnings newest first; auto-expires stale ones |
| `GET /api/warnings/regions` · `/instructions/:type` | Region catalog · per-disaster instruction sets |
| `PATCH /api/warnings/:id/cancel` | Withdraw a simulated warning |
| `POST/GET/DELETE /api/safecircle/...` | Safe Circle membership + live safety status |

## Testing status

Verified live against the real backend (see `disaster-relay/CHANGELOG.md` for
the full log): health, emergency POST with exact confidence math (90 → CRITICAL),
status PATCH, validation errors, warning issue + plausibility guard (a tsunami
for Bengaluru is rejected), check-in → Safe Circle `SAFE`, shelter recommend,
API-key 401s on all writes, rate-limit 429s on write floods, and all reads
staying open.

Not covered by automation and requiring manual testing on hardware: Android
build/launch, notification delivery, GPS flow, map gestures, Socket.IO client
reception.

## Documentation

- [`DEMO_SCRIPT.md`](DEMO_SCRIPT.md) — 6-minute judged walkthrough, Q&A, fallbacks.
- [`PRODUCT_AUDIT.md`](PRODUCT_AUDIT.md) — honest weaknesses audit, feature
  priorities (must/should/remove), and the top-10 "what a judge could attack" list.
- [`disaster-relay/CHANGELOG.md`](disaster-relay/CHANGELOG.md) — dated change log.
