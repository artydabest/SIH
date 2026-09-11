package com.sih.trial2;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thin HTTP client for the disaster-relay backend.
 *
 * Base URL notes:
 *  - Emulator: uses the adb reverse tunnel. Run once per emulator boot:
 *      adb reverse tcp:3000 tcp:3000
 *    This is the most reliable path — it works even when 10.0.2.2
 *    (host loopback alias) is blocked by VPNs or firewall rules.
 *  - Physical device: use your PC's LAN IP instead, e.g. "http://192.168.1.5:3000"
 *    (device and PC must be on the same Wi-Fi network).
 */
public class ApiClient {

    private static final String TAG = "ApiClient";

    private final String baseUrl;

    private static final int TIMEOUT_MS = 8000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** @param baseUrl e.g. "http://10.0.2.2:3000" or "http://192.168.1.20:3000" */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    /** Single-value callback delivered on the main thread. */
    public interface ApiCallback {
        void onSuccess(String responseBody);

        void onError(String message);
    }

    /** List-style callback already parsed into models, delivered on the main thread. */
    public interface SafeZonesCallback {
        void onSuccess(List<SafePlace> safePlaces);

        void onError(String message);
    }

    public interface VolunteersCallback {
        void onSuccess(List<Volunteer> volunteers);

        void onError(String message);
    }

    public interface FriendsCallback {
        void onSuccess(List<Friend> friends);

        void onError(String message);
    }

    public interface AlertCallback {
        void onSuccess(DisasterAlert alert);

        void onError(String message);
    }

    /** GET /api/alerts?active=1 — the newest active disaster alert, if any. */
    public void getActiveAlert(final AlertCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("GET", baseUrl + "/api/alerts?active=1", null);
                JSONObject json = new JSONObject(response);
                org.json.JSONArray alerts = json.getJSONArray("alerts");

                if (alerts.length() == 0) {
                    mainHandler.post(() -> callback.onSuccess(null));
                    return;
                }

                JSONObject alertJson = alerts.getJSONObject(0);
                java.util.List<String> instructions = new ArrayList<>();
                org.json.JSONArray instr = alertJson.optJSONArray("instructions");
                if (instr != null) {
                    for (int i = 0; i < instr.length(); i++) {
                        instructions.add(instr.getString(i));
                    }
                }

                DisasterAlert alert = new DisasterAlert(
                        alertJson.getString("_id"),
                        alertJson.getString("type"),
                        alertJson.getString("message"),
                        alertJson.has("latitude") && !alertJson.isNull("latitude")
                                ? alertJson.getDouble("latitude") : null,
                        alertJson.has("longitude") && !alertJson.isNull("longitude")
                                ? alertJson.getDouble("longitude") : null,
                        alertJson.has("radiusKm") && !alertJson.isNull("radiusKm")
                                ? alertJson.getDouble("radiusKm") : null,
                        alertJson.optBoolean("active", true),
                        instructions
                );
                mainHandler.post(() -> callback.onSuccess(alert));
            } catch (Exception e) {
                postAlertError(callback, "Could not load alerts: " + e.getMessage());
            }
        });
    }

    private void postAlertError(final AlertCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }

    /**
     * GET /api/people — everyone's latest check-in. Includes this device too;
     * the caller filters itself out by deviceId when rendering "friends".
     */
    public void getPeople(final FriendsCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("GET", baseUrl + "/api/people", null);
                JSONObject json = new JSONObject(response);
                JSONArray people = json.getJSONArray("people");

                List<Friend> friends = new ArrayList<>();
                for (int i = 0; i < people.length(); i++) {
                    JSONObject person = people.getJSONObject(i);
                    friends.add(new Friend(
                            person.getString("deviceId"),
                            person.optString("name", null),
                            person.getDouble("latitude"),
                            person.getDouble("longitude"),
                            person.optBoolean("safe", true),
                            person.optString("lastSeenAt", null)
                    ));
                }
                mainHandler.post(() -> callback.onSuccess(friends));
            } catch (Exception e) {
                postFriendError(callback, "Could not load friends: " + e.getMessage());
            }
        });
    }

    /**
     * POST /api/safezones/seed-nearby — asks the backend to generate safe
     * spots around the device's current GPS position. Called once per launch
     * so the safe-spot list is always relevant to where the user actually is.
     */
    public void seedNearbySafeZones(
            double latitude,
            double longitude,
            ApiCallback callback
    ) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("latitude", latitude);
                body.put("longitude", longitude);

                String response = request(
                        "POST",
                        baseUrl + "/api/safezones/seed-nearby",
                        body.toString()
                );
                postSuccess(callback, response);
            } catch (Exception e) {
                postError(callback, "Seed failed: " + e.getMessage());
            }
        });
    }

    private void postFriendError(final FriendsCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }

    /**
     * POST /api/emergency — report an emergency with location + detection data.
     * The backend computes confidence; the app never fabricates it.
     */
    public void postEmergency(
            String deviceId,
            double latitude,
            double longitude,
            Double altitude,
            long stationaryMinutes,
            int nearbyDevices,
            boolean emergencyMode,
            ApiCallback callback
    ) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("deviceId", deviceId);
                body.put("latitude", latitude);
                body.put("longitude", longitude);
                if (altitude != null) {
                    body.put("altitude", altitude);
                }
                body.put("stationaryMinutes", stationaryMinutes);
                body.put("nearbyDevices", nearbyDevices);
                body.put("emergencyMode", emergencyMode);

                String response = request(
                        "POST",
                        baseUrl + "/api/emergency",
                        body.toString()
                );
                postSuccess(callback, response);
            } catch (Exception e) {
                postError(callback, "Report failed: " + e.getMessage());
            }
        });
    }

    /** GET /api/safezones — safe shelters/hospitals/camps for the nearest-first list. */
    public void getSafeZones(final SafeZonesCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("GET", baseUrl + "/api/safezones", null);
                JSONObject json = new JSONObject(response);
                JSONArray zones = json.getJSONArray("safeZones");

                List<SafePlace> places = new ArrayList<>();
                for (int i = 0; i < zones.length(); i++) {
                    JSONObject zone = zones.getJSONObject(i);
                    places.add(new SafePlace(
                            zone.getString("name"),
                            zone.getDouble("latitude"),
                            zone.getDouble("longitude")
                    ));
                }
                mainHandler.post(() -> callback.onSuccess(places));
            } catch (Exception e) {
                postSafeZoneError(callback, "Could not load safe zones: " + e.getMessage());
            }
        });
    }

    /**
     * POST /api/people/checkin — share this device's location and safety status
     * so every dashboard and teammate sees this person on the map.
     */
    public void checkIn(
            String deviceId,
            String name,
            double latitude,
            double longitude,
            boolean safe,
            ApiCallback callback
    ) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("deviceId", deviceId);
                if (name != null && name.trim().length() > 0) {
                    body.put("name", name.trim());
                }
                body.put("latitude", latitude);
                body.put("longitude", longitude);
                body.put("safe", safe);

                String response = request(
                        "POST",
                        baseUrl + "/api/people/checkin",
                        body.toString()
                );
                postSuccess(callback, response);
            } catch (Exception e) {
                postError(callback, "Check-in failed: " + e.getMessage());
            }
        });
    }

    /** GET /api/volunteers — available volunteers for the nearest-first list. */
    public void getVolunteers(final VolunteersCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("GET", baseUrl + "/api/volunteers", null);
                JSONObject json = new JSONObject(response);
                JSONArray people = json.getJSONArray("volunteers");

                List<Volunteer> volunteers = new ArrayList<>();
                for (int i = 0; i < people.length(); i++) {
                    JSONObject person = people.getJSONObject(i);
                    volunteers.add(new Volunteer(
                            person.getString("name"),
                            person.getDouble("latitude"),
                            person.getDouble("longitude"),
                            person.optBoolean("available", true),
                            person.optString("phone", null)
                    ));
                }
                mainHandler.post(() -> callback.onSuccess(volunteers));
            } catch (Exception e) {
                postVolunteerError(callback, "Could not load volunteers: " + e.getMessage());
            }
        });
    }

    /** Blocking HTTP request; always call from a background thread. */
    private String request(String method, String urlString, String jsonBody) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            if (jsonBody != null) {
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int code = connection.getResponseCode();
            java.io.InputStream stream =
                    code >= 400 ? connection.getErrorStream() : connection.getInputStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
            }

            if (code >= 400) {
                throw new Exception("HTTP " + code + ": " + response);
            }
            return response.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void postSuccess(final ApiCallback callback, final String body) {
        mainHandler.post(() -> callback.onSuccess(body));
    }

    private void postError(final ApiCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }

    private void postSafeZoneError(final SafeZonesCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }

    private void postVolunteerError(final VolunteersCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }
}
