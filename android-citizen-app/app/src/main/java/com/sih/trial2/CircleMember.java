package com.sih.trial2;

/** A member of the user's Safe Circle with their latest reported status. */
public class CircleMember {
    public enum SafetyStatus { SAFE, UNKNOWN, POSSIBLE_EMERGENCY }

    public final String deviceId;
    public final String name;
    public final String lastSeenAt; // ISO-8601, nullable
    public final Double latitude;
    public final Double longitude;
    public final SafetyStatus safetyStatus;

    public CircleMember(String deviceId, String name, String lastSeenAt,
                        Double latitude, Double longitude, SafetyStatus safetyStatus) {
        this.deviceId = deviceId;
        this.name = name;
        this.lastSeenAt = lastSeenAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.safetyStatus = safetyStatus;
    }

    public String displayName() {
        return name != null && name.length() > 0 ? name : deviceId;
    }
}
