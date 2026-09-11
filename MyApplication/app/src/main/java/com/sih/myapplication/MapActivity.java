package com.sih.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Rescue Map — WebView wrapper around assets/rescue_map.html.
 *
 * Every 7 seconds it fetches the live incident list from the same
 * disaster-relay backend the responder dashboard uses, then pushes it into
 * the map via the bridge the HTML already exposes:
 *   window.loadEmergencies(jsonArray, selectedId?)
 */
public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final long POLL_INTERVAL_MS = 7000; // matches dashboard polling

    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private boolean pageReady = false;
    private boolean pollRunning = false;
    private String lastJson = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                // Push whatever we already fetched while the page was loading.
                if (lastJson != null) {
                    pushToMap(lastJson, null);
                }
                startPolling();
            }
        });

        webView.loadUrl("file:///android_asset/rescue_map.html");
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
            final String json = toJson(list);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (json != null && !json.equals(lastJson)) {
                    lastJson = json;
                    if (pageReady) pushToMap(json, null);
                }
                if (!list.isEmpty()) {
                    mainHandler.postDelayed(this::fetchOnce, POLL_INTERVAL_MS);
                } else {
                    // Backend unreachable or empty — retry on a slower cadence.
                    mainHandler.postDelayed(this::fetchOnce, POLL_INTERVAL_MS * 2);
                }
            });
        });
    }

    /** Serialize to the same JSON shape rescue_map.html expects (raw backend objects). */
    private String toJson(List<ApiClient.Emergency> list) {
        if (list == null) return null;
        try {
            JSONArray arr = new JSONArray();
            for (ApiClient.Emergency e : list) {
                // Keep the exact backend field names — the map reads _id, deviceId,
                // latitude, longitude, status, confidence, confidenceLevel, createdAt.
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("_id", e.id);
                o.put("deviceId", e.deviceId);
                o.put("latitude", e.latitude);
                o.put("longitude", e.longitude);
                o.put("status", e.status);
                o.put("nearbyDevices", e.nearbyDevices);
                o.put("stationaryMinutes", e.stationaryMinutes);
                if (e.createdAtMs > 0) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    o.put("createdAt", sdf.format(new java.util.Date(e.createdAtMs)));
                }
                arr.put(o);
            }
            return arr.toString();
        } catch (Exception e) {
            Log.w(TAG, "JSON encode failed: " + e.getMessage());
            return null;
        }
    }

    private void pushToMap(String json, String selectedId) {
        if (webView == null) return;
        String script = selectedId == null
                ? String.format("window.loadEmergencies(%s)", json)
                : String.format("window.loadEmergencies(%s, \"%s\")", json, selectedId);
        webView.evaluateJavascript(script, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
        networkExecutor.shutdownNow();
        if (webView != null) {
            webView.destroy();
        }
    }
}
