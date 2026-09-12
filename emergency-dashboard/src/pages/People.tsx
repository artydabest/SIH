import { Users, MapPin, ShieldCheck, ShieldAlert } from "lucide-react";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { formatCoords, formatRelative } from "../lib/format";
import { MONO } from "../lib/format";
import "../styles/page.css";

export default function People() {
  const { people } = useEmergencyData();
  const now = Date.now();

  return (
    <div className="page">
      <div className="page__heading">
        <h2>People / Safety Circle</h2>
        <p className="page__sub">
          Latest check-ins from citizen devices — updated live
        </p>
      </div>

      {people.length === 0 ? (
        <div className="empty-state">
          <Users size={30} strokeWidth={1.6} aria-hidden="true" />
          <h3>NO CHECK-INS YET</h3>
          <p>
            No devices have checked in. Open the citizen app and it will check
            in automatically with its location.
          </p>
        </div>
      ) : (
        <div className="people-grid">
          {people.map((person) => (
            <article
              key={person._id}
              className={`panel person-card ${person.safe ? "" : "person-card--unsafe"}`}
              aria-label={`${person.name ?? person.deviceId} safety status`}
            >
              <header className="person-card__top">
                <strong>{person.name ?? person.deviceId}</strong>
                {person.safe ? (
                  <span className="person-chip person-chip--safe">
                    <ShieldCheck size={12} aria-hidden="true" /> SAFE
                  </span>
                ) : (
                  <span className="person-chip person-chip--unsafe">
                    <ShieldAlert size={12} aria-hidden="true" /> NEEDS HELP
                  </span>
                )}
              </header>
              <dl className="fact-grid">
                <div className="fact">
                  <dt>Location</dt>
                  <dd className={MONO}>{formatCoords(person.latitude, person.longitude)}</dd>
                </div>
                <div className="fact">
                  <dt>Last seen</dt>
                  <dd className={MONO}>{formatRelative(person.lastSeenAt, now)}</dd>
                </div>
              </dl>
              <p className="person-card__hint">
                <MapPin size={12} aria-hidden="true" /> Shown on the Rescue Map
              </p>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
