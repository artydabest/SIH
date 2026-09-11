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

public class MainActivity extends AppCompatActivity {

    private LocationHelper locationHelper;
    private TextView locationText;
    private TextView safePlacesText;
    private TextView volunteersText;
    private Button refreshButton;

    private final List<SafePlace> safePlaces = new ArrayList<>();
    private final List<Volunteer> volunteers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        locationText = findViewById(R.id.locationText);
        safePlacesText = findViewById(R.id.safePlacesText);
        volunteersText = findViewById(R.id.volunteersText);
        refreshButton = findViewById(R.id.refreshButton);

        locationHelper = new LocationHelper(this);
        loadDummyData();

        refreshButton.setOnClickListener(v -> getUserLocation());

        if (locationHelper.hasLocationPermission()) {
            getUserLocation();
        } else {
            locationHelper.requestLocationPermission();
        }
    }

    private void loadDummyData() {
        // TEST DATA ONLY. Replace with backend data later.
        safePlaces.clear();
        safePlaces.add(new SafePlace("Shelter A", 12.9716, 77.5946));
        safePlaces.add(new SafePlace("Shelter B", 12.9352, 77.6245));
        safePlaces.add(new SafePlace("Shelter C", 12.9980, 77.5800));

        volunteers.clear();
        volunteers.add(new Volunteer("Volunteer A", 12.9725, 77.5920, true));
        volunteers.add(new Volunteer("Volunteer B", 12.9400, 77.6100, true));
        volunteers.add(new Volunteer("Volunteer C", 12.9800, 77.6500, false));
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
        Uri uri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback if Google Maps is not installed.
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + latitude + "," + longitude);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
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
