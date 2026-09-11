package com.sih.trial2;

import java.util.List;

/** A disaster alert broadcast from the backend with safety instructions. */
public class DisasterAlert {
    public final String id;
    public final String type;
    public final String message;
    public final Double latitude;
    public final Double longitude;
    public final Double radiusKm;
    public final boolean active;
    public final List<String> instructions;

    public DisasterAlert(String id, String type, String message, Double latitude, Double longitude,
                         Double radiusKm, boolean active, List<String> instructions) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        this.active = active;
        this.instructions = instructions;
    }

    public boolean hasZone() {
        return latitude != null && longitude != null && radiusKm != null;
    }
}
