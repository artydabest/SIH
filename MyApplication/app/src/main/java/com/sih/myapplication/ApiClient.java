package com.sih.myapplication;

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
import java.util.Collections;
import java.util.List;

/**
 * Minimal synchronous HTTP + JSON client for the disaster-relay backend.
 * All calls block on network I/O — run them on a background thread.
 *
 * Endpoints (see disaster-relay/src/routes/emergency.ts):
 *   GET   /api/emergency                     -> { success, emergencies: [...] }
 *   POST  /api/emergency                     -> { success, emergency }
 *   PATCH /api/emergency/:id/status {status} -> { success, emergency }
 */
public final class ApiClient {

    private static final String TAG = "ApiClient";
    /**
     * 10.0.2.2 is the host machine's loopback from the Android emulator.
     * For a physical device on the same Wi-Fi, replace with your machine's
     * LAN IP (e.g. http://192.168.1.20:3000/api/emergency).
     */
    public static final String BASE_URL = "http://10.0.2.2:3000/api/emergency";
    private static final int TIMEOUT_MS = 8000;

    private ApiClient() {}

    /** Model object for one emergency, parsed leniently from the backend JSON. */
    public static class Emergency {
        public String id = "";
        public String deviceId = "";
        public double latitude;
        public double longitude;
        public String status = "NEW";
        public int nearbyDevices;
        public int stationaryMinutes;
        public long createdAtMs;

        static Emergency fromJson(JSONObject o) {
            Emergency e = new Emergency();
            if (o == null) return e;
            e.id = o.optString("_id", "");
            e.deviceId = o.optString("deviceId", "");
            e.latitude = o.optDouble("latitude", 0);
            e.longitude = o.optDouble("longitude", 0);
            e.status = o.optString("status", "NEW");
            e.nearbyDevices = o.optInt("nearbyDevices", 0);
            e.stationaryMinutes = o.optInt("stationaryMinutes", 0);
            String created = o.optString("createdAt", "");
            if (!created.isEmpty()) {
                try {
                    // ISO-8601 e.g. 2026-09-11T12:34:56.789Z
                    String clean = created.replace("Z", "");
                    int dot = clean.indexOf('.');
                    if (dot > 0) clean = clean.substring(0, dot);
                    java.text.SimpleDateFormat sdf =
                            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date parsed = sdf.parse(clean);
                    if (parsed != null) e.createdAtMs = parsed.getTime();
                } catch (Exception parseFailure) {
                    e.createdAtMs = System.currentTimeMillis();
                }
            }
            return e;
        }
    }

    /** GET /api/emergency — newest first; empty list on failure. */
    public static List<Emergency> getEmergencies() {
        try {
            HttpURLConnection conn = open("GET", null);
            int code = conn.getResponseCode();
            String body = readBody(conn);
            conn.disconnect();
            if (code != 200) {
                Log.w(TAG, "GET /api/emergency failed: HTTP " + code);
                return Collections.emptyList();
            }
            JSONArray arr = new JSONObject(body).optJSONArray("emergencies");
            List<Emergency> out = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    out.add(Emergency.fromJson(arr.optJSONObject(i)));
                }
            }
            return out;
        } catch (Exception e) {
            Log.w(TAG, "GET /api/emergency error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * POST /api/emergency — report a new emergency from this device.
     * Returns the stored record (with server-assigned id), or null on failure.
     */
    public static Emergency reportEmergency(String deviceId, double latitude,
                                            double longitude, int nearbyDevices,
                                            int stationaryMinutes) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("deviceId", deviceId);
            payload.put("latitude", latitude);
            payload.put("longitude", longitude);
            payload.put("nearbyDevices", nearbyDevices);
            payload.put("stationaryMinutes", stationaryMinutes);

            HttpURLConnection conn = open("POST", payload.toString());
            int code = conn.getResponseCode();
            String body = readBody(conn);
            conn.disconnect();
            if (code != 201 && code != 200) {
                Log.w(TAG, "POST /api/emergency failed: HTTP " + code + " " + body);
                return null;
            }
            return Emergency.fromJson(new JSONObject(body).optJSONObject("emergency"));
        } catch (Exception e) {
            Log.w(TAG, "POST /api/emergency error: " + e.getMessage());
            return null;
        }
    }

    /** PATCH /api/emergency/:id/status — advance NEW→ACKNOWLEDGED→RESPONDING→RESOLVED. */
    public static boolean updateStatus(String id, String status) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("status", status);
            HttpURLConnection conn = open("PATCH", payload.toString());
            int code = conn.getResponseCode();
            readBody(conn);
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            Log.w(TAG, "PATCH status error: " + e.getMessage());
            return false;
        }
    }

    private static HttpURLConnection open(String method, String jsonBody) throws Exception {
        URL url = new URL(BASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        if (jsonBody != null) {
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
        }
        return conn;
    }

    private static String readBody(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception streamError) {
            // Non-2xx: read the error stream for diagnostics, then rethrow code check.
            return "";
        }
    }
}
