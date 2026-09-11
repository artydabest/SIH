import SafeZone from "./models/SafeZone";
import Volunteer from "./models/Volunteer";

const SEED_SAFE_ZONES = [
  { name: "City Emergency Shelter", latitude: 12.9716, longitude: 77.5946, capacity: 500, kind: "SHELTER" },
  { name: "General Hospital", latitude: 12.9352, longitude: 77.6245, capacity: 200, kind: "HOSPITAL" },
  { name: "Relief Camp North", latitude: 12.998, longitude: 77.58, capacity: 300, kind: "RELIEF_CAMP" },
  { name: "Supply Depot South", latitude: 12.9082, longitude: 77.6474, capacity: 0, kind: "SUPPLY_DEPOT" },
] as const;

const SEED_VOLUNTEERS = [
  { name: "Volunteer A", latitude: 12.9725, longitude: 77.592, available: true, phone: "+91-90000-00001" },
  { name: "Volunteer B", latitude: 12.94, longitude: 77.61, available: true, phone: "+91-90000-00002" },
  { name: "Volunteer C", latitude: 12.98, longitude: 77.65, available: false, phone: "+91-90000-00003" },
] as const;

/** Idempotent: only seeds collections that are still empty. */
export async function seedDatabase(): Promise<void> {
  const zoneCount = await SafeZone.estimatedDocumentCount();
  if (zoneCount === 0) {
    await SafeZone.insertMany(SEED_SAFE_ZONES.map((zone) => ({ ...zone })));
    console.log(`🗄️ Seeded ${SEED_SAFE_ZONES.length} safe zones`);
  } else {
    console.log(`🗄️ Safe zones already seeded (${zoneCount})`);
  }

  const volunteerCount = await Volunteer.estimatedDocumentCount();
  if (volunteerCount === 0) {
    await Volunteer.insertMany(SEED_VOLUNTEERS.map((v) => ({ ...v })));
    console.log(`🗄️ Seeded ${SEED_VOLUNTEERS.length} volunteers`);
  } else {
    console.log(`🗄️ Volunteers already seeded (${volunteerCount})`);
  }
}
