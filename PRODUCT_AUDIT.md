# Product Logic Audit — Disaster Response Prototype

Written as a hostile-but-fair hackathon judge. Covers sections 20–25 of the product brief.
This document is deliberately honest: it exists so the project can be defended, not flattered.

---

## 20. Product Logic Audit

### W1. Passive detection infers "emergency" from thin evidence

- **WEAKNESS:** Stationary + nearby phones + emergency mode can mean many things: someone asleep at home, a phone left on a desk, a person at the cinema. The engine treats stillness as distress.
- **WHY IT MATTERS:** Responders get false positives. A few wasted dispatches and they stop trusting the queue.
- **CURRENT ASSUMPTION:** Stillness *during an active disaster* correlates with someone who cannot call for help.
- **IMPROVEMENT:** Weight passive detections far lower when no warning/alert is active in the area; present them as "unconfirmed signals" not "emergencies." The confidence score already does this implicitly (max 60 without emergency mode) — surface that distinction in UI wording.
- **JUDGE QUESTION:** "Someone sleeping through your app's notification looks identical to someone unconscious. How do you tell?"
- **BEST DEFENSE:** "We don't claim to tell. The score ranks *urgency of review*, not diagnosis — SOS reports always outrank passive detections of equal score, and we show responders which signal type it is."

### W2. BLE relay claim must not be overstated

- **WEAKNESS:** There is no working BLE mesh in the codebase. `nearbyDevices` is currently user-reported (the Android app sends `0`).
- **WHY IT MATTERS:** Claiming offline resilience that doesn't exist is the fastest way to lose credibility.
- **CURRENT ASSUMPTION:** Internet + GPS work.
- **IMPROVEMENT:** Present BLE relay as designed future scope. Do not demo it.
- **JUDGE QUESTION:** "Is this a real mesh or a slide?"
- **BEST DEFENSE:** "A prototype boundary we are explicit about: the confidence engine accepts device-count evidence, but collecting it via BLE is future scope."

### W3. No defense against malicious/fake reports

- **WEAKNESS:** Anyone with the API can POST emergencies for any device ID, spam Safe Circles, or issue warnings if they can reach the dashboard.
- **WHY IT MATTERS:** An attacker can flood responders or cause panic.
- **CURRENT ASSUMPTION:** Demo environment, trusted network.
- **IMPROVEMENT:** Say out loud: production needs device attestation, rate limits, and an authenticated issuing authority for warnings. Not in scope for the prototype.
- **JUDGE QUESTION:** "What stops one prankster from triggering 500 fake SOS alerts?"
- **BEST DEFENSE:** "Nothing in the prototype — and that's the honest answer. The architecture (server-side confidence, status workflow) is where verification hooks would go."

### W4. Shelter data has no owner

- **WEAKNESS:** Occupancy and `lastVerifiedAt` are simulated seeds. Nobody maintains this data; a "FULL" shelter in the app may be open in reality and vice versa.
- **WHY IT MATTERS:** Sending a family to a shelter that is actually full is worse than not sending them.
- **CURRENT ASSUMPTION:** Demo data labeled "SIMULATED".
- **IMPROVEMENT:** In the demo, narrate the intended loop: municipal feeds + volunteer verification via the dashboard. The `lastVerifiedAt` field is the hook for this.
- **JUDGE QUESTION:** "Who is responsible when the app says a shelter has space and it doesn't?"
- **BEST DEFENSE:** "The prototype marks occupancy as simulated everywhere it appears. A real deployment needs an authority responsible for shelter status — we surface staleness (`lastVerifiedAt`) so the burden is explicit."

### W5. Evacuation advice can be wrong on routes

- **WHY IT MATTERS:** A route through a flooded underpass is deadly. The app recommends by distance/suitability only.
- **CURRENT ASSUMPTION:** Walking straight lines, open roads.
- **IMPROVEMENT:** The system already tells users to move "while routes are open" and defaults to STAY PUT for non-travel disasters (lightning, heatwave, thunderstorm). Present route awareness as future scope; never claim safe routing.
- **JUDGE QUESTION:** "Your app sent someone toward a danger zone."
- **BEST DEFENSE:** "Recommendations are advisory with reasons shown, and for disaster types where travel itself is dangerous we explicitly say stay put. We never claim to route around hazards."

### W6. Alert fatigue is only partially solved

- **WEAKNESS:** Relevance filtering exists (DIRECT / NEARBY / INFORMATIONAL + once-per-warning popups), but poll-based delivery can still stack notifications across regions during a multi-region event.
- **IMPROVEMENT:** Per-severity notification rules (already: only HIGH/CRITICAL DIRECT warnings get dialogs) — good; keep quiet for the rest.

### W7. Indoor / GPS-accuracy failure

- **WEAKNESS:** Indoors, GPS can drift by hundreds of meters — enough to place someone inside or outside a warning radius incorrectly.
- **IMPROVEMENT:** Warning radii already have a buffer (radiusKm + 2 km in relevance checks, "NEARBY" tier). Present this as graceful degradation, not precision.

### W8. Single-region scale test

- **WEAKNESS:** MongoDB Atlas demo cluster + one Node process. A real disaster means thousands of reports/minute.
- **IMPROVEMENT:** Say: prototype validates the interaction model, not production scale. The stateless API layer would scale horizontally behind a load balancer.

---

## 21. Challenging the Core Claims

**A. "Passive emergency detection helps people who cannot press SOS."**
Partially defensible. Honest framing: it surfaces *unconfirmed signals* for responder review, ranked by evidence strength. It cannot distinguish sleep from unconsciousness. Present as triage assistance, not detection.

**B. "BLE relay helps when internet fails."**
DO NOT CLAIM THIS. No BLE implementation exists. Future scope only.

**C. "Confidence scoring reduces false positives."**
KEEP, BUT REFRAME. The weights (stationary 20/30/40, devices 10/20/30, mode 20) are heuristic, not validated against incident data. Honest framing: "a transparent, tunable ranking of signal strength — responders see the components, not a black box." The score is shown with its raw inputs so responders can disagree with it.

**D. "Safe shelters help people."**
The value is information (which shelter, why, how far), not transportation or capacity guarantees. The recommendation engine's most defensible property is conservatism: no relevant warning → "no action needed", travel-dangerous disaster types → "stay put".

**E. "Official warnings become actionable."**
The real additions: (1) location relevance — only warn people in the affected area, (2) disaster-specific instructions attached to each warning, (3) nearest suitable shelters with reasons, (4) I'M SAFE loop for family. Not "we deliver warnings better than the government" — "we make a received warning personally actionable."

---

## 22. What Should NOT Be Built

- **BLE mesh** — cannot be faked credibly or built in a hackathon. REMOVE from claims; keep as future scope.
- **Shelter capacity editing from the citizen app** — citizens shouldn't write infrastructure data. Not built; correct.
- **In-app chat between citizens and responders** — duplicates phone calls; adds moderation burden. REMOVE.
- **Battery/BFS one-tap "power saving SOS"** — complexity without demo value. REMOVE.
- **Real government API integration** — no stable public API for Indian met warnings suitable for a demo; the simulator with plausibility guards is the right call. KEEP SIMULATOR, clearly labeled.

## 23. Feature Classification

**MUST HAVE (all present, verified):**
- SOS → backend → confidence → dashboard workflow
- Status workflow NEW → ACKNOWLEDGED → RESPONDING → RESOLVED
- Simulated warning system with geographic plausibility guard
- Disaster-specific instructions (data-driven catalog)
- Location relevance filtering (DIRECT/NEARBY/INFORMATIONAL)
- Advance warnings with countdown
- Shelter recommendation with reasons + stay-put advice
- I'M SAFE + Safe Circle
- Warning history (active vs past)

**SHOULD HAVE (present):**
- Socket.IO live updates for incidents, warnings, safety
- Severity-mapped danger zones on map (distinct from incident markers)
- Safe-spot seeding around the user's actual GPS position

**NICE TO HAVE:**
- Shelter occupancy simulation (labeled)
- Demo seeding of plausible regional warnings

**REMOVE / FUTURE SCOPE:**
- BLE device counting (future scope; `nearbyDevices` stays user-reported)
- Any claim of routing around hazards

## 24. Final Product Test — walkthrough status

Verified end-to-end against the live backend (see CHANGELOG for test evidence):
warning issued via simulator API → stored with instructions → plausibility guard rejects nonsense → shelter recommendation returns EVACUATE with reasons → SOS stores emergency with correct confidence (90 → CRITICAL) → responder PATCH moves it through the workflow → citizen check-in reflects SAFE in the circle. Remaining untested on real hardware: Android build/launch, notification delivery, map rendering on device.

## 25. WHAT A HACKATHON JUDGE COULD ATTACK — Top 10

| # | Weakness | Seriousness | Fix / Explanation during judging |
|---|----------|-------------|----------------------------------|
| 1 | No auth; anyone can POST emergencies/warnings | ~~HIGH~~ → MEDIUM | **Addressed:** writes now require `X-API-Key` + rate limiting (reads stay open). Production still needs device attestation + an authenticated warning authority |
| 2 | Confidence weights are unvalidated heuristics | HIGH | Present as transparent, tunable triage ranking; components visible to responders |
| 3 | BLE/`nearbyDevices` is user-reported, not measured | HIGH | DO NOT CLAIM mesh; future scope |
| 4 | Shelter data is simulated, no owner | MEDIUM | Labeled everywhere; `lastVerifiedAt` is the hook for real verification workflows |
| 5 | Passive detection false positives (sleep ≠ emergency) | MEDIUM | SOS outranks passive; score = review priority, not diagnosis |
| 6 | No safe-route awareness during evacuation advice | MEDIUM | Advisory only, reasons shown; stay-put default for travel-dangerous disasters |
| 7 | GPS inaccuracy indoors misclassifies relevance | MEDIUM | Radius buffers + NEARBY tier already degrade gracefully |
| 8 | Single-region scale unproven | MEDIUM | Prototype validates interaction model; API layer is horizontally scalable |
| 9 | Simulator could be mistaken for a real met feed | LOW | Every surface labeled SIMULATED/DEMO; source field says so |
| 10 | Notification stacking across many warnings | LOW | Once-per-warning popups, severity-gated dialogs; only HIGH/CRITICAL in-area warnings interrupt |

**Bottom line:** KEEP the demo chain (warning → relevance → instructions → shelter advice → I'M SAFE → SOS → confidence → responder workflow) — it is coherent and verified. REMOVE all BLE/offline claims. PRESENT confidence as triage ranking, future-scope the mesh, and be upfront that the prototype has no authentication.
