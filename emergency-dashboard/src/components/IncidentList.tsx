import type { Emergency } from "../types/emergency";
import IncidentCard from "./IncidentCard";
import { ShieldCheck } from "lucide-react";
import "../styles/incident-list.css";

interface IncidentListProps {
  emergencies: Emergency[];
  loading: boolean;
  now: number;
  updatingId: string | null;
  selectedId: string | null;
  onStatusUpdate: (id: string, nextStatus: Emergency["status"]) => void;
  onSelect: (emergency: Emergency) => void;
}

function SkeletonCard() {
  return (
    <div className="incident-card skeleton" aria-hidden="true">
      <div className="skeleton-line skeleton-line--w60" />
      <div className="skeleton-line skeleton-line--w90" />
      <div className="skeleton-line skeleton-line--w40" />
      <div className="skeleton-btn" />
    </div>
  );
}

export default function IncidentList({
  emergencies,
  loading,
  now,
  updatingId,
  selectedId,
  onStatusUpdate,
  onSelect,
}: IncidentListProps) {
  if (loading && emergencies.length === 0) {
    return (
      <div className="incident-list" role="status" aria-label="Loading incidents">
        <SkeletonCard />
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  if (emergencies.length === 0) {
    return (
      <div className="empty-state">
        <ShieldCheck size={30} strokeWidth={1.6} aria-hidden="true" />
        <h3>ALL CLEAR</h3>
        <p>No active incidents detected.</p>
        <p className="empty-state__sub">
          The responder network is currently quiet.
        </p>
      </div>
    );
  }

  return (
    <div className="incident-list">
      {emergencies.map((emergency) => (
        <IncidentCard
          key={emergency._id}
          emergency={emergency}
          now={now}
          updatingId={updatingId}
          onStatusUpdate={onStatusUpdate}
          onSelect={onSelect}
          selected={selectedId === emergency._id}
        />
      ))}
    </div>
  );
}
