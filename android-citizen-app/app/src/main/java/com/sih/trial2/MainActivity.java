package com.sih.trial2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import org.maplibre.android.maps.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String DEVICE_ID = "CITIZEN-" + android.os.Build.MODEL;

    /** Legacy disaster alerts (drill broadcasts) — popup + notification once per id. */
    private DisasterAlert activeAlert = null;
    private final java.util.Set<String> shownAlertIds = new java.util.HashSet<>();
    private final java.util.Timer alertTimer = new java.util.Timer();

    /** Simulated meteorological warnings, newest first (DEMO DATA). */
    private final List<Warning> warnings = new ArrayList<>();
    /** Warnings already shown as popup/notification for this session. */
    private final java.util.Set<String> shownWarningIds = new java.util.HashSet<>();
    private static final long WARNING_POLL_MS = 8000L;

    private final List<SafePlace> safePlaces = new ArrayList<>();
    private final List<Volunteer> volunteers = new ArrayList<>();
    private final List<CircleMember> circleMembers = new ArrayList<>();
    private final List<Marker> mapMarkers = new ArrayList<>();

    /** Free OpenStreetMap-based style (no Google Maps, no API key). */
    private static final String STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

    private LocationHelper locationHelper;
    private ApiClient apiClient;
    private MovementTracker movementTracker;
    private AppSettings settings;
    /** Live Socket.IO link; warnings/safety push instead of waiting for the poll. */
    private WarningSocketClient socketClient;

    // --- Views (Home tab) ---
    private TextView locationText;
    private TextView homeSafePlacesText;
    private TextView homeCircleText;
    private TextView sosStatusText;
    private TextView checkinStatusText;
    private Button refreshButton;
    private Button sosButton;
    private View settingsButton;
    private Button safeToggleButton;
    private Button imSafeButton;
    private LinearLayout homeSafePlacesContainer;
    private LinearLayout homeCircleContainer;

    // --- Shelter guidance card (Home) ---
    private TextView shelterAdviceText;
    private TextView shelterAdviceReasonText;
    private View shelterAdviceStripe;
    private TextView shelterAdviceSimulatedNote;
    private LinearLayout shelterAdviceContainer;

    // --- Warning banner (Home) ---
    private LinearLayout warningBanner;
    private View warningBannerStripe;
    private TextView warningBannerTitle;
    private TextView warningBannerMeta;
    private TextView warningBannerRelevance;

    // --- Alerts tab ---
    private TextView alertsActiveText;
    private LinearLayout alertsActiveContainer;
    private TextView alertsPastText;
    private LinearLayout alertsPastContainer;

    // --- Safe places tab ---
    private TextView safePlacesText;
    private TextView volunteersText;
    private LinearLayout safePlacesContainer;
    private LinearLayout volunteersContainer;

    // --- Safe circle tab ---
    private TextView deviceIdText;
    private TextView circleText;
    private LinearLayout circleContainer;
    private View addFriendButton;

    // --- Map ---
    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private boolean mapReady = false;

    // --- Navigation ---
    private View tabHome, tabAlerts, tabPlaces, tabMap, tabCircle;
    private View navHome, navAlerts, navPlaces, navMap, navCircle;    private double lastLatitude;
    private double lastLongitude;
    private Double lastAltitude;
    private boolean hasLocation = false;
    private boolean seededNearby = false;

    /** Nav id of the currently visible tab. */
    private int selectedTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bindViews();
        setupNavigation();

        settings = new AppSettings(this);
        apiClient = new ApiClient(settings.getBaseUrl(), settings.getApiKey());
        locationHelper = new LocationHelper(this);
        movementTracker = new MovementTracker();
        socketClient = new WarningSocketClient();
        connectLiveLink();

        if (deviceIdText != null) {
            deviceIdText.setText(DEVICE_ID);
        }

        refreshButton.setOnClickListener(v -> getUserLocation());
        sosButton.setOnClickListener(v -> reportEmergency());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        safeToggleButton.setOnClickListener(v -> toggleSafeStatus());
        imSafeButton.setOnClickListener(v -> markSafe());
        addFriendButton.setOnClickListener(v -> showAddFriendDialog());
        updateSafeToggleUi();

        setupMap(savedInstanceState);

        if (locationHelper.hasLocationPermission()) {
            getUserLocation();
        } else {
            locationHelper.requestLocationPermission();
        }

        AlertNotifier.requestPermissionIfNeeded(this);
        startPolling();
    }

    private void bindViews() {
        // Home tab
        locationText = findViewById(R.id.locationText);
        homeSafePlacesText = findViewById(R.id.homeSafePlacesText);
        homeCircleText = findViewById(R.id.homeCircleText);
        sosStatusText = findViewById(R.id.sosStatusText);
        checkinStatusText = findViewById(R.id.checkinStatusText);
        refreshButton = findViewById(R.id.refreshButton);
        sosButton = findViewById(R.id.sosButton);
        settingsButton = findViewById(R.id.settingsButton);
        safeToggleButton = findViewById(R.id.safeToggleButton);
        imSafeButton = findViewById(R.id.imSafeButton);
        homeSafePlacesContainer = findViewById(R.id.homeSafePlacesContainer);
        homeCircleContainer = findViewById(R.id.homeCircleContainer);

        // Shelter guidance card
        shelterAdviceText = findViewById(R.id.shelterAdviceText);
        shelterAdviceReasonText = findViewById(R.id.shelterAdviceReasonText);
        shelterAdviceStripe = findViewById(R.id.shelterAdviceStripe);
        shelterAdviceSimulatedNote = findViewById(R.id.shelterAdviceSimulatedNote);
        shelterAdviceContainer = findViewById(R.id.shelterAdviceContainer);

        // Warning banner
        warningBanner = findViewById(R.id.warningBanner);
        warningBannerStripe = findViewById(R.id.warningBannerStripe);
        warningBannerTitle = findViewById(R.id.warningBannerTitle);
        warningBannerMeta = findViewById(R.id.warningBannerMeta);
        warningBannerRelevance = findViewById(R.id.warningBannerRelevance);

        // Alerts tab
        alertsActiveText = findViewById(R.id.alertsActiveText);
        alertsActiveContainer = findViewById(R.id.alertsActiveContainer);
        alertsPastText = findViewById(R.id.alertsPastText);
        alertsPastContainer = findViewById(R.id.alertsPastContainer);

        // Safe places tab
        safePlacesText = findViewById(R.id.safePlacesText);
        volunteersText = findViewById(R.id.volunteersText);
        safePlacesContainer = findViewById(R.id.safePlacesContainer);
        volunteersContainer = findViewById(R.id.volunteersContainer);

        // Safe circle tab
        deviceIdText = findViewById(R.id.deviceIdText);
        circleText = findViewById(R.id.circleText);
        circleContainer = findViewById(R.id.circleContainer);
        addFriendButton = findViewById(R.id.addFriendButton);

        // Map
        mapView = findViewById(R.id.mapView);

        // Tabs + nav
        tabHome = findViewById(R.id.tabHome);
        tabAlerts = findViewById(R.id.tabAlerts);
        tabPlaces = findViewById(R.id.tabPlaces);
        tabMap = findViewById(R.id.tabMap);
        tabCircle = findViewById(R.id.tabCircle);
        navHome = findViewById(R.id.navHome);
        navAlerts = findViewById(R.id.navAlerts);
        navPlaces = findViewById(R.id.navPlaces);
        navMap = findViewById(R.id.navMap);
        navCircle = findViewById(R.id.navCircle);
    }

    /** Switches between the five sections. */
    private void setupNavigation() {
        View.OnClickListener listener = v -> selectTab(v.getId());
        navHome.setOnClickListener(listener);
        navAlerts.setOnClickListener(listener);
        navPlaces.setOnClickListener(listener);
        navMap.setOnClickListener(listener);
        navCircle.setOnClickListener(listener);
        selectTab(R.id.navHome);
    }

    private void selectTab(int navId) {
        selectedTab = navId;
        tabHome.setVisibility(navId == R.id.navHome ? View.VISIBLE : View.GONE);
        tabAlerts.setVisibility(navId == R.id.navAlerts ? View.VISIBLE : View.GONE);
        tabPlaces.setVisibility(navId == R.id.navPlaces ? View.VISIBLE : View.GONE);
        tabMap.setVisibility(navId == R.id.navMap ? View.VISIBLE : View.GONE);
        tabCircle.setVisibility(navId == R.id.navCircle ? View.VISIBLE : View.GONE);

        int selected = ContextCompat.getColor(this, R.color.brand_primary);
        int inactive = ContextCompat.getColor(this, R.color.nav_inactive);
        View[] navs = {navHome, navAlerts, navPlaces, navMap, navCircle};
        for (View nav : navs) {
            ((TextView) nav).setTextColor(nav.getId() == navId ? selected : inactive);
        }

        // MapLibre needs a layout pass + nudge after becoming visible.
        if (navId == R.id.navMap && mapReady && mapLibreMap != null) {
            mapView.post(() -> {
                if (hasLocation) {
                    mapLibreMap.moveCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastLatitude, lastLongitude), 12.5));
                }
                applyMapData();
            });
        }
    }

    /**
     * Live Socket.IO link: warning and safety pushes trigger the same refresh
     * paths as the poll timers, so updates land in under a second. Polling
     * continues as the fallback for dropped connections.
     */
    private void connectLiveLink() {
        if (socketClient == null) return;
        socketClient.connect(settings.getBaseUrl(), new WarningSocketClient.Listener() {
            @Override
            public void onWarningsChanged() {
                apiClient.getWarnings(new ApiClient.WarningsCallback() {
                    @Override
                    public void onSuccess(List<Warning> fresh) {
                        warnings.clear();
                        warnings.addAll(fresh);
                        notifyNewWarnings();
                        renderWarnings();
                    }

                    @Override
                    public void onError(String message) {
                        // Poll timer will pick it up.
                    }
                });
            }

            @Override
            public void onSafetyChanged() {
                refreshCircle();
            }
        });
    }

    /**
     * Polls legacy disaster alerts (2s delay, 8s period) and simulated
     * warnings (8s period). Warnings drive the home banner, Alerts tab,
     * notifications, and the map danger zones.
     */
    private void startPolling() {
        alertTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> apiClient.getActiveAlert(new ApiClient.AlertCallback() {
                    @Override
                    public void onSuccess(DisasterAlert alert) {
                        activeAlert = alert;

                        if (alert != null && !shownAlertIds.contains(alert.id)) {
                            shownAlertIds.add(alert.id);
                            AlertNotifier.showAlertDialog(MainActivity.this, alert);
                            AlertNotifier.showNotification(MainActivity.this, alert);
                        }
                        applyMapData();
                    }

                    @Override
                    public void onError(String message) {
                        // Silent — retries on next tick.
                    }
                }));
            }
        }, 2000L, 8000L);

        alertTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> apiClient.getWarnings(new ApiClient.WarningsCallback() {
                    @Override
                    public void onSuccess(List<Warning> fresh) {
                        warnings.clear();
                        warnings.addAll(fresh);
                        notifyNewWarnings();
                        renderWarnings();
                    }

                    @Override
                    public void onError(String message) {
                        // Silent — warnings retry on next tick.
                    }
                }));
            }
        }, 3500L, WARNING_POLL_MS);
    }

    /** Popup + notification exactly once per warning, for DIRECT-relevance ones. */
    private void notifyNewWarnings() {
        if (!hasLocation) return;
        for (Warning warning : warnings) {
            if (!warning.active || shownWarningIds.contains(warning.id)) continue;
            Warning.Relevance relevance = warning.relevanceFor(lastLatitude, lastLongitude);
            if (relevance == Warning.Relevance.INFORMATIONAL) continue;

            shownWarningIds.add(warning.id);
            if (relevance == Warning.Relevance.DIRECT
                    && ("HIGH".equals(warning.severity) || "CRITICAL".equals(warning.severity))) {
                showWarningDialog(warning);
            }
            WarningNotifier.showNotification(this, warning);
        }
    }

    /** Full-screen dialog for high-severity warnings affecting the user's area. */
    private void showWarningDialog(Warning warning) {
        StringBuilder text = new StringBuilder(warning.description).append("\n\n");
        if (warning.isAdvance()) {
            text.append("Expected in ")
                    .append(formatDuration(warning.msUntilStart()))
                    .append("\n\n");
        }
        for (String line : warning.immediateActions) {
            text.append("• ").append(line).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle(warning.severity + " " + warning.typeLabel().toUpperCase(Locale.US)
                        + " WARNING (SIMULATED)")
                .setMessage(text.toString())
                .setPositiveButton("I understand", null)
                .show();
    }

    // ------------------------------------------------------------------
    // Warning rendering: home banner + alerts tab
    // ------------------------------------------------------------------

    private void renderWarnings() {
        renderHomeBanner();
        renderAlertsTab();
        applyMapData();
    }

    /** Home shows the most urgent relevant active warning (direct > nearby > none). */
    private void renderHomeBanner() {
        Warning best = null;
        int bestRank = -1;
        if (hasLocation) {
            for (Warning warning : warnings) {
                if (!warning.active) continue;
                Warning.Relevance relevance = warning.relevanceFor(lastLatitude, lastLongitude);
                if (relevance == Warning.Relevance.INFORMATIONAL) continue;

                int rank = severityRank(warning.severity)
                        * (relevance == Warning.Relevance.DIRECT ? 10 : 1);
                if (rank > bestRank) {
                    bestRank = rank;
                    best = warning;
                }
            }
        }

        if (best == null) {
            warningBanner.setVisibility(View.GONE);
            return;
        }

        warningBanner.setVisibility(View.VISIBLE);
        int stripeColor;
        switch (best.severity) {
            case "CRITICAL": stripeColor = R.color.sev_stripe_critical; break;
            case "HIGH": stripeColor = R.color.sev_stripe_high; break;
            case "MEDIUM": stripeColor = R.color.sev_stripe_medium; break;
            default: stripeColor = R.color.sev_stripe_low; break;
        }
        warningBannerStripe.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, stripeColor)));

        warningBannerTitle.setText(best.title);
        String timing = best.isAdvance()
                ? "Expected in " + formatDuration(best.msUntilStart())
                : "In progress";
        warningBannerMeta.setText(best.severity + " · " + timing + " · " + best.regionName);

        Warning.Relevance relevance = best.relevanceFor(lastLatitude, lastLongitude);
        warningBannerRelevance.setText(relevance == Warning.Relevance.DIRECT
                ? R.string.warning_relevance_direct
                : R.string.warning_relevance_nearby);
    }

    /** Alerts tab: active warnings with details, then history below. */
    private void renderAlertsTab() {
        alertsActiveContainer.removeAllViews();
        alertsPastContainer.removeAllViews();

        int activeCount = 0;
        int pastCount = 0;
        for (Warning warning : warnings) {
            if (warning.active) {
                activeCount++;
                if (activeCount == 1) alertsActiveText.setVisibility(View.GONE);
                alertsActiveContainer.addView(makeWarningCard(warning, true));
            } else {
                pastCount++;
                if (pastCount == 1) alertsPastText.setVisibility(View.GONE);
                alertsPastContainer.addView(makeWarningCard(warning, false));
            }
        }

        if (activeCount == 0) {
            alertsActiveText.setVisibility(View.VISIBLE);
            alertsActiveText.setText(R.string.alerts_empty);
        }
        if (pastCount == 0) {
            alertsPastText.setVisibility(View.VISIBLE);
            alertsPastText.setText(R.string.alerts_empty);
        }
    }

    /** Card with severity stripe, timing, instructions (active) or summary (past). */
    private View makeWarningCard(Warning warning, boolean isActive) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.card_background);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(8);
        card.setLayoutParams(cardParams);

        View stripe = new View(this);
        stripe.setBackgroundResource(R.drawable.severity_stripe);
        int stripeColor;
        switch (warning.severity) {
            case "CRITICAL": stripeColor = R.color.sev_stripe_critical; break;
            case "HIGH": stripeColor = R.color.sev_stripe_high; break;
            case "MEDIUM": stripeColor = R.color.sev_stripe_medium; break;
            default: stripeColor = R.color.sev_stripe_low; break;
        }
        stripe.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, stripeColor)));
        card.addView(stripe, new LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        body.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(warning.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        body.addView(title);

        TextView meta = new TextView(this);
        String timing;
        if (!isActive) {
            timing = "CANCELLED".equals(warning.status) ? "Withdrawn" : "Expired";
        } else if (warning.isAdvance()) {
            timing = "Expected in " + formatDuration(warning.msUntilStart());
        } else {
            timing = "In progress — ends in " + formatDuration(warning.msUntilEnd());
        }
        meta.setText(warning.severity + " · " + warning.regionName + " · " + timing);
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        meta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        meta.setPadding(0, dp(3), 0, 0);
        body.addView(meta);

        if (warning.description != null && warning.description.length() > 0) {
            TextView desc = new TextView(this);
            desc.setText(warning.description);
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            desc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            desc.setPadding(0, dp(6), 0, 0);
            body.addView(desc);
        }

        if (isActive && !warning.immediateActions.isEmpty()) {
            TextView actions = new TextView(this);
            StringBuilder sb = new StringBuilder("WHAT TO DO:\n");
            for (String line : warning.immediateActions) {
                sb.append("• ").append(line).append("\n");
            }
            actions.setText(sb.toString().trim());
            actions.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            actions.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            actions.setPadding(0, dp(8), 0, 0);
            body.addView(actions);
        }

        card.addView(body, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private static int severityRank(String severity) {
        switch (severity) {
            case "CRITICAL": return 4;
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            default: return 1;
        }
    }

    /** "1 h 42 m" / "45 m" / "30 s" from a millisecond span. */
    private static String formatDuration(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return h + " h " + m + " min";
        if (m > 0) return m + " min";
        return s + " sec";
    }

    // ------------------------------------------------------------------
    // Map
    // ------------------------------------------------------------------

    /** Map: free OSM tiles, full gestures, custom icons, click dialogs. */
    private void setupMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            map.getUiSettings().setAllGesturesEnabled(true);

            map.setOnMarkerClickListener(marker -> {
                showMarkerDialog(marker);
                return true;
            });

            map.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
                mapReady = true;
                applyMapData();
            });
        });
    }

    /** Tap-action dialog for volunteer (call/navigate) and safe place (navigate). */
    private void showMarkerDialog(Marker marker) {
        final Volunteer volunteer = findVolunteerByMarker(marker);
        final SafePlace place = volunteer == null ? findSafePlaceByMarker(marker) : null;

        if (volunteer != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Volunteer: " + volunteer.getName())
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
                    .setTitle(place.getName())
                    .setMessage("Safe spot near you" + (place.getDistanceKm() > 0
                            ? " — " + formatDistance(place.getDistanceKm())
                            : ""))
                    .setPositiveButton("Navigate", (d, w) ->
                            openNavigation(place.getLatitude(), place.getLongitude()))
                    .setNegativeButton("Close", null)
                    .show();
        }
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

    /**
     * Draws markers with custom per-layer icons: safe spots (green square),
     * volunteers (blue square), friends (green/red circle), and you
     * (blue circle). Adds danger-zone polygons for active alerts + warnings.
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

        for (final CircleMember member : circleMembers) {
            if (member.latitude == null || member.longitude == null) continue;
            boolean safe = member.safetyStatus == CircleMember.SafetyStatus.SAFE;
            mapMarkers.add(mapLibreMap.addMarker(new MarkerOptions()
                    .title(member.displayName() + (safe ? " — SAFE" : " — NEEDS HELP"))
                    .icon(safe ? MapIcons.friendSafe(this) : MapIcons.friendUnsafe(this))
                    .position(new LatLng(member.latitude, member.longitude))));
        }

        if (hasLocation) {
            LatLng self = new LatLng(lastLatitude, lastLongitude);
            mapMarkers.add(mapLibreMap.addMarker(new MarkerOptions()
                    .title("You")
                    .icon(MapIcons.self(this))
                    .position(self)));

            mapLibreMap.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(self, 12.5));
        }

        drawDisasterZones();
    }

    /** Danger-zone polygons for the legacy alert + all active warnings. */
    private void drawDisasterZones() {
        if (mapLibreMap == null) return;

        if (activeAlert != null && activeAlert.latitude != null
                && activeAlert.longitude != null && activeAlert.radiusKm != null) {
            mapLibreMap.addPolygon(new PolygonOptions()
                    .addAll(createCirclePoints(activeAlert.latitude, activeAlert.longitude,
                            activeAlert.radiusKm * 1000))
                    .fillColor(0x2Ed93025)
                    .strokeColor(0xFFd93025));
        }

        long now = System.currentTimeMillis();
        for (Warning warning : warnings) {
            if (!warning.active || warning.msUntilEnd() < 0 || warning.expectedStartAtMs > now + 3_600_000) {
                continue; // only show zones for ongoing/imminent warnings
            }
            mapLibreMap.addPolygon(new PolygonOptions()
                    .addAll(createCirclePoints(warning.latitude, warning.longitude,
                            warning.radiusKm * 1000))
                    .fillColor(severityFill(warning.severity))
                    .strokeColor(severityStroke(warning.severity)));
        }
    }

    private static int severityStroke(String severity) {
        switch (severity) {
            case "CRITICAL": return 0xFFC62828;
            case "HIGH": return 0xFFE65100;
            case "MEDIUM": return 0xFFF9A825;
            default: return 0xFF2E7D32;
        }
    }

    private static int severityFill(String severity) {
        switch (severity) {
            case "CRITICAL": return 0x2EC62828;
            case "HIGH": return 0x2EE65100;
            case "MEDIUM": return 0x2EF9A825;
            default: return 0x2E2E7D32;
        }
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

    // ------------------------------------------------------------------
    // Safety status / I'm Safe / Safe Circle
    // ------------------------------------------------------------------

    /** In-app settings dialog: display name + backend URL (per-device, no rebuild). */
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

        final EditText keyInput = new EditText(this);
        keyInput.setHint("API key (leave blank if backend has none)");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setText(settings.getApiKey());
        box.addView(keyInput);

        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(box)
                .setPositiveButton("Save", (dialog, which) -> {
                    settings.setName(nameInput.getText().toString());
                    String newUrl = urlInput.getText().toString().trim();
                    String newKey = keyInput.getText().toString().trim();
                    boolean urlChanged = !newUrl.equals(settings.getBaseUrl());
                    boolean keyChanged = !newKey.equals(settings.getApiKey());
                    settings.setBaseUrl(newUrl);
                    settings.setApiKey(newKey);
                    if (urlChanged || keyChanged) {
                        apiClient = new ApiClient(settings.getBaseUrl(), settings.getApiKey());
                        Toast.makeText(this, "Backend updated — tap Refresh.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Flips declared safety status and checks in immediately. */
    private void toggleSafeStatus() {
        boolean nowSafe = !settings.isSafe();
        settings.setSafe(nowSafe);
        updateSafeToggleUi();
        if (hasLocation) {
            checkIn(nowSafe);
        }
    }

    private void updateSafeToggleUi() {
        int color = settings.isSafe() ? R.color.safe_green : R.color.danger;
        safeToggleButton.setText(settings.isSafe() ? R.string.safe_toggle_on : R.string.safe_toggle_off);
        safeToggleButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
    }

    /** I'M SAFE: explicit positive check-in, visible to the Safe Circle. */
    private void markSafe() {
        settings.setSafe(true);
        updateSafeToggleUi();
        if (!hasLocation) {
            Toast.makeText(this, "Waiting for location — tap Refresh.", Toast.LENGTH_SHORT).show();
            getUserLocation();
            return;
        }
        checkIn(true);
        Toast.makeText(this, R.string.im_safe_done, Toast.LENGTH_LONG).show();
    }

    /** Sends this device's position + safety status to the backend. */
    private void checkIn(boolean safe) {
        checkinStatusText.setText("Checking in…");
        apiClient.checkIn(DEVICE_ID, settings.getName(), lastLatitude, lastLongitude, safe,
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        checkinStatusText.setText("Checked in — your Safe Circle can see your status.");
                        refreshCircle();
                    }

                    @Override
                    public void onError(String message) {
                        checkinStatusText.setText("Check-in failed — retrying on next refresh.");
                    }
                });
    }

    /** Add-friend dialog: friend's device ID + optional display name. */
    private void showAddFriendDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), 0);

        final EditText idInput = new EditText(this);
        idInput.setHint(R.string.circle_add_hint);
        idInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(idInput);

        final EditText nameInput = new EditText(this);
        nameInput.setHint(R.string.circle_add_name_hint);
        nameInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(nameInput);

        new AlertDialog.Builder(this)
                .setTitle(R.string.circle_add_title)
                .setView(box)
                .setPositiveButton("Add", (dialog, which) -> {
                    String memberId = idInput.getText().toString().trim();
                    if (memberId.isEmpty()) {
                        Toast.makeText(this, "Device ID is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    apiClient.addCircleMember(DEVICE_ID, memberId,
                            nameInput.getText().toString().trim(),
                            new ApiClient.ApiCallback() {
                                @Override
                                public void onSuccess(String responseBody) {
                                    Toast.makeText(MainActivity.this,
                                            "Friend added to Safe Circle.", Toast.LENGTH_SHORT).show();
                                    refreshCircle();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Pulls the Safe Circle and re-renders circle + home glance. */
    private void refreshCircle() {
        apiClient.getSafeCircle(DEVICE_ID, new ApiClient.CircleCallback() {
            @Override
            public void onSuccess(List<CircleMember> members) {
                circleMembers.clear();
                circleMembers.addAll(members);
                renderCircle();
                applyMapData();
            }

            @Override
            public void onError(String message) {
                circleText.setText(message);
                homeCircleText.setText(message);
            }
        });
    }

    private void renderCircle() {
        renderCircleList(circleContainer, circleText, true);
        renderCircleList(homeCircleContainer, homeCircleText, false);
    }

    /** Renders circle rows; the full tab shows remove buttons, the glance does not. */
    private void renderCircleList(LinearLayout container, TextView emptyText, boolean withRemove) {
        container.removeAllViews();

        if (circleMembers.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(R.string.circle_empty);
            return;
        }

        emptyText.setVisibility(View.GONE);
        for (final CircleMember member : circleMembers) {
            container.addView(makeCircleRow(member, withRemove));
        }
    }

    private View makeCircleRow(final CircleMember member, boolean withRemove) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.row_background);
        int pad = dp(12);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        row.setLayoutParams(params);

        // Status dot
        View dot = new View(this);
        int dotColor;
        switch (member.safetyStatus) {
            case SAFE: dotColor = R.color.safe_green; break;
            case POSSIBLE_EMERGENCY: dotColor = R.color.danger; break;
            default: dotColor = R.color.unknown_gray; break;
        }
        dot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, dotColor)));
        dot.setBackgroundResource(R.drawable.severity_chip_bg);
        row.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(10);
        textCol.setLayoutParams(textParams);

        TextView name = new TextView(this);
        name.setText(member.displayName());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        textCol.addView(name);

        TextView status = new TextView(this);
        int statusRes;
        switch (member.safetyStatus) {
            case SAFE: statusRes = R.string.circle_status_safe; break;
            case POSSIBLE_EMERGENCY: statusRes = R.string.circle_status_emergency; break;
            default: statusRes = R.string.circle_status_unknown; break;
        }
        status.setText(statusRes);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        status.setTextColor(ContextCompat.getColor(this, dotColor));
        textCol.addView(status);

        row.addView(textCol);

        if (withRemove) {
            TextView remove = new TextView(this);
            remove.setText(R.string.circle_remove);
            remove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            remove.setTextColor(ContextCompat.getColor(this, R.color.danger));
            remove.setPadding(dp(10), dp(6), dp(4), dp(6));
            remove.setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Remove " + member.displayName() + "?")
                    .setMessage("They will no longer see your status and you will not see theirs.")
                    .setPositiveButton("Remove", (d, w) ->
                            apiClient.removeCircleMember(DEVICE_ID, member.deviceId,
                                    new ApiClient.ApiCallback() {
                                        @Override
                                        public void onSuccess(String responseBody) {
                                            refreshCircle();
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(MainActivity.this, message,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }))
                    .setNegativeButton("Cancel", null)
                    .show());
            row.addView(remove);
        }

        return row;
    }

    // ------------------------------------------------------------------
    // Location + backend data
    // ------------------------------------------------------------------

    private void reportEmergency() {
        if (!hasLocation) {
            Toast.makeText(this, "Waiting for GPS — try again shortly.", Toast.LENGTH_LONG).show();
            getUserLocation();
            return;
        }

        sosButton.setEnabled(false);
        sosStatusText.setText("Reporting emergency to backend…");

        long stationaryMinutes = Math.max(1, movementTracker.getStationaryMinutes());
        int nearbyDevices = NearbyDevicesProvider.getNearbyDeviceCount(this);

        apiClient.postEmergency(DEVICE_ID, lastLatitude, lastLongitude, lastAltitude,
                stationaryMinutes, nearbyDevices, true,
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
        locationText.setText("Getting your GPS location…");
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
                loadShelterAdvice(lastLatitude, lastLongitude);
                checkIn(settings.isSafe());
            }

            @Override
            public void onLocationError(@NonNull String message) {
                refreshButton.setEnabled(true);
                locationText.setText(message);
                safePlacesText.setText(R.string.location_needed_safe_spots);
                homeSafePlacesText.setText(R.string.location_needed_safe_spots);
                volunteersText.setText(R.string.location_needed_volunteers);
                safePlacesContainer.removeAllViews();
                homeSafePlacesContainer.removeAllViews();
                volunteersContainer.removeAllViews();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Pulls safe spots, volunteers, and the Safe Circle from the backend.
     * First GPS fix of a session also seeds safe spots around the user's area.
     */
    private void loadBackendData(double userLatitude, double userLongitude) {
        safePlacesText.setVisibility(View.VISIBLE);
        volunteersText.setVisibility(View.VISIBLE);
        homeSafePlacesText.setVisibility(View.VISIBLE);
        homeCircleText.setVisibility(View.VISIBLE);
        safePlacesText.setText(R.string.loading);
        volunteersText.setText(R.string.loading);
        homeSafePlacesText.setText(R.string.loading);
        homeCircleText.setText(R.string.loading);
        safePlacesContainer.removeAllViews();
        volunteersContainer.removeAllViews();
        homeSafePlacesContainer.removeAllViews();
        homeCircleContainer.removeAllViews();

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

        refreshCircle();
    }

    /**
     * Asks the backend whether the user should evacuate to a shelter, stay
     * put, or do nothing — and which shelters are best, with reasons.
     * Occupancy figures shown are simulated demo data (labeled in the UI).
     */
    private void loadShelterAdvice(double userLatitude, double userLongitude) {
        shelterAdviceText.setText(R.string.shelter_guidance_loading);
        shelterAdviceReasonText.setText("");
        shelterAdviceContainer.removeAllViews();
        shelterAdviceSimulatedNote.setVisibility(View.GONE);
        shelterAdviceStripe.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.nav_inactive)));

        apiClient.getShelterRecommendation(userLatitude, userLongitude,
                new ApiClient.ShelterAdviceCallback() {
                    @Override
                    public void onSuccess(ShelterAdvice advice) {
                        renderShelterAdvice(advice);
                    }

                    @Override
                    public void onError(String message) {
                        shelterAdviceText.setText("Shelter guidance unavailable.");
                        shelterAdviceReasonText.setText(message);
                    }
                });
    }

    /** Advice header + up to three tappable recommended shelters. */
    private void renderShelterAdvice(ShelterAdvice advice) {
        int stripeColor;
        String headline;
        if (advice.isEvacuate()) {
            stripeColor = R.color.danger;
            headline = getString(R.string.shelter_guidance_evacuate);
        } else if ("MONITOR_STAY_PUT".equals(advice.advice)) {
            stripeColor = R.color.sev_stripe_medium;
            headline = getString(R.string.shelter_guidance_stay_put);
        } else {
            stripeColor = R.color.safe_green;
            headline = getString(R.string.shelter_guidance_no_action);
        }
        shelterAdviceStripe.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, stripeColor)));
        shelterAdviceText.setText(headline);

        StringBuilder reason = new StringBuilder();
        if (advice.adviceReason != null && advice.adviceReason.length() > 0) {
            reason.append(advice.adviceReason);
        }
        if (advice.warningSeverity != null && advice.warningTitle != null) {
            if (reason.length() > 0) reason.append("\n");
            reason.append(advice.warningSeverity).append(" — ").append(advice.warningTitle);
        }
        shelterAdviceReasonText.setText(reason.toString());

        shelterAdviceContainer.removeAllViews();
        for (ShelterAdvice.RecommendedShelter shelter : advice.shelters) {
            String distance = shelter.travelMinutes > 0
                    ? formatDistance((float) shelter.distanceKm) + " · ~" + shelter.travelMinutes + " min walk"
                    : formatDistance((float) shelter.distanceKm);
            String subtitle = (shelter.recommended ? "★ " : "") + distance
                    + (shelter.reason != null && shelter.reason.length() > 0
                            ? " — " + shelter.reason : "");
            shelterAdviceContainer.addView(makeRowView(
                    shelter.name,
                    subtitle,
                    0,
                    () -> openNavigation(shelter.latitude, shelter.longitude)
            ));
        }

        shelterAdviceSimulatedNote.setVisibility(
                advice.shelters.isEmpty() ? View.GONE : View.VISIBLE);
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
                homeSafePlacesText.setText(message);
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
        homeSafePlacesText.setVisibility(View.GONE);
        safePlacesContainer.removeAllViews();
        homeSafePlacesContainer.removeAllViews();

        if (safePlaces.isEmpty()) {
            String message = "No safe spots registered yet.";
            safePlacesText.setVisibility(View.VISIBLE);
            safePlacesText.setText(message);
            homeSafePlacesText.setVisibility(View.VISIBLE);
            homeSafePlacesText.setText(message);
            return;
        }

        // Home glance: top 3. Full list on the Safe Places tab.
        int homeLimit = Math.min(3, safePlaces.size());
        for (int i = 0; i < homeLimit; i++) {
            final SafePlace place = safePlaces.get(i);
            homeSafePlacesContainer.addView(makeRowView(
                    place.getName(),
                    formatDistance(place.getDistanceKm()),
                    i + 1,
                    () -> openNavigation(place.getLatitude(), place.getLongitude())
            ));
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
                    volunteer.getPhone() != null
                            ? formatDistance(volunteer.getDistanceKm()) + " · " + volunteer.getPhone()
                            : formatDistance(volunteer.getDistanceKm()),
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

    // ------------------------------------------------------------------
    // Shared UI helpers
    // ------------------------------------------------------------------

    /** Builds a tappable row; index 0 hides the number prefix. */
    private View makeRowView(String title, String distance, int index, Runnable onClick) {
        TextView row = new TextView(this);
        row.setText(index > 0
                ? String.format(Locale.US, "%d. %s — %s", index, title, distance)
                : String.format(Locale.US, "%s · %s", title, distance));
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        row.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackgroundResource(R.drawable.row_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        row.setLayoutParams(params);
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
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        if (socketClient != null && settings != null) {
            connectLiveLink();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if (socketClient != null) {
            socketClient.disconnect();
        }
    }

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
        if (socketClient != null) {
            socketClient.disconnect();
        }
    }
}
