export interface Volunteer {
  _id: string;
  name: string;
  latitude: number;
  longitude: number;
  available: boolean;
  phone?: string;
}
