import { Users } from "lucide-react";
import "../styles/page.css";

/**
 * PLACEHOLDER — the backend does not yet provide people/safety-circle data.
 * Per the design brief: only implement when backend functionality exists.
 */
export default function People() {
  return (
    <div className="page">
      <div className="page__heading">
        <h2>People / Safety Circle</h2>
        <p className="page__sub">Friend and family safety status</p>
      </div>
      <div className="empty-state">
        <Users size={30} strokeWidth={1.6} aria-hidden="true" />
        <h3>NOT YET AVAILABLE</h3>
        <p>
          Safety Circle requires people-status APIs that the backend does not
          provide yet. This section will activate when the feature ships.
        </p>
      </div>
    </div>
  );
}
