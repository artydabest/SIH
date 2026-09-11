package com.sih.trial2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.util.TypedValue;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.PolygonOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap.OnMarkerClickListener;
import org.maplibre.android.maps.Style;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String DEVICE_ID = "CITIZEN-" + android.os.Build.MODEL;

    /** Active disaster alert received from the backend (null when all-clear). */
    private DisasterAlert activeAlert = null;
    /** IDs of alerts the user has already been shown — prevents re-alert spam. */
    private final java.util.Set<String> shownAlertIds = new java.util.HashSet<>();
    private final java.util.Timer alertTimer = new java.util.Timer();

    /** Free OpenStreetMap-based styles (no Google Maps, no API key). */
    private static final String STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";
    private static final String STYLE_URL_FALLBACK = "https://demotiles.maplibre.org/style.json";

    private LocationHelper locationHelper;
    private ApiClient apiClient;
    private MovementTracker movementTracker;
    private AppSettings settings;

    private TextView locationText;
    private TextView safePlacesText;
    private TextView volunteersText;
    private TextView friendsText;
    private TextView sosStatusText;
    private TextView checkinStatusText;
    private Button refreshButton;
    private Button sosButton;
    private Button settingsButton;
    private Button safeToggleButton;
    private LinearLayout safePlacesContainer;
    private LinearLayout volunteersContainer;
    private LinearLayout friendsContainer;

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private boolean mapReady = false;

    // Persistent alert banner (stays visible while an alert is active)
    private LinearLayout alertBanner;
    private TextView alertBannerTitle;
    private TextView alertBannerMessage;
    private LinearLayout alertBannerInstructions;

    private double lastLatitude;
    private double lastLongitude;
    private Double lastAltitude;
    private boolean hasLocation = false;
    private boolean seededNearby = false;

    private final List<SafePlace> safePlaces = new ArrayList<>();
    private final List<Volunteer> volunteers = new ArrayList<>();
    private final List<Friend> friends = new ArrayList<>();
    private final List<Marker> mapMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);
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
        friendsText = findViewById(R.id.friendsText);
        sosStatusText = findViewById(R.id.sosStatusText);
        checkinStatusText = findViewById(R.id.checkinStatusText);
        refreshButton = findViewById(R.id.refreshButton);
        sosButton = findViewById(R.id.sosButton);
        settingsButton = findViewById(R.id.settingsButton);
        safeToggleButton = findViewById(R.id.safeToggleButton);
        safePlacesContainer = findViewById(R.id.safePlacesContainer);
        volunteersContainer = findViewById(R.id.volunteersContainer);
        friendsContainer = findViewById(R.id.friendsContainer);
        mapView = findViewById(R.id.mapView);
        alertBanner = findViewById(R.id.alertBanner);
        alertBannerTitle = findViewById(R.id.alertBannerTitle);
        alertBannerMessage = findViewById(R.id.alertBannerMessage);
        alertBannerInstructions = findViewById(R.id.alertBannerInstructions);

        settings = new AppSettings(this);
        apiClient = new ApiClient(settings.getBaseUrl());
        locationHelper = new LocationHelper(this);
        movementTracker = new MovementTracker();

        refreshButton.setOnClickListener(v -> getUserLocation());
        sosButton.setOnClickListener(v -> reportEmergency());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        safeToggleButton.setOnClickListener(v -> toggleSafeStatus());
        updateSafeToggleUi();

        setupMap(savedInstanceState);

        if (locationHelper.hasLocationPermission()) {
            getUserLocation();
        } else {
            locationHelper.requestLocationPermission();
        }

        AlertNotifier.requestPermissionIfNeeded(this);
        startAlertPolling();
    }

    /** Polls for active disaster alerts every 8s; popup + notification on new ones. */
    private void startAlertPolling() {
        alertTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {                runOnUiThread(() -> apiClient.getActiveAlert(new ApiClient.AlertCallback() {
                    @Override
                    public void onSuccess(DisasterAlert alert) {
                        activeAlert = alert;

                        // Popup + notification exactly once per alert id.
                        if (alert != null && !shownAlertIds.contains(alert.id)) {
                            shownAlertIds.add(alert.id);
                            AlertNotifier.showAlertDialog(MainActivity.this, alert);
                            AlertNotifier.showNotification(MainActivity.this, alert);
                        }

                        updateAlertBanner();      // persistent banner, always in sync
                        applyMapData();           // redraw incl. danger-zone circle
                    }

                    @Override
                    public void onError(String message) {
                        // Silent — alerts retry on next tick.
                    }
                }));
            }
        }, 2000L, 8000L);
    }

    /** Map: free OSM tiles, full gestures, custom icons, click dialogs. */
    private void setupMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            map.getUiSettings().setAllGesturesEnabled(true);

            // Marker taps open an action dialog instead of the plain popup.
            map.setOnMarkerClickListener(marker -> {
                showMarkerDialog(marker);
                return true; // consume — custom dialog replaces default popup
            });

            map.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
                mapReady = true;
                applyMapData();
            });
        });
    }

    /** Pop-action dialog for volunteer (call/navigate) and safe place (navigate). */
    private void showMarkerDialog(Marker marker) {
        final Volunteer volunteer = findVolunteerByMarker(marker);
        final SafePlace place = volunteer == null ? findSafePlaceByMarker(marker) : null;

        if (volunteer != null) {
            new AlertDialog.Builder(this)
                    .setTitle("📞 Volunteer: " + volunteer.getName())
                    .setMessage(volunteer.getPhone() != null
                            ? "Phone: " + volunteer.getPhone()
                            : "Available nearby")
                    .setPositiveButton("Call", (d, w) -> {
                        if (volunteer.getPhone() != null) {
                            startActivity(new Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:" + volunteer.getPhone())));
                        }
                    })
                    .setNegativeButton("Navigate", (d, w) ->
                            openNavigation(volunteer.getLatitude(), volunteer.getLongitude()))
                    .setNeutralButton("Close", null)
                    .show();
        } else if (place != null) {
            new AlertDialog.Builder(this)
                    .setTitle("🟩 " + place.getName())
                    .setMessage("Safe spot near you" + (place.getDistanceKm() > 0
                            ? " — " + formatDistance(place.getDistanceKm())
                            : ""))
                    .setPositiveButton("Navigate", (d, w) ->
                            openNavigation(place.getLatitude(), place.getLongitude()))
                    .setNegativeButton("Close", null)
                    .show();
        }
        // Friend and self markers keep the default info window behavior.
    }

    private Volunteer findVolunteerByMarker(Marker marker) {
        for (Volunteer v : volunteers) {
            if (v.isAvailable()
                    && marker.getPosition().getLatitude() == v.getLatitude()
                    && marker.getPosition().getLongitude() == v.getLongitude()) {
                return v;
            }
        }
        return null;
    }

    private SafePlace findSafePlaceByMarker(Marker marker) {
        for (SafePlace p : safePlaces) {
            if (marker.getPosition().getLatitude() == p.getLatitude()
                    && marker.getPosition().getLongitude() == p.getLongitude()) {
                return p;
            }
        }
        return null;
    }

    /** In-app settings: display name + backend URL (per-device, no rebuild needed). */
    private void showSettingsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), 0);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Your display name (seen by friends)");
        nameInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(settings.getName());
        box.addView(nameInput);

        final EditText urlInput = new EditText(this);
        urlInput.setHint("Backend URL e.g. http://192.168.1.20:3000");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setText(settings.getBaseUrl());
        box.addView(urlInput);

        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(box)
                .setPositiveButton("Save", (dialog, which) -> {
                    settings.setName(nameInput.getText().toString());
                    String newUrl = urlInput.getText().toString().trim();
                    boolean urlChanged = !newUrl.equals(settings.getBaseUrl());
                    settings.setBaseUrl(newUrl);
                    if (urlChanged) {
                        apiClient = new ApiClient(settings.getBaseUrl());
                        Toast.makeText(this, "Backend updated — tap Refresh.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Flips safety status and checks in immediately. */
    private void toggleSafeStatus() {
        boolean nowSafe = !settings.isSafe();
        settings.setSafe(nowSafe);
        updateSafeToggleUi();
        if (hasLocation) {
            checkIn(nowSafe);
        }
    }

    private void updateSafeToggleUi() {
        if (settings.isSafe()) {
            safeToggleButton.setText("I am SAFE");
            safeToggleButton.setBackgroundColor(0xFF2E7D32);
        } else {
            safeToggleButton.setText("I NEED HELP");
            safeToggleButton.setBackgroundColor(0xFFC62828);
        }
    }

    /** Sends this device's position + safety status to the backend. */
    private void checkIn(boolean safe) {
        checkinStatusText.setText("Checking in...");
        apiClient.checkIn(DEVICE_ID, settings.getName(), lastLatitude, lastLongitude, safe,
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        checkinStatusText.setText("Checked in — friends see your live location.");
                    }

                    @Override
                    public void onError(String message) {
                        checkinStatusText.setText("Check-in failed — retrying on next refresh.");
                    }
                });
    }

    /** SOS: reports an emergency with position + detection data. */
    private void reportEmergency() {
        if (!hasLocation) {
            Toast.makeText(this, "Waiting for GPS — try again shortly.", Toast.LENGTH_LONG).show();
            getUserLocation();
            return;
        }

        sosButton.setEnabled(false);
        sosStatusText.setText("Reporting emergency to backend...");

        long stationaryMinutes = Math.max(1, movementTracker.getStationaryMinutes());

        apiClient.postEmergency(DEVICE_ID, lastLatitude, lastLongitude, lastAltitude,
                stationaryMinutes, 0, true,
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        sosButton.setEnabled(true);
                        sosStatusText.setText("Emergency reported. Responders notified.");
                        Toast.makeText(MainActivity.this,
                                "Emergency reported — responders notified.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String message) {
                        sosButton.setEnabled(true);
                        sosStatusText.setText("Report failed — check connection and retry.");
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void getUserLocation() {
        locationText.setText("Getting your GPS location...");
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

                lastLatitude = location.getLatitude();
                lastLongitude = location.getLongitude();
                lastAltitude = location.hasAltitude() ? location.getAltitude() : null;
                hasLocation = true;

                movementTracker.onLocationChanged(location);

                locationText.setText(String.format(Locale.US,
                        "Lat %.5f, Lon %.5f%s",
                        lastLatitude, lastLongitude,
                        lastAltitude != null
                                ? String.format(Locale.US, " · alt %.0f m", lastAltitude)
                                : ""));

                loadBackendData(lastLatitude, lastLongitude);
                checkIn(settings.isSafe());
            }

            @Override
            public void onLocationError(@NonNull String message) {
                refreshButton.setEnabled(true);
                locationText.setText(message);
                safePlacesText.setText("Location needed for safe spots.");
                volunteersText.setText("Location needed for volunteers.");
                friendsText.setText("Location needed for friend check-ins.");
                safePlacesContainer.removeAllViews();
                volunteersContainer.removeAllViews();
                friendsContainer.removeAllViews();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Pulls safe spots, volunteers, and friends from the backend.
     * First GPS fix of a session also seeds safe spots around the user's area.
     */
    private void loadBackendData(double userLatitude, double userLongitude) {
        safePlacesText.setText("Loading...");
        volunteersText.setText("Loading...");
        friendsText.setText("Loading...");
        safePlacesContainer.removeAllViews();
        volunteersContainer.removeAllViews();
        friendsContainer.removeAllViews();

        if (!seededNearby) {
            seededNearby = true;
            apiClient.seedNearbySafeZones(userLatitude, userLongitude, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    fetchSafePlaces(userLatitude, userLongitude);
                }

                @Override
                public void onError(String message) {
                    fetchSafePlaces(userLatitude, userLongitude);
                }
            });
        } else {
            fetchSafePlaces(userLatitude, userLongitude);
        }

        apiClient.getVolunteers(new ApiClient.VolunteersCallback() {
            @Override
            public void onSuccess(List<Volunteer> people) {
                volunteers.clear();
                volunteers.addAll(people);
                renderVolunteers(userLatitude, userLongitude);
            }

            @Override
            public void onError(String message) {
                volunteersText.setText(message);
            }
        });

        apiClient.getPeople(new ApiClient.FriendsCallback() {
            @Override
            public void onSuccess(List<Friend> people) {
                friends.clear();
                for (Friend person : people) {
                    // Everyone except this device is a "friend" on the map/list.
                    if (!person.getDeviceId().equals(DEVICE_ID)) {
                        friends.add(person);
                    }
                }
                renderFriends(userLatitude, userLongitude);
            }

            @Override
            public void onError(String message) {
                friendsText.setText(message);
            }
        });
    }

    private void fetchSafePlaces(double userLatitude, double userLongitude) {
        apiClient.getSafeZones(new ApiClient.SafeZonesCallback() {
            @Override
            public void onSuccess(List<SafePlace> places) {
                safePlaces.clear();
                safePlaces.addAll(places);
                renderSafePlaces(userLatitude, userLongitude);
            }

            @Override
            public void onError(String message) {
                safePlacesText.setText(message);
            }
        });
    }

    private void renderSafePlaces(double userLatitude, double userLongitude) {
        for (SafePlace place : safePlaces) {
            float[] result = new float[1];
            Location.distanceBetween(userLatitude, userLongitude,
                    place.getLatitude(), place.getLongitude(), result);
            place.setDistanceKm(result[0] / 1000f);
        }
        Collections.sort(safePlaces, Comparator.comparingDouble(SafePlace::getDistanceKm));

        safePlacesText.setVisibility(View.GONE);
        safePlacesContainer.removeAllViews();

        if (safePlaces.isEmpty()) {
            safePlacesText.setVisibility(View.VISIBLE);
            safePlacesText.setText("No safe spots registered yet.");
            return;
        }

        for (int i = 0; i < safePlaces.size(); i++) {
            final SafePlace place = safePlaces.get(i);
            safePlacesContainer.addView(makeRowView(
                    place.getName(),
                    formatDistance(place.getDistanceKm()),
                    i + 1,
                    () -> openNavigation(place.getLatitude(), place.getLongitude())
            ));
        }
        applyMapData();
    }

    private void renderVolunteers(double userLatitude, double userLongitude) {
        List<Volunteer> available = new ArrayList<>();
        for (Volunteer volunteer : volunteers) {
            if (volunteer.isAvailable()) {
                float[] result = new float[1];
                Location.distanceBetween(userLatitude, userLongitude,
                        volunteer.getLatitude(), volunteer.getLongitude(), result);
                volunteer.setDistanceKm(result[0] / 1000f);
                available.add(volunteer);
            }
        }
        Collections.sort(available, Comparator.comparingDouble(Volunteer::getDistanceKm));

        volunteersText.setVisibility(View.GONE);
        volunteersContainer.removeAllViews();

        if (available.isEmpty()) {
            volunteersText.setVisibility(View.VISIBLE);
            volunteersText.setText("No available volunteers nearby.");
            return;
        }

        for (int i = 0; i < available.size(); i++) {
            final Volunteer volunteer = available.get(i);
            volunteersContainer.addView(makeRowView(
                    volunteer.getName(),
                    formatDistance(volunteer.getDistanceKm()),
                    i + 1,
                    () -> {
                        String phone = volunteer.getPhone();
                        if (phone != null) {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                        }
                    }
            ));
        }
        applyMapData();
    }

    /** Friends list with live status; tap a friend to fly the map to them. */
    private void renderFriends(double userLatitude, double userLongitude) {
        friendsText.setVisibility(View.GONE);
        friendsContainer.removeAllViews();

        if (friends.isEmpty()) {
            friendsText.setVisibility(View.VISIBLE);
            friendsText.setText("No friends checked in yet. Share the app — their status shows here and on the map.");
            return;
        }

        for (final Friend friend : friends) {
            float[] result = new float[1];
            Location.distanceBetween(userLatitude, userLongitude,
                    friend.getLatitude(), friend.getLongitude(), result);
            friend.setDistanceKm(result[0] / 1000f);

            String status = friend.isSafe() ? "SAFE" : "NEEDS HELP";
            friendsContainer.addView(makeRowView(
                    friend.getName() + " — " + status,
                    formatDistance(friend.getDistanceKm()) + " away",
                    0,
                    () -> {
                        if (mapReady && mapLibreMap != null) {
                            mapLibreMap.easeCamera(
                                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                            new CameraPosition.Builder()
                                                    .target(new LatLng(friend.getLatitude(), friend.getLongitude()))
                                                    .zoom(14)
                                                    .build()),
                                    800);
                        }
                    }
            ));
        }
        applyMapData();
    }

    /**
     * Draws all markers on the map with custom per-layer icons:
     * safe spots (green square), volunteers (blue square), friends
     * (green/red circle), and you (blue circle).
     */
    private void applyMapData() {
        if (!mapReady || mapLibreMap == null) return;

        mapLibreMap.removeAnnotations();
        mapMarkers.clear();

        for (final SafePlace place : safePlaces) {
            mapMarkers.add(mapLibreMap.addMarker(new MarkerOptions()
                    .title("Safe spot: " + place.getName())
                    .icon(MapIcons.safePlace(this))
                    .position(new LatLng(place.getLatitude(), place.getLongitude()))));
        }

        for (final Volunteer volunteer : volunteers) {
            if (!volunteer.isAvailable()) continue;
            mapMarkers.add(mapLibreMap.addMarker(new MarkerOptions()
                    .title("Volunteer: " + volunteer.getName())
                    .snippet(volunteer.getPhone() != null ? volunteer.getPhone() : "Available")
                    .icon(MapIcons.volunteer(this))
                    .position(new LatLng(volunteer.getLatitude(), volunteer.getLongitude()))));
        }

        for (final Friend friend : friends) {
            mapMarkers.add(mapLibreMap.addMarker(new MarkerOptions()
                    .title(friend.getName() + (friend.isSafe() ? " — SAFE" : " — NEEDS HELP"))
                    .snippet(friend.getDistanceKm() > 0
                            ? formatDistance(friend.getDistanceKm()) + " away"
                            : "Checked in")
                    .icon(friend.isSafe() ? MapIcons.friendSafe(this) : MapIcons.friendUnsafe(this))
                    .position(new LatLng(friend.getLatitude(), friend.getLongitude()))));
        }

        if (hasLocation) {
            LatLng self = new LatLng(lastLatitude, lastLongitude);
            mapMarkers.add(mapLibreMap.addMarker(new MarkerOptions()
                    .title("You")
                    .icon(MapIcons.self(this))
                    .position(self)));

            mapLibreMap.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(self, 12.5));
        }

        drawDisasterZone();
    }

    /**
     * Persistent banner: which disaster it is on top, safety instructions
     * listed separately below. Stays visible until the alert is cleared.
     */
    private void updateAlertBanner() {
        if (activeAlert == null) {
            alertBanner.setVisibility(View.GONE);
            return;
        }

        alertBanner.setVisibility(View.VISIBLE);
        alertBannerTitle.setText("🚨 " + activeAlert.type + " ALERT");
        alertBannerMessage.setText(activeAlert.message);

        // One row per instruction, listed separately below the title.
        alertBannerInstructions.removeAllViews();
        if (activeAlert.instructions != null) {
            for (String instruction : activeAlert.instructions) {
                TextView row = new TextView(this);
                row.setText("• " + instruction);
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                row.setTextColor(0xFFFFFFFF);
                row.setPadding(0, dp(3), 0, dp(3));
                alertBannerInstructions.addView(row);
            }
        }
    }

    /** Big red danger-zone circle for the active disaster alert (if located). */
    private void drawDisasterZone() {
        if (activeAlert == null
                || activeAlert.latitude == null
                || activeAlert.longitude == null
                || activeAlert.radiusKm == null
                || mapLibreMap == null) {
            return;
        }

        mapLibreMap.addPolygon(new PolygonOptions()
                .addAll(createCirclePoints(
                        activeAlert.latitude, activeAlert.longitude,
                        activeAlert.radiusKm * 1000))
                .fillColor(0x2Ed93025)
                .strokeColor(0xFFd93025));
    }

    /** Approximate circle points around a lat/lon for MapLibre polygons. */
    private static List<LatLng> createCirclePoints(double centerLat, double centerLon, double radiusM) {
        List<LatLng> points = new ArrayList<>();
        final int steps = 48;
        for (int i = 0; i <= steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            double dLat = (radiusM * Math.cos(angle)) / 111_320.0;
            double dLon = (radiusM * Math.sin(angle))
                    / (111_320.0 * Math.max(0.2, Math.cos(Math.toRadians(centerLat))));
            points.add(new LatLng(centerLat + dLat, centerLon + dLon));
        }
        return points;
    }

    /** Builds a tappable row; index 0 hides the number prefix. */
    private View makeRowView(String title, String distance, int index, Runnable onClick) {
        TextView row = new TextView(this);
        row.setText(index > 0
                ? String.format(Locale.US, "%d. %s — %s", index, title, distance)
                : String.format(Locale.US, "%s · %s", title, distance));
        row.setTextSize(15);
        row.setTextColor(0xFF17202A);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackgroundResource(R.drawable.row_background);
        row.setOnClickListener(v -> onClick.run());
        return row;
    }

    private int dp(int value) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(value * scale);
    }

    private String formatDistance(float distanceKm) {
        if (distanceKm < 1f) {
            return String.format(Locale.US, "%.0f m", distanceKm * 1000f);
        }
        return String.format(Locale.US, "%.2f km", distanceKm);
    }

    /** Opens turn-by-turn navigation (Google Maps app if installed, web fallback). */
    private void openNavigation(double latitude, double longitude) {
        Uri uri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + latitude + "," + longitude);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
                locationText.setText("Location permission denied — enable it in Settings.");
                refreshButton.setEnabled(true);
            }
        }
    }

    // MapView lifecycle passthrough
    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }

    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }

    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alertTimer.cancel();
        mapView.onDestroy();
    }
}
