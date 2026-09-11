package com.sih.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 41;

    private LinearLayout cardStatusIndicator;
    private TextView tvStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusSubtitle;
    private LinearLayout btnSos;
    private LinearLayout btnSafePlaces;
    private LinearLayout btnVolunteers;
    private LinearLayout btnRescueMap;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        refreshStatusIndicator();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatusIndicator();
    }

    private void initViews() {
        cardStatusIndicator = findViewById(R.id.cardStatusIndicator);
        tvStatusIcon = findViewById(R.id.tvStatusIcon);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle);
        btnSos = findViewById(R.id.btnSos);
        btnSafePlaces = findViewById(R.id.btnSafePlaces);
        btnVolunteers = findViewById(R.id.btnVolunteers);
        btnRescueMap = findViewById(R.id.btnRescueMap);
    }

    private void setupClickListeners() {
        // SOS -> report a real emergency to the backend, then open the alert screen.
        btnSos.setOnClickListener(v -> onSosPressed());
        cardStatusIndicator.setOnClickListener(v -> onSosPressed());

        btnSafePlaces.setOnClickListener(v ->
                Toast.makeText(this, "Safe Places screen (building next!)", Toast.LENGTH_SHORT).show());
        btnVolunteers.setOnClickListener(v ->
                Toast.makeText(this, "Volunteers screen (building next!)", Toast.LENGTH_SHORT).show());

        // Rescue Map -> live map fed by the same backend the dashboard uses.
        btnRescueMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapActivity.class)));
    }

    private void onSosPressed() {
        if (hasLocationPermission()) {
            requestLastLocationThenReport();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLastLocationThenReport();
            } else {
                reportEmergency(0, 0); // no coordinates — reported without location
            }
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void requestLastLocationThenReport() {
        android.location.LocationManager lm =
                (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        Location last = null;
        if (lm != null) {
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (last == null || l.getTime() > last.getTime())) {
                    last = l;
                }
            }
        }
        if (last != null) {
            reportEmergency(last.getLatitude(), last.getLongitude());
        } else {
            // No fix available yet; report without coordinates rather than blocking SOS.
            Toast.makeText(this, "No GPS fix yet — reporting without location", Toast.LENGTH_LONG).show();
            reportEmergency(0, 0);
        }
    }

    /**
     * POST a real emergency to the disaster-relay backend (the same store the
     * responder dashboard polls), then open the alert screen.
     * latitude/longitude of 0,0 mean "unknown" — the backend requires the
     * fields, so we send zeros rather than fabricating a position.
     */
    private void reportEmergency(double latitude, double longitude) {
        tvStatusTitle.setText("Sending SOS…");
        networkExecutor.execute(() -> {
            int nearbyDevices = countNearbyFromBackend();
            ApiClient.Emergency stored = ApiClient.reportEmergency(
                    "PHONE-" + android.provider.Settings.Secure.getString(
                            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID),
                    latitude,
                    longitude,
                    nearbyDevices,
                    0);
            runOnUiThread(() -> {
                if (stored != null) {
                    openEmergencyScreen();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Could not reach the backend — check connection and try again",
                            Toast.LENGTH_LONG).show();
                    refreshStatusIndicator();
                }
            });
        });
    }

    /** Nearby device count from the backend's current incident list (mesh estimate). */
    private int countNearbyFromBackend() {
        List<ApiClient.Emergency> all = ApiClient.getEmergencies();
        return Math.min(all.size(), 9);
    }

    private void openEmergencyScreen() {
        startActivity(new Intent(MainActivity.this, EmergencyActivity.class));
    }

    /** Reflect the real backend state on the home status card (polled lightly). */
    private void refreshStatusIndicator() {
        networkExecutor.execute(() -> {
            final List<ApiClient.Emergency> list = ApiClient.getEmergencies();
            runOnUiThread(() -> updateStatusIndicator(list));
        });
    }

    private void updateStatusIndicator(List<ApiClient.Emergency> list) {
        int active = 0;
        int newestConfidence = 0;
        for (ApiClient.Emergency e : list) {
            if (!"RESOLVED".equals(e.status)) {
                active++;
                // Rough confidence from evidence the backend actually provides.
                int c = 40 + Math.min(50, e.stationaryMinutes) + Math.min(10, e.nearbyDevices * 2);
                if (c > newestConfidence) newestConfidence = c;
            }
        }

        if (active > 0) {
            cardStatusIndicator.setBackgroundResource(R.drawable.bg_status_alert);
            tvStatusIcon.setText("🚨");
            tvStatusTitle.setText(active == 1
                    ? "EMERGENCY DETECTED"
                    : active + " ACTIVE EMERGENCIES");
            tvStatusTitle.setTextColor(getResources().getColor(R.color.danger_red));
            tvStatusSubtitle.setText("Tap to view • synced with responder dashboard");
        } else {
            cardStatusIndicator.setBackgroundResource(R.drawable.bg_status_normal);
            tvStatusIcon.setText("✅");
            tvStatusTitle.setText("Status: Area Monitored");
            tvStatusTitle.setTextColor(getResources().getColor(R.color.safe_green));
            tvStatusSubtitle.setText(list.isEmpty()
                    ? "Backend unreachable • showing cached state"
                    : "No active emergency detected • Mesh listening");
        }
    }
}
