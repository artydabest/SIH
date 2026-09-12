import { Activity, Gauge, MapPin, Timer } from "lucide-react";
import type { Emergency } from "../types/emergency";
import { formatStationary } from "../lib/format";
import "../styles/evidence.css";

interface EvidencePanelProps {
  emergency: Emergency;
}

/**
 * Explainability panel. Derives every line strictly from real backend
 * fields (stationaryMinutes, nearbyDevices, emergencyMode, createdAt).
 * No fabricated confidence/priority values.
 */
export default function EvidencePanel({ emergency }: EvidencePanelProps) {
  return (
    <section className="evidence" aria-label="Detection evidence">
      <h3 className="evidence__title">DETECTION EVIDENCE</h3>
      <ul className="evidence__list">
        <li>
          <Timer size={14} aria-hidden="true" className="evidence__icon" />
          Device stationary for {formatStationary(emergency.stationaryMinutes)}
        </li>
        <li>
          <Activity size={14} aria-hidden="true" className="evidence__icon" />
          {emergency.nearbyDevices} nearby{" "}
          {emergency.nearbyDevices === 1 ? "device" : "devices"} detected
        </li>
        <li>
          <MapPin size={14} aria-hidden="true" className="evidence__icon" />
          GPS position reported by the device
        </li>
        {emergency.emergencyMode && (
          <li>
            <Gauge size={14} aria-hidden="true" className="evidence__icon" />
            User activated emergency (SOS) mode
          </li>
        )}
      </ul>
      <p className="evidence__conclusion">Possible person in distress</p>
    </section>
  );
}
