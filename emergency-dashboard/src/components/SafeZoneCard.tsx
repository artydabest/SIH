import { TriangleAlert } from "lucide-react";
import "../styles/safe-zone.css";

/**
 * DEMO COMPONENT — the backend provides no safe-zone data.
 * Clearly marked as simulated; never presented as real infrastructure.
 */
export default function SafeZoneCard() {
  return (
    <section className="safe-zone" aria-label="Nearest safe zone (demo)">
      <h3 className="panel__title">Nearest safe zone</h3>
      <div className="safe-zone__card">
        <p className="safe-zone__name">City Emergency Shelter</p>
        <p className="safe-zone__distance mono">650 m</p>
        <span className="demo-chip">Demo data</span>
        <button type="button" className="btn btn--primary" disabled>
          Navigate to safety
        </button>
        <p className="safe-zone__note">
          <TriangleAlert size={12} aria-hidden="true" /> Safe-zone routing is not
          available in this prototype.
        </p>
      </div>
    </section>
  );
}
