package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private LinearLayout reportsContainer;
    private TextView eventText;
    private TextView countText;

    private final Set<String> reportedDeviceIds = new HashSet<>();
    private final ArrayList<EmergencyReport> reports = new ArrayList<>();

    private String currentEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button simulateButton = findViewById(R.id.simulateButton);
        Button resetButton = findViewById(R.id.resetButton);

        reportsContainer = findViewById(R.id.reportsContainer);
        eventText = findViewById(R.id.eventText);
        countText = findViewById(R.id.countText);

        simulateButton.setOnClickListener(
                v -> simulateNearbyDevices()
        );

        resetButton.setOnClickListener(
                v -> resetSimulation()
        );
    }

    private void simulateNearbyDevices() {

        if (currentEventId == null) {

            currentEventId =
                    "EVENT_" +
                            UUID.randomUUID()
                                    .toString()
                                    .substring(0, 8)
                                    .toUpperCase();

            eventText.setText(
                    "Emergency Event: " + currentEventId
            );
        }

        String[] devices = {
                "DEVICE_A",
                "DEVICE_B",
                "DEVICE_C",
                "DEVICE_D"
        };
        String[] names = {
                "Rahul",
                "Priya",
                "Arjun",
                "Ananya"
        };

        String[] phones = {
                "+919000000001",
                "+919000000002",
                "+919000000003",
                "+919000000004"
        };

        String[] locations = {
                "17.3850, 78.4867",
                "17.3900, 78.4800",
                "17.3750, 78.4900",
                "17.3950, 78.5000"
        };

        String[] signalStrength = {
                "STRONG",
                "MEDIUM",
                "WEAK",
                "MEDIUM"
        };

        // Only A and B report an emergency.
        boolean[] reporting = {
                true,
                true,
                false,
                false
        };

        String timestamp =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(new Date());

        int newReports = 0;

        for (int i = 0; i < devices.length; i++) {

            String deviceId = devices[i];

            // Prevent duplicate reports.
            if (reportedDeviceIds.contains(deviceId)) {
                continue;
            }

            if (!reporting[i]) {

                addNearbyCard(
                        deviceId,
                        locations[i],
                        signalStrength[i]
                );

                continue;
            }

            reportedDeviceIds.add(deviceId);

            EmergencyReport report =
                    new EmergencyReport(
                            deviceId,
                            currentEventId,
                            timestamp,
                            locations[i],
                            "EMERGENCY_REPORTED",
                            signalStrength[i],
                            names[i],
                            phones[i]
                    );

            reports.add(report);

            addReportCard(report);

            newReports++;

            sendEmergencyReport(report);
        }

        countText.setText(
                "Independent emergency reports: "
                        + reportedDeviceIds.size()
                        + " / 4"
        );

        if (newReports > 0) {

            Toast.makeText(
                    this,
                    newReports +
                            " emergency reports generated",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "No new emergency reports",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================
    // EMERGENCY DEVICE CARD
    // =========================

    private void addReportCard(EmergencyReport report) {

        LinearLayout card = createCard();

        TextView device =
                createText(
                        "📱 " +
                                report.deviceId +
                                "    🚨 REPORTING",
                        19,
                        Color.rgb(180, 25, 25),
                        true
                );

        TextView name =
                createText(
                        "👤 " + report.name,
                        16,
                        Color.DKGRAY,
                        true
                );

        TextView event = createText(
                "Event: " + report.eventId,
                14,
                Color.DKGRAY,
                false
        );

        TextView location = createText(
                "📍 " + report.location,
                14,
                Color.DKGRAY,
                false
        );

        TextView signal = createText(
                "📶 Signal: " + report.signalStrength,
                14,
                Color.DKGRAY,
                false
        );

        TextView time = createText(
                "🕒 " + report.timestamp,
                14,
                Color.DKGRAY,
                false
        );

        TextView status = createText(
                "● " + report.status +
                        "   ✓ REPORT CREATED",
                14,
                Color.rgb(30, 130, 70),
                true
        );

        Button openButton = new Button(this);

        openButton.setText("OPEN DEVICE");

        openButton.setTextColor(Color.WHITE);

        openButton.setBackgroundColor(
                Color.rgb(70, 80, 220)
        );

        openButton.setOnClickListener(
                v -> openDevicePage(
                        report.deviceId,
                        report.eventId,
                        report.location,
                        report.signalStrength,
                        report.timestamp,
                        report.status
                )
        );

        // Clicking the card also opens the page.
        card.setOnClickListener(
                v -> openDevicePage(
                        report.deviceId,
                        report.eventId,
                        report.location,
                        report.signalStrength,
                        report.timestamp,
                        report.status
                )
        );

        card.addView(device);
        card.addView(name);
        card.addView(event);
        card.addView(location);
        card.addView(signal);
        card.addView(time);
        card.addView(status);
        card.addView(openButton);

        reportsContainer.addView(card);
    }

    // =========================
    // NEARBY DEVICE CARD
    // =========================

    private void addNearbyCard(
            String deviceId,
            String location,
            String signal) {

        LinearLayout card = createCard();

        TextView device = createText(
                "📱 " + deviceId + "    🟢 NEARBY",
                19,
                Color.rgb(45, 100, 75),
                true
        );

        TextView locationText = createText(
                "📍 " + location,
                14,
                Color.DKGRAY,
                false
        );

        TextView signalText = createText(
                "📶 Signal: " + signal,
                14,
                Color.DKGRAY,
                false
        );

        TextView status = createText(
                "○ No emergency report",
                14,
                Color.GRAY,
                false
        );

        Button openButton = new Button(this);

        openButton.setText("OPEN DEVICE");

        openButton.setTextColor(Color.WHITE);

        openButton.setBackgroundColor(
                Color.rgb(70, 80, 220)
        );

        openButton.setOnClickListener(
                v -> openDevicePage(
                        deviceId,
                        currentEventId,
                        location,
                        signal,
                        "Not reporting",
                        "NEARBY"
                )
        );

        card.setOnClickListener(
                v -> openDevicePage(
                        deviceId,
                        currentEventId,
                        location,
                        signal,
                        "Not reporting",
                        "NEARBY"
                )
        );

        card.addView(device);
        card.addView(locationText);
        card.addView(signalText);
        card.addView(status);
        card.addView(openButton);

        reportsContainer.addView(card);
    }

    // =========================
    // OPEN DEVICE PAGE
    // =========================

    private void openDevicePage(
            String deviceId,
            String eventId,
            String location,
            String signal,
            String timestamp,
            String status) {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        DeviceDetailsActivity.class
                );

        intent.putExtra("deviceId", deviceId);
        intent.putExtra("eventId", eventId);
        intent.putExtra("location", location);
        intent.putExtra("signal", signal);
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("status", status);

// Contact information
        String name = "";
        String phone = "";

        if (deviceId.equals("DEVICE_A")) {
            name = "Your Contact name";
            phone = "6855";
        } else if (deviceId.equals("DEVICE_B")) {
            name = "Emergency Contact";
            phone = "1234567890";
        }

        intent.putExtra("name", name);
        intent.putExtra("phone", phone);

        startActivity(intent);
        intent.putExtra("name", name);
        intent.putExtra("phone", phone);

        startActivity(intent);

        if (deviceId.equals("DEVICE_A")) {
            name = "Rahul Sharma";
            phone = "685-123-4567";
        } else if (deviceId.equals("DEVICE_B")) {
            name = "Priya Reddy";
            phone = "685-234-5678";
        } else if (deviceId.equals("DEVICE_C")) {
            name = "Arjun Kumar";
            phone = "685-345-6789";
        } else if (deviceId.equals("DEVICE_D")) {
            name = "Sneha Patel";
            phone = "685-456-7890";
        }

        intent.putExtra("name", name);
        intent.putExtra("phone", phone);

        startActivity(intent);
    }

    // =========================
    // CREATE CARD
    // =========================

    private LinearLayout createCard() {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                28,
                24,
                28,
                24
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                20
        );

        card.setLayoutParams(params);

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(Color.WHITE);

        background.setCornerRadius(28);

        background.setStroke(
                2,
                Color.rgb(230, 230, 235)
        );

        card.setBackground(background);

        card.setClickable(true);

        return card;
    }

    // =========================
    // TEXT
    // =========================

    private TextView createText(
            String text,
            int size,
            int color,
            boolean bold) {

        TextView view =
                new TextView(this);

        view.setText(text);

        view.setTextSize(size);

        view.setTextColor(color);

        if (bold) {

            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        view.setPadding(
                0,
                4,
                0,
                4
        );

        return view;
    }

    // =========================
    // RESET
    // =========================

    private void resetSimulation() {

        reportedDeviceIds.clear();

        reports.clear();

        currentEventId = null;

        reportsContainer.removeAllViews();

        eventText.setText(
                "Emergency Event: Waiting..."
        );

        countText.setText(
                "Independent emergency reports: 0 / 4"
        );

        Toast.makeText(
                this,
                "Simulation reset",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================
    // SEND TO BACKEND
    // =========================

    private void sendEmergencyReport(
            EmergencyReport report) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(
                                "http://10.0.2.2:3000/api/emergency"
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setDoOutput(true);

                JSONObject json =
                        new JSONObject();

                json.put(
                        "deviceId",
                        report.deviceId
                );

                json.put(
                        "eventId",
                        report.eventId
                );

                json.put(
                        "timestamp",
                        report.timestamp
                );

                json.put(
                        "location",
                        report.location
                );

                json.put(
                        "status",
                        report.status
                );

                json.put(
                        "signalStrength",
                        report.signalStrength
                );

                json.put(
                        "stationaryMinutes",
                        10
                );

                json.put(
                        "nearbyDevices",
                        reportedDeviceIds.size()
                );

                json.put(
                        "emergencyMode",
                        true
                );

                String body =
                        json.toString();

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        body.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                outputStream.flush();
                outputStream.close();

                int responseCode =
                        connection.getResponseCode();

                System.out.println(
                        report.deviceId +
                                " -> HTTP " +
                                responseCode
                );

                connection.disconnect();

            } catch (Exception e) {

                System.out.println(
                        "Report failed for "
                                + report.deviceId
                                + ": "
                                + e.getMessage()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
    private void openDeviceDetails(EmergencyReport report) {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        DeviceDetailsActivity.class
                );

        intent.putExtra("deviceId", report.deviceId);
        intent.putExtra("eventId", report.eventId);
        intent.putExtra("name", report.name);
        intent.putExtra("phone", report.phone);
        intent.putExtra("location", report.location);
        intent.putExtra("timestamp", report.timestamp);
        intent.putExtra("signal", report.signalStrength);
        intent.putExtra("status", report.status);

        startActivity(intent);
    }
}