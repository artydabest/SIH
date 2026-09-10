import { AlertTriangle, CheckCircle2 } from "lucide-react";
import { useToasts } from "../context/ToastContext";
import "../styles/toasts.css";

export default function Toasts() {
  const { toasts, dismissToast } = useToasts();

  return (
    <div className="toast-region" aria-live="polite">
      {toasts.map((toast) => (
        <button
          key={toast.id}
          type="button"
          className={`toast toast--${toast.kind}`}
          onClick={() => dismissToast(toast.id)}
          title="Dismiss"
        >
          {toast.kind === "error" ? (
            <AlertTriangle size={15} aria-hidden="true" />
          ) : (
            <CheckCircle2 size={15} aria-hidden="true" />
          )}
          <span>{toast.message}</span>
        </button>
      ))}
    </div>
  );
}
