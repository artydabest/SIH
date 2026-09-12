package com.sih.trial2;

import android.net.Uri;
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

    /** Shared secret sent as X-API-Key; null/empty means the backend runs without a key. */
    private final String apiKey;

    private static final int TIMEOUT_MS = 8000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** @param baseUrl e.g. "http://10.0.2.2:3000" or "http://192.168.1.20:3000" */
    public ApiClient(String baseUrl) {
        this(baseUrl, null);
    }

    /** @param apiKey shared backend secret; may be null or empty when the backend has none set. */
    public ApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.apiKey = apiKey == null || apiKey.trim().isEmpty() ? null : apiKey.trim();
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

    public interface AlertCallback {
        void onSuccess(DisasterAlert alert);

        void onError(String message);
    }

    public interface WarningsCallback {
        void onSuccess(List<Warning> warnings);

        void onError(String message);
    }

    public interface CircleCallback {
        void onSuccess(List<CircleMember> members);

        void onError(String message);
    }

    public interface ShelterAdviceCallback {
        void onSuccess(ShelterAdvice advice);

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

    /**
     * GET /api/safezones/recommend?latitude=&longitude= — the backend's
     * explained shelter recommendation (advice + top shelters with reasons).
     * Occupancy data in the response is simulated demo data.
     */
    public void getShelterRecommendation(
            double latitude,
            double longitude,
            final ShelterAdviceCallback callback
    ) {
        executor.execute(() -> {
            try {
                String response = request("GET", baseUrl
                        + "/api/safezones/recommend?latitude=" + latitude
                        + "&longitude=" + longitude, null);
                JSONObject json = new JSONObject(response);

                JSONObject relevantWarning = json.optJSONObject("relevantWarning");
                JSONArray recommendationJson = json.optJSONArray("recommendations");

                List<ShelterAdvice.RecommendedShelter> shelters = new ArrayList<>();
                if (recommendationJson != null) {
                    for (int i = 0; i < recommendationJson.length(); i++) {
                        JSONObject r = recommendationJson.getJSONObject(i);
                        shelters.add(new ShelterAdvice.RecommendedShelter(
                                r.getString("name"),
                                r.optString("kind", "SHELTER"),
                                r.getDouble("latitude"),
                                r.getDouble("longitude"),
                                r.getDouble("distanceKm"),
                                r.optInt("travelMinutes", 0),
                                r.optString("reason", ""),
                                r.optBoolean("recommended", false)
                        ));
                    }
                }

                ShelterAdvice advice = new ShelterAdvice(
                        json.optString("advice", "NO_ACTION"),
                        json.optString("adviceReason", ""),
                        relevantWarning != null ? relevantWarning.optString("title", null) : null,
                        relevantWarning != null ? relevantWarning.optString("severity", null) : null,
                        shelters
                );
                mainHandler.post(() -> callback.onSuccess(advice));
            } catch (Exception e) {
                postShelterAdviceError(callback, "Could not load shelter guidance: " + e.getMessage());
            }
        });
    }

    private void postShelterAdviceError(final ShelterAdviceCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
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

    /**
     * GET /api/warnings — simulated meteorological warnings (DEMO DATA),
     * newest first. Callers filter by location relevance for alerting.
     */
    public void getWarnings(final WarningsCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("GET", baseUrl + "/api/warnings", null);
                JSONObject json = new JSONObject(response);
                JSONArray items = json.getJSONArray("warnings");

                List<Warning> warnings = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    warnings.add(parseWarning(items.getJSONObject(i)));
                }
                mainHandler.post(() -> callback.onSuccess(warnings));
            } catch (Exception e) {
                postWarningError(callback, "Could not load warnings: " + e.getMessage());
            }
        });
    }

    private Warning parseWarning(JSONObject w) throws Exception {
        JSONObject instr = w.optJSONObject("instructions");
        return new Warning(
                w.getString("_id"),
                w.optString("source", "Simulated source"),
                w.getString("type"),
                w.getString("severity"),
                w.getString("title"),
                w.optString("description", ""),
                w.optString("regionName", ""),
                w.getDouble("latitude"),
                w.getDouble("longitude"),
                w.optDouble("radiusKm", 25),
                isoToMillis(w.optString("issuedAt", "")),
                isoToMillis(w.optString("expectedStartAt", "")),
                isoToMillis(w.optString("expectedEndAt", "")),
                stringList(instr, "immediate"),
                stringList(instr, "avoid"),
                stringList(instr, "prepare"),
                w.optString("status", "ACTIVE"),
                w.optBoolean("active", true)
        );
    }

    /** The backend serializes dates as ISO-8601 strings; optLong would silently yield 0. */
    private static long isoToMillis(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        try {
            return java.time.Instant.parse(raw).toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static List<String> stringList(JSONObject parent, String key) {
        List<String> out = new ArrayList<>();
        JSONArray arr = parent != null ? parent.optJSONArray(key) : null;
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String line = arr.optString(i, "").trim();
                if (line.length() > 0) out.add(line);
            }
        }
        return out;
    }

    private void postWarningError(final WarningsCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }

    /** GET /api/safecircle/:deviceId — circle members with live safety status. */
    public void getSafeCircle(String ownerDeviceId, final CircleCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("GET",
                        baseUrl + "/api/safecircle/" + Uri.encode(ownerDeviceId), null);
                JSONObject json = new JSONObject(response);
                JSONArray items = json.getJSONArray("members");

                List<CircleMember> members = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject m = items.getJSONObject(i);
                    members.add(new CircleMember(
                            m.getString("memberDeviceId"),
                            m.optString("memberName", null),
                            m.optString("lastSeenAt", null),
                            m.has("latitude") && !m.isNull("latitude")
                                    ? m.getDouble("latitude") : null,
                            m.has("longitude") && !m.isNull("longitude")
                                    ? m.getDouble("longitude") : null,
                            parseSafetyStatus(m.optString("safetyStatus", "UNKNOWN"))
                    ));
                }
                mainHandler.post(() -> callback.onSuccess(members));
            } catch (Exception e) {
                postCircleError(callback, "Could not load Safe Circle: " + e.getMessage());
            }
        });
    }

    private static CircleMember.SafetyStatus parseSafetyStatus(String raw) {
        switch (raw) {
            case "SAFE": return CircleMember.SafetyStatus.SAFE;
            case "POSSIBLE_EMERGENCY": return CircleMember.SafetyStatus.POSSIBLE_EMERGENCY;
            default: return CircleMember.SafetyStatus.UNKNOWN;
        }
    }

    /** POST /api/safecircle/:owner/members — add a friend to the circle. */
    public void addCircleMember(String ownerDeviceId, String memberDeviceId,
                                String memberName, ApiCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("memberDeviceId", memberDeviceId);
                if (memberName != null && memberName.trim().length() > 0) {
                    body.put("memberName", memberName.trim());
                }
                String response = request("POST",
                        baseUrl + "/api/safecircle/" + Uri.encode(ownerDeviceId) + "/members",
                        body.toString());
                postSuccess(callback, response);
            } catch (Exception e) {
                postError(callback, "Add failed: " + e.getMessage());
            }
        });
    }

    /** DELETE /api/safecircle/:owner/members/:member — remove a friend. */
    public void removeCircleMember(String ownerDeviceId, String memberDeviceId,
                                   ApiCallback callback) {
        executor.execute(() -> {
            try {
                String response = request("DELETE",
                        baseUrl + "/api/safecircle/" + Uri.encode(ownerDeviceId)
                                + "/members/" + Uri.encode(memberDeviceId), null);
                postSuccess(callback, response);
            } catch (Exception e) {
                postError(callback, "Remove failed: " + e.getMessage());
            }
        });
    }

    private void postCircleError(final CircleCallback callback, final String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
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
            if (apiKey != null) {
                connection.setRequestProperty("X-API-Key", apiKey);
            }

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
            if (stream == null) {
                throw new Exception("HTTP " + code + " with no response body");
            }

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
