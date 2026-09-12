package com.sih.trial2;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed response of GET /api/safezones/recommend — the backend's explained
 * shelter recommendation. Occupancy figures in here are SIMULATED DEMO data
 * (the backend labels them as such; we keep that label visible in the UI).
 */
public class ShelterAdvice {

    /** What the app wants the user to do: EVACUATE, MONITOR_STAY_PUT, NO_ACTION. */
    public final String advice;
    public final String adviceReason;

    /** The active warning that drove the advice (null when none). */
    public final String warningTitle;
    public final String warningSeverity;

    public final List<RecommendedShelter> shelters;

    public ShelterAdvice(String advice,
                         String adviceReason,
                         String warningTitle,
                         String warningSeverity,
                         List<RecommendedShelter> shelters) {
        this.advice = advice;
        this.adviceReason = adviceReason;
        this.warningTitle = warningTitle;
        this.warningSeverity = warningSeverity;
        this.shelters = shelters;
    }

    /** One recommended shelter with distance/travel time and human reasons. */
    public static class RecommendedShelter {
        public final String name;
        public final String kind;
        public final double latitude;
        public final double longitude;
        public final double distanceKm;
        public final int travelMinutes;
        public final String reason;
        public final boolean recommended;

        public RecommendedShelter(String name, String kind, double latitude, double longitude,
                                  double distanceKm, int travelMinutes, String reason,
                                  boolean recommended) {
            this.name = name;
            this.kind = kind;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distanceKm = distanceKm;
            this.travelMinutes = travelMinutes;
            this.reason = reason;
            this.recommended = recommended;
        }
    }

    /** True when the backend says the user should move to a shelter now. */
    public boolean isEvacuate() {
        return "EVACUATE".equals(advice);
    }
}
