export interface Toast {
  id: number;
  message: string;
  kind: "info" | "error";
}

export interface ToastApi {
  toasts: Toast[];
  pushToast: (message: string, kind?: Toast["kind"]) => void;
  dismissToast: (id: number) => void;
}
