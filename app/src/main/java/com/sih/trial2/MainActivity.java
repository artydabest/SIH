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

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;


public class MainActivity extends AppCompatActivity
       {
    private LocationHelper locationHelper;
    private TextView locationText;
    private TextView safePlacesText;
    private TextView volunteersText;
    private Button refreshButton;
    private Volunteer nearestVolunteer;


    private final List<SafePlace> safePlaces = new ArrayList<>();
    private final List<Volunteer> volunteers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        WebView rescueMapWebView = findViewById(R.id.rescueMapWebView);

        WebSettings webSettings = rescueMapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        rescueMapWebView.addJavascriptInterface(new Object() {

            @android.webkit.JavascriptInterface
            public void callVolunteer(String phone) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                });
            }

            @android.webkit.JavascriptInterface
            public void navigateTo(String latitude, String longitude) {
                runOnUiThread(() -> {
                    String url = "https://www.google.com/maps/dir/?api=1&destination="
                            + latitude + "," + longitude;

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                });
            }

            @android.webkit.JavascriptInterface
            public void centerMap() {
                runOnUiThread(() -> {
                    // Tell the HTML map to center itself on the user's location
                    rescueMapWebView.evaluateJavascript(
                            "centerMapFromAndroid();",
                            null
                    );
                });
            }

        }, "Android");

        rescueMapWebView.setWebViewClient(new WebViewClient());

        rescueMapWebView.loadUrl("file:///android_asset/rescue_map.html");

        rescueMapWebView.loadUrl("file:///android_asset/rescue_map.html");

        rescueMapWebView.setWebViewClient(new WebViewClient());

        rescueMapWebView.loadUrl("file:///android_asset/rescue_map.html");
        try {
            java.io.InputStream inputStream =
                    getAssets().open("rescue_map.html");

            java.io.ByteArrayOutputStream outputStream =
                    new java.io.ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();

            String html = outputStream.toString("UTF-8");

            rescueMapWebView.loadDataWithBaseURL(
                    "https://example.com/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
            );

        } catch (Exception e) {
            e.printStackTrace();
        }



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

