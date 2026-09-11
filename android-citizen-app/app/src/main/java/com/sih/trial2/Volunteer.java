package com.sih.trial2;

public class Volunteer {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final boolean available;
    private float distanceKm;

    public Volunteer(String name, double latitude, double longitude, boolean available) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isAvailable() { return available; }
    public float getDistanceKm() { return distanceKm; }
    public void setDistanceKm(float distanceKm) { this.distanceKm = distanceKm; }
}
