package com.example.myapplication;

public class EmergencyReport {

    String deviceId;
    String eventId;
    String timestamp;
    String location;
    String status;
    String signalStrength;
    String name;
    String phone;

    public EmergencyReport(
            String deviceId,
            String eventId,
            String timestamp,
            String location,
            String status,
            String signalStrength,
            String name,
            String phone) {

        this.deviceId = deviceId;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.location = location;
        this.status = status;
        this.signalStrength = signalStrength;
        this.name = name;
        this.phone = phone;
    }
}