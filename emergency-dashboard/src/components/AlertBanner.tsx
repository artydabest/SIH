import { TriangleAlert } from "lucide-react";
import type { DisasterAlert } from "../types/alert";
import { DISASTER_LABEL } from "../types/alert";
import "../styles/alert-banner.css";

interface AlertBannerProps {
  alert: DisasterAlert;
}

/** Full-width disaster banner — visible on every page while the alert is active. */
export default function AlertBanner({ alert }: AlertBannerProps) {
  return (
    <div className="alert-banner" role="alert">
      <div className="alert-banner__head">
        <TriangleAlert size={18} aria-hidden="true" />
        <strong>
          {DISASTER_LABEL[alert.type]?.toUpperCase() ?? alert.type} ALERT
        </strong>
        <span className="alert-banner__msg">{alert.message}</span>
      </div>
      {alert.instructions && alert.instructions.length > 0 && (
        <ul className="alert-banner__instructions">
          {alert.instructions.slice(0, 4).map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
