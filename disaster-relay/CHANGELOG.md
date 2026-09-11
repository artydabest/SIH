# Changelog — disaster-relay

## 2026-09-11 — Both Android clients share the backend (saanvi merge)

### Added

- Merged `origin/saanvi` (Trial2_SafePlaces_Volunteers) into `responder-dashboard`; both Android apps now live side by side (`MyApplication/` = responder-side app, root `app/` = Safe Places/Volunteers app).
- **Trial2 app (`app/`) backend integration:**
  - `EmergencyApi.java` — SOS POST + live incident feed against the shared `/api/emergency` store.
  - `MainActivity.java` — SOS button (fused-location fix, sends without coordinates when denied), 7 s incident polling, status line; live incidents pushed into the offline rescue map via a new `window.loadIncidents(...)` bridge.
  - `assets/rescue_map.html` — additive red pulsing incident markers (colour-coded by status, green when resolved) with Navigate-to-SOS-site popups; existing user/shelter/volunteer layers untouched.
  - Manifest: cleartext enabled for the dev backend (permissions already present).

### Notes

- Trial2 shelters/volunteers remain hardcoded test data (marked as such in-app) — backend has no shelter collection yet.
- Emulator base URL is `http://10.0.2.2:3000`; use the dev machine's LAN IP for physical devices.

---

## 2026-09-11 — Android client connected to the shared backend

### Added

- **`MyApplication/`** — The Android app now reads and writes the same `/api/emergency` store the responder dashboard uses, so both clients work simultaneously off one source of truth:
  - `ApiClient.java` — minimal HTTP+JSON client for `GET/POST /api/emergency` and `PATCH /:id/status` (base URL `http://10.0.2.2:3000/api/emergency`, i.e. host loopback from the emulator).
  - `MapActivity.java` — WebView wrapper for the existing `assets/rescue_map.html`; polls the backend every 7 s and pushes live incidents through the documented `window.loadEmergencies(...)` bridge (the HTML file itself is unchanged).
  - `MainActivity.java` — SOS now POSTs a real incident (last known GPS position when permitted; zeros + a notice when not) before opening the alert screen; home status card reflects live backend state; new Rescue Map card opens `MapActivity`.
  - `EmergencyActivity.java` — binds the newest incident (status, derived confidence, nearby devices, coordinates, age) and polls every 7 s, so dashboard actions (acknowledge/responding/resolved) appear on the phone within one poll cycle.
  - Manifest: `INTERNET`/`ACCESS_FINE_LOCATION` permissions, cleartext enabled for the dev backend, `MapActivity` registered.

### Notes

- Dashboard ↔ phone sync cadence is bounded by each client's 7 s poll; no backend changes were required.
- For a physical device, point `ApiClient.BASE_URL` at the dev machine's LAN IP instead of `10.0.2.2`.

---

## 2026-09-10 — Dashboard v2: responder operations redesign

### Changed

- **`emergency-dashboard/`** — Full rebuild of the frontend per `emergency-response-design-brief.md`:
  - Dark emergency-operations design system (`src/styles/tokens.css`): brief palette (#090A0C bg), Inter + JetBrains Mono (mono for IDs/coords/timestamps), lucide-react icons instead of emoji.
  - Router architecture: react-router-dom with `AppShell` (collapsible `Sidebar`, ops `Header` with SYSTEM ONLINE/OFFLINE pill, last-sync time, responder identity, live clock) and pages: Overview, Active Incidents, Incident Detail, Rescue Map, Detection History, People (honest placeholder), Settings.
  - `EmergencyDataContext` — single source of incident data: 7s polling with stale-response guard, connection tracking, toasts via `ToastContext` bridge.
  - `EvidencePanel` — "WHY THIS ALERT?" explainability derived only from real backend fields; `SeverityChip` is derived (HIGH/ELEVATED/ACTIVE/CLOSED), never fabricated.
  - `IncidentDetail` — dedicated operational view: facts grid, evidence, focused map, workflow action.
  - `RescueMap` — CartoDB dark basemap (no API key), status-colored pulsing emergency markers, relay-device + responder markers explicitly labeled "SIMULATED" in popups, legend.
  - Incident selection ↔ map focus sync; status filters; history table; ALL CLEAR empty state; SYSTEM OFFLINE banner with RETRY; skeleton loading.
  - Demo data (`src/mocks/demoEmergencies.ts`) shown ONLY when backend is unreachable and no real data exists, always labeled "DEMO DATA"; disable with `VITE_ENABLE_MOCK_DATA=false`.
  - Backend contract untouched; unknown API fields (e.g. stray `confidence` in test docs) are ignored.

---

## 2026-09-10 — Emergency response dashboard frontend

### Added

- **`emergency-dashboard/`** — New React + TypeScript + Vite frontend (at repo root) that consumes the existing backend API. Built from the `emergency-dashboard-agent.md` spec:
  - `src/types/emergency.ts` — `Emergency` model, `EmergencyStatus` union, and workflow helpers (`NEXT_STATUS`, `ACTION_LABEL`) enforcing `NEW → ACKNOWLEDGED → RESPONDING → RESOLVED`.
  - `src/api/emergencyApi.ts` — API service layer (`getEmergencies`, `updateEmergencyStatus`) using `VITE_API_URL` from `.env.local` (no hard-coded URLs in components).
  - `src/components/` — `StatusBadge` (glyph + text, never color alone), `EmergencyCard` (all required fields, next-action button, OpenStreetMap link), `EmergencyList` (skeleton loading + empty states), `SummaryCards` (counts derived from fetched data), `EmergencyMap` (Leaflet + OpenStreetMap, status-colored markers, popups, auto-fit/fly-to), `ErrorBoundary` (dashboard survives map failure).
  - `src/App.tsx` — Header with connection status/clock/refresh, summary row, split list+map layout, 7s polling with cleanup and stale-response guard, toasts for update success/failure, error banner when backend is unreachable.
  - Dark operations theme, responsive down to mobile (map stacks below list), keyboard-accessible controls, semantic HTML.

### Fixed

- Reinstalled backend `node_modules` — the previous install carried a Windows esbuild binary (`@esbuild/win32-x64`), which crashed `tsx` on macOS.

---

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
