export interface Toast {
  id: number;
  message: string;
  kind: "info" | "error";
}
