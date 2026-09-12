import { TriangleAlert } from "lucide-react";
import type { SimWarning } from "../types/warning";
import { SEVERITY_CLASS, WARNING_TYPE_LABEL } from "../types/warning";
import { useNow } from "../hooks/useNow";
import "../styles/warnings.css";

interface ActiveWarningsPanelProps {
  warnings: SimWarning[];
}

function countdown(target: string, now: number): string {
  const ms = new Date(target).getTime() - now;
  if (ms <= 0) return "in progress";
  const totalSec = Math.floor(ms / 1000);
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/** Active simulated warnings for the responder overview. Clearly labeled DEMO. */
export default function ActiveWarningsPanel({ warnings }: ActiveWarningsPanelProps) {
  const now = useNow(1000).getTime();
  const active = warnings.filter(
    (w) => w.status === "ACTIVE" && new Date(w.expectedEndAt).getTime() > now
  );

  if (active.length === 0) {
    return (
      <section className="warnings-panel" aria-label="Active simulated warnings">
        <h3 className="panel__title">ACTIVE SIMULATED WARNINGS</h3>
        <p className="warnings-panel__empty">
          No active simulated warnings. Issue one from the Meteorological Alert
          Simulator.
        </p>
      </section>
    );
  }

  return (
    <section className="warnings-panel" aria-label="Active simulated warnings">
      <div className="panel__heading">
        <h3 className="panel__title">ACTIVE SIMULATED WARNINGS</h3>
        <span className="demo-chip-inline">DEMO DATA</span>
      </div>

      {active.map((warning) => {
        const notStarted = new Date(warning.expectedStartAt).getTime() > now;
        return (
          <article
            key={warning._id}
            className={`sim-warning sim-warning--${SEVERITY_CLASS[warning.severity]} ${
              warning.severity === "CRITICAL" ? "sim-warning--critical-urgency" : ""
            }`}
          >
            <header className="sim-warning__head">
              <TriangleAlert size={14} aria-hidden="true" className="sim-warning__icon" />
              <span className={`sim-sev-chip sim-sev-chip--${SEVERITY_CLASS[warning.severity]}`}>
                {warning.severity}
              </span>
              <strong className="sim-warning__title">{warning.title}</strong>
            </header>
            <p className="sim-warning__desc">{warning.description}</p>
            <div className="warnings-panel__timing mono">
              <span>
                {WARNING_TYPE_LABEL[warning.type]} · area: {warning.regionName}
              </span>
              <span>
                {notStarted
                  ? `Expected in ${countdown(warning.expectedStartAt, now)}`
                  : `Ends in ${countdown(warning.expectedEndAt, now)}`}
              </span>
            </div>
            <ul className="warnings-panel__actions">
              {warning.instructions.immediate.slice(0, 3).map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          </article>
        );
      })}
    </section>
  );
}
