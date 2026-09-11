package com.sih.trial2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
       {
    private LocationHelper locationHelper;
    private TextView locationText;
    private TextView safePlacesText;
    private TextView volunteersText;
    private TextView incidentStatusText;
    private Button refreshButton;
    private Button sosButton;
    private WebView rescueMapWebView;
    private Volunteer nearestVolunteer;

    /* Shared-backend sync: same store the responder dashboard polls. */
    private static final long POLL_INTERVAL_MS = 7000;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService networkExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean incidentsPollRunning = false;


    private final List<SafePlace> safePlaces = new ArrayList<>();
    private final List<Volunteer> volunteers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        rescueMapWebView = findViewById(R.id.rescueMapWebView);

        WebSettings webSettings = rescueMapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        rescueMapWebView.addJavascriptInterface(new Object() {

            @android.webkit.JavascriptInterface
            public void callVolunteer(String phone) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            }

        }, "Android");

        rescueMapWebView.setWebViewClient(new WebViewClient());

        rescueMapWebView.loadUrl("file:///android_asset/rescue_map.html");



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        locationText = findViewById(R.id.locationText);
        safePlacesText = findViewById(R.id.safePlacesText);
        volunteersText = findViewById(R.id.volunteersText);
        volunteersText.setOnClickListener(v -> {

            if (nearestVolunteer == null) {
                Toast.makeText(this,
                        "No available volunteers nearby.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(nearestVolunteer.getName())
                    .setMessage(
                            "Distance: " +
                                    formatDistance(nearestVolunteer.getDistanceKm()) +
                                    "\nStatus: Available"
                    )
                    .setPositiveButton("Call", (dialog, which) -> {

                        Intent intent = new Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:" + nearestVolunteer.getPhoneNumber())
                        );

                        startActivity(intent);
                    })
                    .setNegativeButton("Location", (dialog, which) -> {

                        openNavigation(
                                nearestVolunteer.getLatitude(),
                                nearestVolunteer.getLongitude()
                        );
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        });
        refreshButton = findViewById(R.id.refreshButton);

        locationHelper = new LocationHelper(this);
        loadDummyData();

        incidentStatusText = findViewById(R.id.incidentStatusText);
        sosButton = findViewById(R.id.sosButton);
        sosButton.setOnClickListener(v -> onSosPressed());

        startIncidentPolling();

        refreshButton.setOnClickListener(v -> getUserLocation());
        safePlacesText.setOnClickListener(v -> {
            if (!safePlaces.isEmpty()) {
                SafePlace nearest = safePlaces.get(0);

                openNavigation(
                        nearest.getLatitude(),
                        nearest.getLongitude()
                );
            }
        });

        if (locationHelper.hasLocationPermission()) {
            getUserLocation();
        } else {
            locationHelper.requestLocationPermission();
        }
    }

    /* ───────── Shared-backend SOS + live incident feed ───────── */

    private void onSosPressed() {
        sosButton.setEnabled(false);
        incidentStatusText.setText("Sending SOS…");
        if (!locationHelper.hasLocationPermission()) {
            locationHelper.requestLocationPermission();
            sosButton.setEnabled(true);
            incidentStatusText.setText("Grant location, then press SOS again.");
            return;
        }
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(@NonNull Location location) {
                sendSos(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onLocationError(@NonNull String message) {
                // Send without coordinates rather than blocking a distress call.
                sendSos(0, 0);
            }
        });
    }

    private void sendSos(double latitude, double longitude) {
        networkExecutor.execute(() -> {
            String deviceId = "PHONE-" + android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            EmergencyApi.Incident stored =
                    EmergencyApi.sendSos(deviceId, latitude, longitude);
            runOnUiThread(() -> {
                sosButton.setEnabled(true);
                if (stored != null) {
                    incidentStatusText.setText("SOS sent — visible on the responder dashboard now.");
                    Toast.makeText(this, "SOS reported to responders", Toast.LENGTH_LONG).show();
                    fetchIncidentsOnce();
                } else {
                    incidentStatusText.setText("Could not reach the backend — check connection and retry.");
                    Toast.makeText(this, "Backend unreachable — SOS not sent", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void startIncidentPolling() {
        if (incidentsPollRunning) return;
        incidentsPollRunning = true;
        fetchIncidentsOnce();
    }

    private void fetchIncidentsOnce() {
        if (isFinishing() || isDestroyed()) return;
        networkExecutor.execute(() -> {
            final List<EmergencyApi.Incident> incidents = EmergencyApi.getIncidents();
            final String json = incidentsToJson(incidents);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                updateIncidentStatus(incidents);
                if (json != null && rescueMapWebView != null) {
                    rescueMapWebView.evaluateJavascript(
                            "window.loadIncidents(" + json + ")", null);
                }
                mainHandler.postDelayed(this::fetchIncidentsOnce, POLL_INTERVAL_MS);
            });
        });
    }

    private void updateIncidentStatus(List<EmergencyApi.Incident> incidents) {
        int active = 0;
        for (EmergencyApi.Incident i : incidents) {
            if (!"RESOLVED".equals(i.status)) active++;
        }
        if (active > 0) {
            incidentStatusText.setText(active + " live SOS" + (active == 1 ? "" : "es")
                    + " on the shared network — shown on the map");
        } else if (!incidents.isEmpty()) {
            incidentStatusText.setText("Connected — no active SOS incidents.");
        } else {
            incidentStatusText.setText("Backend unreachable or no incidents yet.");
        }
    }

    private String incidentsToJson(List<EmergencyApi.Incident> incidents) {
        try {
            JSONArray arr = new JSONArray();
            for (EmergencyApi.Incident i : incidents) {
                JSONObject o = new JSONObject();
                o.put("_id", i.id);
                o.put("deviceId", i.deviceId);
                o.put("latitude", i.latitude);
                o.put("longitude", i.longitude);
                o.put("status", i.status);
                arr.put(o);
            }
            return arr.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        incidentsPollRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
        networkExecutor.shutdownNow();
    }

    private void loadDummyData() {
        // TEST DATA ONLY. Replace with backend data later.
        safePlaces.clear();
        safePlaces.add(new SafePlace("Shelter A", 12.9716, 77.5946));
        safePlaces.add(new SafePlace("Shelter B", 12.9352, 77.6245));
        safePlaces.add(new SafePlace("Shelter C", 12.9980, 77.5800));

        volunteers.add(new Volunteer(
                "Volunteer A",
                12.9500,
                77.5800,
                true,
                "9876543210"
        ));

        volunteers.add(new Volunteer(
                "Volunteer B",
                12.9400,
                77.6000,
                true,
                "9123456780"
        ));

        volunteers.add(new Volunteer(
                "Volunteer C",
                12.9800,
                77.5600,
                false,
                "9988776655"
        ));
    }

    private void getUserLocation() {
        locationText.setText("Getting your GPS location...");
        safePlacesText.setText("Calculating...");
        volunteersText.setText("Calculating...");
        refreshButton.setEnabled(false);

        if (!locationHelper.hasLocationPermission()) {
            locationHelper.requestLocationPermission();
            refreshButton.setEnabled(true);
            return;
        }

        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(@NonNull Location location) {
                refreshButton.setEnabled(true);

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                locationText.setText(String.format(
                        Locale.US,
                        "Latitude: %.6f\nLongitude: %.6f",
                        latitude,
                        longitude
                ));

                calculateSafePlaceDistances(latitude, longitude);
                calculateVolunteerDistances(latitude, longitude);
            }

            @Override
            public void onLocationError(@NonNull String message) {
                refreshButton.setEnabled(true);
                locationText.setText(message);
                safePlacesText.setText("Unable to calculate safe places.");
                volunteersText.setText("Unable to calculate volunteers.");
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void calculateSafePlaceDistances(double userLatitude, double userLongitude) {
        for (SafePlace place : safePlaces) {
            float[] result = new float[1];
            Location.distanceBetween(
                    userLatitude,
                    userLongitude,
                    place.getLatitude(),
                    place.getLongitude(),
                    result
            );
            place.setDistanceKm(result[0] / 1000f);
        }

        Collections.sort(safePlaces, Comparator.comparingDouble(SafePlace::getDistanceKm));

        StringBuilder output = new StringBuilder();
        if (safePlaces.isEmpty()) {
            output.append("No safe places available.");
        } else {
            for (int i = 0; i < safePlaces.size(); i++) {
                SafePlace place = safePlaces.get(i);
                output.append(i + 1)
                        .append(". ")
                        .append(place.getName())
                        .append(" - ")
                        .append(formatDistance(place.getDistanceKm()))
                        .append("\n");
            }
        }
        safePlacesText.setText(output.toString().trim());
    }

    private void calculateVolunteerDistances(double userLatitude, double userLongitude) {
        // Only available volunteers are considered.
        List<Volunteer> availableVolunteers = new ArrayList<>();
        for (Volunteer volunteer : volunteers) {
            if (volunteer.isAvailable()) {
                float[] result = new float[1];
                Location.distanceBetween(
                        userLatitude,
                        userLongitude,
                        volunteer.getLatitude(),
                        volunteer.getLongitude(),
                        result
                );
                volunteer.setDistanceKm(result[0] / 1000f);
                availableVolunteers.add(volunteer);
            }
        }

        Collections.sort(availableVolunteers,
                Comparator.comparingDouble(Volunteer::getDistanceKm));
        if (!availableVolunteers.isEmpty()) {
            nearestVolunteer = availableVolunteers.get(0);
        } else {
            nearestVolunteer = null;
        }

        StringBuilder output = new StringBuilder();
        if (availableVolunteers.isEmpty()) {
            output.append("No available volunteers nearby.");
        } else {
            for (int i = 0; i < availableVolunteers.size(); i++) {
                Volunteer volunteer = availableVolunteers.get(i);
                output.append(i + 1)
                        .append(". ")
                        .append(volunteer.getName())
                        .append(" - ")
                        .append(formatDistance(volunteer.getDistanceKm()))
                        .append(" - Available\n");
            }
        }output.append("\n\nUNAVAILABLE VOLUNTEERS\n");

            for (Volunteer volunteer : volunteers) {
                if (!volunteer.isAvailable()) {
                    output.append("• ")
                            .append(volunteer.getName())
                            .append(" - Unavailable\n");
                }
            }
        volunteersText.setText(output.toString().trim());
    }

    private String formatDistance(float distanceKm) {
        if (distanceKm < 1f) {
            return String.format(Locale.US, "%.0f m", distanceKm * 1000f);
        }
        return String.format(Locale.US, "%.2f km", distanceKm);
    }

    // Optional: opens Google Maps for a selected destination.
    // Example usage later: openNavigation(place.getLatitude(), place.getLongitude());
    private void openNavigation(double latitude, double longitude) {

        String url = "https://www.google.com/maps/dir/?api=1&destination="
                + latitude + "," + longitude;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                getUserLocation();
            } else {
                locationText.setText("Location permission was denied. Enable it in Settings to use Safe Places and Volunteers.");
                refreshButton.setEnabled(true);
            }
        }
    }

}

