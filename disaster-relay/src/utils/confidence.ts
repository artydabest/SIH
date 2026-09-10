export type ConfidenceLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

interface ConfidenceInput {
  stationaryMinutes: number;
  nearbyDevices: number;
  emergencyMode: boolean;
}

interface ConfidenceResult {
  score: number;
  level: ConfidenceLevel;
}

export function calculateConfidence(
  input: ConfidenceInput
): ConfidenceResult {
  let score = 0;

  const {
    stationaryMinutes,
    nearbyDevices,
    emergencyMode,
  } = input;

  // Stationary phone
  if (stationaryMinutes >= 20) {
    score += 40;
  } else if (stationaryMinutes >= 10) {
    score += 30;
  } else if (stationaryMinutes >= 5) {
    score += 20;
  }

  // Nearby phones confirming the signal
  if (nearbyDevices >= 3) {
    score += 30;
  } else if (nearbyDevices === 2) {
    score += 20;
  } else if (nearbyDevices === 1) {
    score += 10;
  }

  // Emergency/disaster mode
  if (emergencyMode) {
    score += 20;
  }

  score = Math.min(score, 100);

  let level: ConfidenceLevel;

  if (score >= 90) {
    level = "CRITICAL";
  } else if (score >= 70) {
    level = "HIGH";
  } else if (score >= 40) {
    level = "MEDIUM";
  } else {
    level = "LOW";
  }

  return {
    score,
    level,
  };
}