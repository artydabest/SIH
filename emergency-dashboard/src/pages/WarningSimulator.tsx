import { useEffect, useMemo, useState } from "react";
import { Megaphone, Send, X, Clock } from "lucide-react";
import { useEmergencyData } from "../context/EmergencyDataContext";
import {
  issueWarning,
  getWarningRegions,
} from "../api/warningApi";
import type {
  SimRegion,
  WarningDisasterType,
  WarningSeverity,
} from "../types/warning";
import {
  WARNING_TYPE_LABEL,
  WARNING_SEVERITIES,
  SEVERITY_CLASS,
} from "../types/warning";
import { formatTimestampFull } from "../lib/format";
import "../styles/simulator.css";

const TYPES = Object.keys(WARNING_TYPE_LABEL) as WarningDisasterType[];

const SEVERITY_HINT: Record<WarningSeverity, string> = {
  LOW: "Advisory — general awareness",
  MEDIUM: "Watch — be ready to act",
  HIGH: "Warning — act to stay safe",
  CRITICAL: "Emergency — immediate threat to life",
};

/** "in 2 h" / "in 45 min" / "now" from minutes-from-now. */
function formatLeadTime(minutes: number): string {
  if (minutes <= 0) return "immediately";
  if (minutes < 60) return `in ${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `in ${h} h ${m} min` : `in ${h} h`;
}

export default function WarningSimulator() {
  const { warnings, withdrawWarning } = useEmergencyData();
  const [regions, setRegions] = useState<SimRegion[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [type, setType] = useState<WarningDisasterType>("HEAVY_RAINFALL");
  const [severity, setSeverity] = useState<WarningSeverity>("HIGH");
  const [regionId, setRegionId] = useState("bengaluru-urban");
  const [message, setMessage] = useState("");
  const [startInMin, setStartInMin] = useState(120);
  const [durationMin, setDurationMin] = useState(180);

  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ kind: "ok" | "error"; text: string } | null>(null);

  useEffect(() => {
    getWarningRegions()
      .then((list) => {
        setRegions(list);
        setLoadError(null);
      })
      .catch((error) =>
        setLoadError(error instanceof Error ? error.message : "Failed to load regions")
      );
  }, []);

  const region = regions.find((r) => r.id === regionId);

  // Only disaster types that are geographically plausible for the region.
  const allowedTypes = useMemo(
    () => (region ? TYPES.filter((t) => region.plausibleTypes.includes(t)) : TYPES),
    [region]
  );

  // Keep the selected type valid when the region changes.
  useEffect(() => {
    if (region && !region.plausibleTypes.includes(type)) {
      setType(region.plausibleTypes[0]);
    }
  }, [region, type]);

  const send = async () => {
    setSending(true);
    setResult(null);
    try {
      const warning = await issueWarning({
        type,
        severity,
        regionId,
        message: message.trim() || undefined,
        expectedStartInMin: startInMin,
        durationMin,
      });
      setResult({
        kind: "ok",
        text: `Sent: "${warning.title}" — broadcast to all connected citizen apps.`,
      });
      setMessage("");
    } catch (error) {
      setResult({
        kind: "error",
        text: error instanceof Error ? error.message : "Failed to send warning",
      });
    } finally {
      setSending(false);
    }
  };

  const activeWarnings = warnings.filter(
    (w) => w.status === "ACTIVE" && new Date(w.expectedEndAt).getTime() > Date.now()
  );
  const pastWarnings = warnings.filter((w) => !activeWarnings.includes(w));

  return (
    <div className="page">
      <div className="page__heading">
        <h2>Meteorological Alert Simulator</h2>
        <p className="page__sub">
          Simulated hazard warnings for live demonstrations — not connected to
          any government meteorological service.
        </p>
      </div>

      <div className="simulator-grid">
        <section className="panel simulator-form" aria-label="Issue simulated warning">
          <h3 className="panel__title">COMPOSE WARNING</h3>

          <label className="sim-field">
            <span className="sim-field__label">Disaster type</span>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as WarningDisasterType)}
            >
              {allowedTypes.map((t) => (
                <option key={t} value={t}>
                  {WARNING_TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </label>

          <label className="sim-field">
            <span className="sim-field__label">Severity</span>
            <div className="sim-severity-row" role="radiogroup" aria-label="Severity">
              {WARNING_SEVERITIES.map((s) => (
                <button
                  key={s}
                  type="button"
                  role="radio"
                  aria-checked={severity === s}
                  className={`sim-severity sim-severity--${SEVERITY_CLASS[s]} ${
                    severity === s ? "sim-severity--selected" : ""
                  }`}
                  onClick={() => setSeverity(s)}
                >
                  {s}
                </button>
              ))}
            </div>
            <span className="sim-field__hint">{SEVERITY_HINT[severity]}</span>
          </label>

          <label className="sim-field">
            <span className="sim-field__label">Region</span>
            <select value={regionId} onChange={(e) => setRegionId(e.target.value)}>
              {regions.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
            {region && <span className="sim-field__hint">{region.geoNote}</span>}
          </label>

          <label className="sim-field">
            <span className="sim-field__label">Message (optional)</span>
            <textarea
              rows={3}
              value={message}
              placeholder="Leave blank to use the standard description for this hazard"
              onChange={(e) => setMessage(e.target.value)}
            />
          </label>

          <div className="sim-field-pair">
            <label className="sim-field">
              <span className="sim-field__label">Expected start</span>
              <select
                value={startInMin}
                onChange={(e) => setStartInMin(Number(e.target.value))}
              >
                <option value={0}>Immediately</option>
                <option value={15}>In 15 minutes</option>
                <option value={30}>In 30 minutes</option>
                <option value={60}>In 1 hour</option>
                <option value={120}>In 2 hours</option>
                <option value={360}>In 6 hours</option>
                <option value={720}>In 12 hours</option>
                <option value={1440}>In 24 hours</option>
              </select>
            </label>

            <label className="sim-field">
              <span className="sim-field__label">Duration</span>
              <select
                value={durationMin}
                onChange={(e) => setDurationMin(Number(e.target.value))}
              >
                <option value={60}>1 hour</option>
                <option value={180}>3 hours</option>
                <option value={360}>6 hours</option>
                <option value={720}>12 hours</option>
                <option value={1440}>24 hours</option>
                <option value={2880}>48 hours</option>
              </select>
            </label>
          </div>

          <div className="sim-send-row">
            <span className="sim-lead mono">
              <Clock size={12} aria-hidden="true" />
              Expected {formatLeadTime(startInMin)}
            </span>
            <button
              type="button"
              className="btn btn--danger btn--lg"
              disabled={sending}
              onClick={() => void send()}
            >
              <Send size={14} aria-hidden="true" />
              {sending ? "SENDING…" : "SEND WARNING"}
            </button>
          </div>

          {result && (
            <p className={result.kind === "ok" ? "sim-result sim-result--ok" : "sim-result sim-result--error"}>
              {result.text}
            </p>
          )}
          {loadError && <p className="sim-result sim-result--error">{loadError}</p>}
        </section>

        <section className="panel" aria-label="Issued warnings">
          <div className="panel__heading">
            <h3 className="panel__title">SIMULATED WARNINGS</h3>
            <span className="panel__count mono">{activeWarnings.length} active</span>
          </div>

          {activeWarnings.length === 0 && pastWarnings.length === 0 ? (
            <p className="muted">
              No warnings issued yet. Use the form to send your first simulated warning.
            </p>
          ) : (
            <>
              {activeWarnings.map((warning) => (
                <article
                  key={warning._id}
                  className={`sim-warning sim-warning--${SEVERITY_CLASS[warning.severity]}`}
                >
                  <header className="sim-warning__head">
                    <span className={`sim-sev-chip sim-sev-chip--${SEVERITY_CLASS[warning.severity]}`}>
                      {warning.severity}
                    </span>
                    <strong className="sim-warning__title">{warning.title}</strong>
                    <button
                      type="button"
                      className="sim-warning__withdraw"
                      onClick={() => void withdrawWarning(warning._id)}
                      aria-label={`Withdraw ${warning.title}`}
                    >
                      <X size={13} aria-hidden="true" /> WITHDRAW
                    </button>
                  </header>
                  <p className="sim-warning__desc">{warning.description}</p>
                  <footer className="sim-warning__meta mono">
                    <Megaphone size={11} aria-hidden="true" /> {warning.source} ·
                    issued {formatTimestampFull(warning.issuedAt)} · ends{" "}
                    {formatTimestampFull(warning.expectedEndAt)}
                  </footer>
                </article>
              ))}

              {pastWarnings.length > 0 && (
                <div className="sim-past">
                  <h4 className="sim-past__title">HISTORICAL (expired or withdrawn)</h4>
                  {pastWarnings.map((warning) => (
                    <div key={warning._id} className="sim-past__row mono">
                      <span className={`sim-sev-chip sim-sev-chip--${SEVERITY_CLASS[warning.severity]}`}>
                        {warning.severity}
                      </span>
                      <span className="sim-past__name">{warning.title}</span>
                      <span className="sim-past__state">
                        {warning.status === "CANCELLED" ? "WITHDRAWN" : "EXPIRED"}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
