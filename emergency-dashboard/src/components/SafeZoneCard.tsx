import { TriangleAlert } from "lucide-react";
import type { SafeZone } from "../types/safezone";
import { SAFE_ZONE_KIND_LABEL } from "../types/safezone";
import { formatCoords } from "../lib/format";
import { osmUrl } from "../lib/format";
import "../styles/safe-zone.css";

interface SafeZoneCardProps {
  safeZones: SafeZone[];
}

/** Nearest safe-zone summary using REAL backend data. */
export default function SafeZoneCard({ safeZones }: SafeZoneCardProps) {
  if (safeZones.length === 0) {
    return (
      <section className="safe-zone" aria-label="Safe zones">
        <h3 className="panel__title">SAFE ZONES</h3>
        <p className="muted">No safe zones registered in the backend yet.</p>
      </section>
    );
  }

  return (
    <section className="safe-zone" aria-label="Safe zones">
      <h3 className="panel__title">SAFE ZONES</h3>
      <div className="safe-zone__list">
        {safeZones.slice(0, 3).map((zone) => (
          <a
            key={zone._id}
            className="safe-zone__card safe-zone__card--link"
            href={osmUrl(zone.latitude, zone.longitude)}
            target="_blank"
            rel="noreferrer"
          >
            <p className="safe-zone__name">{zone.name}</p>
            <p className="safe-zone__kind">
              {SAFE_ZONE_KIND_LABEL[zone.kind] ?? zone.kind}
              {zone.capacity != null && zone.capacity > 0
                ? ` · capacity ${zone.capacity}`
                : ""}
            </p>
            <span className="safe-zone__coords mono">
              {formatCoords(zone.latitude, zone.longitude)}
            </span>
          </a>
        ))}
      </div>
      <p className="safe-zone__note">
        <TriangleAlert size={12} aria-hidden="true" /> Routing is a prototype —
        links open the location on a map.
      </p>
    </section>
  );
}
