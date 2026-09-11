import { calculateConfidence } from "./confidence";

const tests = [
  {
    name: "Normal phone",
    stationaryMinutes: 1,
    nearbyDevices: 0,
    emergencyMode: false,
  },
  {
    name: "Phone stationary",
    stationaryMinutes: 5,
    nearbyDevices: 0,
    emergencyMode: false,
  },
  {
    name: "Possible emergency",
    stationaryMinutes: 10,
    nearbyDevices: 1,
    emergencyMode: true,
  },
  {
    name: "Strong confirmation",
    stationaryMinutes: 20,
    nearbyDevices: 3,
    emergencyMode: true,
  },
];

for (const test of tests) {
  const result = calculateConfidence(test);

  console.log(test.name);
  console.log(result);
  console.log("--------------------");
}