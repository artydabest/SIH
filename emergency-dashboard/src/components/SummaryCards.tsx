import { useMemo } from "react";
import type { Emergency } from "../types/emergency";
import "../styles/summary-cards.css";

interface SummaryCardsProps {
  emergencies: Emergency[];
  loading: boolean;
}

interface Metric {
  key: string;
  label: string;
  value: number;
  tone: "danger" | "warning" | "info" | "success" | "neutral";
}

export default function SummaryCards({ emergencies, loading }: SummaryCardsProps) {
  const metrics = useMemo<Metric[]>(() => {
    const counts = { NEW: 0, ACKNOWLEDGED: 0, RESPONDING: 0, RESOLVED: 0 };
    for (const e of emergencies) counts[e.status] += 1;

    const active = counts.NEW + counts.ACKNOWLEDGED + counts.RESPONDING;

    return [
      { key: "active", label: "Active incidents", value: active, tone: "danger" },
      { key: "new", label: "New alerts", value: counts.NEW, tone: "warning" },
      { key: "responding", label: "Responding", value: counts.RESPONDING, tone: "info" },
      { key: "resolved", label: "Resolved", value: counts.RESOLVED, tone: "success" },
    ];
  }, [emergencies]);

  return (
    <section className="summary-cards" aria-label="Incident summary">
      {metrics.map((metric) => (
        <div
          key={metric.key}
          className={`summary-card summary-card--${metric.tone} ${
            loading ? "summary-card--loading" : ""
          }`}
        >
          <span className="summary-card__value mono">
            {String(metric.value).padStart(2, "0")}
          </span>
          <span className="summary-card__label">{metric.label}</span>
        </div>
      ))}
    </section>
  );
}
