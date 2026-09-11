package com.sih.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyActivity extends AppCompatActivity {

    private static final long POLL_INTERVAL_MS = 7000;

    private TextView tvConfidencePercent;
    private TextView tvConfidenceLevel;
    private TextView tvNearbyDevices;
    private TextView tvLocationStatus;
    private TextView tvLocationCoordinates;
    private TextView tvResponderDispatchStatus;
    private android.widget.Button btnBack;
    private android.widget.Button btnViewSafePlaces;
    private android.widget.Button btnFindVolunteers;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private boolean pollRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        initViews();
        setupClickListeners();
        startPolling();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvConfidencePercent = findViewById(R.id.tvConfidencePercent);
        tvConfidenceLevel = findViewById( R.id.tvConfidenceLevel);
        tvNearbyDevices = findViewById(R.id.tvNearbyDevices);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvLocationCoordinates = findViewById(R.id.tvLocationCoordinates);
        tvResponderDispatchStatus = findViewById(R.id.tvResponderDispatchStatus);
        btnViewSafePlaces = findViewById(R.id.btnViewSafePlaces);
        btnFindVolunteers = findViewById(R.id.btnFindVolunteers);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnViewSafePlaces.setOnClickListener(v ->
                Toast.makeText(this, "Safe Places screen (building next!)", Toast.LENGTH_SHORT).show());
        btnFindVolunteers.setOnClickListener(v ->
                Toast.makeText(this, "Volunteers screen (building next!)", Toast.LENGTH_SHORT).show());
    }

    private void startPolling() {
        if (pollRunning) return;
        pollRunning = true;
        fetchOnce();
    }

    private void fetchOnce() {
        if (isFinishing() || isDestroyed()) return;
        networkExecutor.execute(() -> {
            final List<ApiClient.Emergency> list = ApiClient.getEmergencies();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                bindLatest(list);
                if (!pollRunning) return;
                mainHandler.postDelayed(this::fetchOnce, POLL_INTERVAL_MS);
            });
        });
    }

    /** Bind the newest incident (backend returns newest-first) to the UI. */
    private void bindLatest(List<ApiClient.Emergency> list) {
        if (list == null || list.isEmpty()) {
            tvConfidencePercent.setText("--");
            tvConfidenceLevel.setText("AWAITING DATA");
            tvConfidenceLevel.setBackgroundColor(Color.parseColor("#667085"));
            tvNearbyDevices.setText("No incidents in the shared network");
            tvLocationStatus.setText("Location unavailable");
            tvLocationCoordinates.setText("Backend unreachable or no incidents yet");
            tvResponderDispatchStatus.setText("—");
            return;
        }
        ApiClient.Emergency e = list.get(0);
        boolean resolved = "RESOLVED".equals(e.status);

        // Confidence is derived from the same evidence the dashboard shows,
        // since the backend does not store a confidence field.
        int confidence = 40 + Math.min(50, e.stationaryMinutes) + Math.min(10, e.nearbyDevices * 2);
        tvConfidencePercent.setText(resolved ? "✓" : confidence + "%");

        String statusLabel;
        int levelColor;
        switch (e.status) {
            case "ACKNOWLEDGED":
                statusLabel = "ACKNOWLEDGED"; levelColor = Color.parseColor("#F59E0B"); break;
            case "RESPONDING":
                statusLabel = "RESPONDING"; levelColor = Color.parseColor("#2563EB"); break;
            case "RESOLVED":
                statusLabel = "RESOLVED"; levelColor = Color.parseColor("#16A34A"); break;
            default:
                statusLabel = "HIGH CONFIDENCE"; levelColor = Color.parseColor("#DC2626"); break;
        }
        tvConfidenceLevel.setText(statusLabel);
        tvConfidenceLevel.setBackgroundColor(levelColor);

        tvNearbyDevices.setText(e.nearbyDevices + " nearby devices reporting");
        tvLocationStatus.setText(resolved ? "Incident resolved" : "Location detected");
        tvLocationCoordinates.setText(formatCoords(e.latitude, e.longitude)
                + " • detected " + formatAge(e.createdAtMs));

        if (resolved) {
            tvResponderDispatchStatus.setText("Incident closed");
            tvResponderDispatchStatus.setTextColor(Color.parseColor("#16A34A"));
        } else if ("RESPONDING".equals(e.status)) {
            tvResponderDispatchStatus.setText("Responders on scene");
            tvResponderDispatchStatus.setTextColor(Color.parseColor("#2563EB"));
        } else {
            tvResponderDispatchStatus.setText("Responders Notified");
            tvResponderDispatchStatus.setTextColor(Color.parseColor("#16A34A"));
        }
    }

    private String formatCoords(double lat, double lon) {
        if (lat == 0 && lon == 0) return "Location not shared";
        return String.format(java.util.Locale.US, "%.4f, %.4f", lat, lon);
    }

    private String formatAge(long thenMs) {
        long seconds = Math.max(0, (System.currentTimeMillis() - thenMs) / 1000);
        if (seconds < 60) return "just now";
        if (seconds < 3600) return (seconds / 60) + " min ago";
        if (seconds < 86400) return (seconds / 3600) + " h ago";
        return (seconds / 86400) + " d ago";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
        networkExecutor.shutdownNow();
    }
}
