# Demo Script — Live Judging Walkthrough

A rehearsed ~6-minute flow. Every step maps to verified working code; nothing here
requires claiming an unimplemented feature. Simulated data is labeled on screen —
point at the labels before a judge does.

## Before the audience arrives

1. **Backend up:** `cd disaster-relay && npm start`
   - Expect: `MongoDB connected`, seeding logs, `listening on port 3000`.
2. **Dashboard up:** `cd emergency-dashboard && npm run dev`
   - Expect: map tiles load, connection pill shows live.
3. **Citizen app:** install debug APK, confirm backend URL + API key in Settings (gear icon), tap Refresh, confirm GPS fix.
4. **Pre-check the chain once:** issue a throwaway warning from the simulator, see it on the phone, withdraw it. Never discover a broken tunnel mid-demo.
5. If using an emulator: `adb reverse tcp:3000 tcp:3000`.

## Act 1 — Alert becomes personal (90 sec)

1. Dashboard → **Meteorological Alert Simulator** tab (labeled SIMULATED — say it out loud).
2. Select Heavy Rainfall, HIGH, Bengaluru Urban, start in 2 h, press **SEND WARNING**.
3. Point out the backend's plausibility guard: "If I tried a tsunami for Bengaluru, the API rejects it — simulated data is constrained to geographically plausible events."
4. Citizen app: warning banner appears — severity stripe, **countdown ("Expected in 2 h")**, area, and disaster-specific instructions (not generic copy).

Talking point: "The app doesn't forward a weather feed. It checks *your* location against the warning radius, distinguishes DIRECT vs NEARBY vs informational, and attaches per-disaster instructions from a data-driven catalog."

## Act 2 — Where should I actually go? (90 sec)

1. Citizen app Home → **Shelter Guidance** card: advice headline, backend's reason, top shelter marked ★.
2. Tap the recommended shelter → navigation hands off to Google Maps.
3. Show the "why": suitable-for-type, capacity available, distance/walk time.
4. Key line: "For a heatwave or lightning warning the same endpoint says STAY PUT — traveling is the dangerous act there. The recommendation is disaster-type aware, not nearest-dot."

## Act 3 — I'M SAFE → Safe Circle (60 sec)

1. Tap **I'M SAFE** on the citizen app.
2. Second device (or simulator on the same device with a different device ID) shows the friend flip to SAFE in the Safe Circle tab.
3. Mention the staleness rule: "A friend who hasn't pinged in 30 minutes shows UNKNOWN, not SAFE — we don't fake certainty."

## Act 4 — SOS → Confidence → Responder (2 min)

1. Press **SOS** on the citizen app.
2. Dashboard: incident appears **instantly** (Socket.IO, not poll refresh) — point at the toast + marker.
3. Explain confidence honestly: "This is a triage ranking, not a diagnosis: stationary time + nearby device count + emergency mode, max 100. Responders see the components. SOS reports always outrank passive detections."
4. Walk the status workflow: NEW → **ACKNOWLEDGED** → RESPONDING → RESOLVED, live badge updates.
5. Show the severity stripes on the map danger zones and note they are warning severity, visually distinct from incident confidence colors.

## Anticipated judge questions → one-line answers

- **"Anyone can POST a fake emergency?"** → "Writes require an API key and are rate-limited; production adds device attestation. Reads stay open so victims without configuration can still be seen." (Show `.env` with `API_KEY` set.)
- **"BLE offline mesh?"** → "Future scope — device-count evidence is accepted by the confidence engine; collecting it via BLE is not implemented. We won't claim it."
- **"Who maintains shelter data?"** → "Prototype marks occupancy simulated everywhere; `lastVerifiedAt` is the hook for a municipal verification workflow."
- **"GPS indoors?"** → "Relevance uses radius buffers and a NEARBY tier, so drift degrades to a softer warning rather than a wrong one."
- **"What's your value over a government SMS blast?"** → "Relevance, instructions, shelter guidance, and a family safe-status loop — making one received warning personally actionable."

## Fallbacks

- **Wi-Fi dies:** dashboard shows offline pill and keeps last data; say "graceful degradation is designed in" and continue with backend + phone via mobile hotspot.
- **GPS won't fix indoors:** pre-record a screen recording of Acts 1–4 as backup; show it while explaining.
- **MongoDB Atlas unreachable:** keep a second `.env` pointing at a local `mongod` started before the demo.

## Reset between runs

- Withdraw demo warnings from the simulator (withdraw button).
- Dashboard incident list: resolve or ignore test incidents; test device IDs (`QA-*`) are safe to leave.
