export interface Person {
  _id: string;
  deviceId: string;
  name?: string;
  latitude: number;
  longitude: number;
  safe: boolean;
  lastSeenAt: string;
}
