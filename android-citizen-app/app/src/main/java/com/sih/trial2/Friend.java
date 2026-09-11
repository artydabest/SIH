package com.sih.trial2;

public class Friend {
    private final String deviceId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final boolean safe;
    private final String lastSeenAt;
    private float distanceKm;

    public Friend(String deviceId, String name, double latitude, double longitude,
                  boolean safe, String lastSeenAt) {
        this.deviceId = deviceId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.safe = safe;
        this.lastSeenAt = lastSeenAt;
    }

    public String getDeviceId() { return deviceId; }
    public String getName() { return name != null && name.length() > 0 ? name : deviceId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isSafe() { return safe; }
    public String getLastSeenAt() { return lastSeenAt; }
    public float getDistanceKm() { return distanceKm; }
    public void setDistanceKm(float distanceKm) { this.distanceKm = distanceKm; }
}
