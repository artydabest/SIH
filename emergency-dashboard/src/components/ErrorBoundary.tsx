import { Component, type ErrorInfo, type ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/**
 * Last-resort guard: if any panel throws while rendering (e.g. an unexpected
 * backend record shape), show a recoverable error screen instead of a blank
 * page. "Reload" re-mounts and re-fetches everything.
 */
class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("UI crashed while rendering:", error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            minHeight: "100vh",
            gap: "0.75rem",
            fontFamily: "monospace",
            color: "#f5f5f5",
            backgroundColor: "#090a0c",
            padding: "2rem",
            textAlign: "center",
          }}
        >
          <h2 style={{ margin: 0, letterSpacing: "0.1em" }}>UI ERROR</h2>
          <p style={{ margin: 0, color: "#a4a8ad", maxWidth: 480 }}>
            Something rendered incorrectly. Your data is safe — this is a
            display problem only.
          </p>
          <pre
            style={{
              color: "#ff3b30",
              fontSize: "0.75rem",
              maxWidth: 640,
              whiteSpace: "pre-wrap",
            }}
          >
            {this.state.error.message}
          </pre>
          <button
            type="button"
            onClick={() => {
              this.setState({ error: null });
              window.location.reload();
            }}
            style={{
              padding: "0.6rem 1.4rem",
              border: "1px solid #0a84ff",
              backgroundColor: "#0a84ff",
              color: "#fff",
              borderRadius: 4,
              cursor: "pointer",
              fontFamily: "inherit",
              fontWeight: 700,
              letterSpacing: "0.08em",
            }}
          >
            RELOAD DASHBOARD
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
