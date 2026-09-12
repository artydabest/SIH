package com.sih.trial2;

import java.util.ArrayList;
import java.util.List;

/**
 * A simulated meteorological warning issued by the demo backend.
 * Clearly labeled as simulated data — never presented as a real
 * government meteorological product.
 */
public class Warning {
    public final String id;
    public final String source;
    public final String type;       // e.g. HEAVY_RAINFALL
    public final String severity;   // LOW / MEDIUM / HIGH / CRITICAL
    public final String title;
    public final String description;
    public final String regionName;
    public final double latitude;
    public final double longitude;
    public final double radiusKm;
    public final long issuedAtMs;
    public final long expectedStartAtMs;
    public final long expectedEndAtMs;
    public final List<String> immediateActions;
    public final List<String> avoidActions;
    public final List<String> prepareActions;
    public final String status;     // ACTIVE / EXPIRED / CANCELLED
    /** Computed server-side: ACTIVE status and not past the expected end. */
    public final boolean active;

    public Warning(String id, String source, String type, String severity,
                   String title, String description, String regionName,
                   double latitude, double longitude, double radiusKm,
                   long issuedAtMs, long expectedStartAtMs, long expectedEndAtMs,
                   List<String> immediateActions, List<String> avoidActions,
                   List<String> prepareActions, String status, boolean active) {
        this.id = id;
        this.source = source;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.regionName = regionName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        this.issuedAtMs = issuedAtMs;
        this.expectedStartAtMs = expectedStartAtMs;
        this.expectedEndAtMs = expectedEndAtMs;
        this.immediateActions = immediateActions != null ? immediateActions : new ArrayList<>();
        this.avoidActions = avoidActions != null ? avoidActions : new ArrayList<>();
        this.prepareActions = prepareActions != null ? prepareActions : new ArrayList<>();
        this.status = status;
        this.active = active;
    }

    /** Human-readable disaster label: HEAVY_RAINFALL → Heavy Rainfall. */
    public String typeLabel() {
        String[] parts = type.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase(java.util.Locale.US));
        }
        return sb.length() > 0 ? sb.toString() : type;
    }

    /** Is the expected start time still in the future (advance warning)? */
    public boolean isAdvance() {
        return expectedStartAtMs > System.currentTimeMillis();
    }

    /** Milliseconds until expected start (negative once started). */
    public long msUntilStart() {
        return expectedStartAtMs - System.currentTimeMillis();
    }

    /** Milliseconds until the warning ends (negative once expired). */
    public long msUntilEnd() {
        return expectedEndAtMs - System.currentTimeMillis();
    }

    /** Distance from a coordinate to the warning's region center, in km. */
    public float distanceKmFrom(double lat, double lon) {
        float[] result = new float[1];
        android.location.Location.distanceBetween(lat, lon, latitude, longitude, result);
        return result[0] / 1000f;
    }

    /**
     * Relevance of this warning to the user's position:
     * inside the radius → DIRECT; within 2x radius → NEARBY; else INFORMATIONAL.
     */
    public Relevance relevanceFor(double lat, double lon) {
        float distanceKm = distanceKmFrom(lat, lon);
        if (distanceKm <= radiusKm) return Relevance.DIRECT;
        if (distanceKm <= radiusKm * 2) return Relevance.NEARBY;
        return Relevance.INFORMATIONAL;
    }

    public enum Relevance { DIRECT, NEARBY, INFORMATIONAL }
}
