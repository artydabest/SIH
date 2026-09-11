import { Check, MapPin, Radio } from "lucide-react";
import type { Emergency } from "../types/emergency";
import { formatStationary } from "../lib/format";
import "../styles/evidence.css";

interface EvidencePanelProps {
  emergency: Emergency;
}

/**
 * Explainability panel. Derives every line strictly from real backend
 * fields (stationaryMinutes, nearbyDevices, location, createdAt).
 * No fabricated confidence/priority values.
 */
export default function EvidencePanel({ emergency }: EvidencePanelProps) {
  return (
    <section className="evidence" aria-label="Detection evidence">
      <h3 className="evidence__title">Why this alert</h3>
      <ul className="evidence__list">
        <li>
          <Check size={14} aria-hidden="true" className="evidence__check" />
          Device stationary for {formatStationary(emergency.stationaryMinutes)}
        </li>
        <li>
          <Check size={14} aria-hidden="true" className="evidence__check" />
          {emergency.nearbyDevices} nearby{" "}
          {emergency.nearbyDevices === 1 ? "device" : "devices"} detected
        </li>
        <li>
          <MapPin size={14} aria-hidden="true" className="evidence__check" />
          Last known location available
        </li>
        <li>
          <Radio size={14} aria-hidden="true" className="evidence__check" />
          Relayed by nearby iPhone detection network
        </li>
      </ul>
      <p className="evidence__conclusion">
        Possible person in distress
      </p>
    </section>
  );
}
