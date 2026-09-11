package com.sih.trial2;

import android.location.Location;

/**
 * Tracks how long the device has been stationary (no GPS movement beyond a
 * threshold). Feeds stationaryMinutes for emergency reports — the same signal
 * the detection network uses for confidence scoring on the backend.
 */
public class MovementTracker {

    private static final float STATIONARY_THRESHOLD_METERS = 30f;

    private Location firstStationaryLocation;
    private long stationarySinceMs;

    /** Call on every location update. */
    public void onLocationChanged(Location location) {
        if (firstStationaryLocation == null
                || location.distanceTo(firstStationaryLocation) > STATIONARY_THRESHOLD_METERS) {
            firstStationaryLocation = location;
            stationarySinceMs = System.currentTimeMillis();
        }
    }

    /** Minutes since the device last moved beyond the threshold. */
    public long getStationaryMinutes() {
        if (firstStationaryLocation == null) {
            return 0;
        }
        long elapsedMs = System.currentTimeMillis() - stationarySinceMs;
        return elapsedMs / 60_000L;
    }

    public void reset() {
        firstStationaryLocation = null;
        stationarySinceMs = 0;
    }
}
