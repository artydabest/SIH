package com.sih.trial2;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Device-local settings (SharedPreferences): display name + backend URL.
 * Lets each teammate point their app at the shared backend without rebuilds.
 */
public class AppSettings {

    private static final String PREFS = "app_settings";
    private static final String KEY_NAME = "display_name";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_SAFE = "safe_flag";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public void setName(String name) {
        prefs.edit().putString(KEY_NAME, name == null ? "" : name.trim()).apply();
    }

    /**
     * Default points at the adb-reverse tunnel (emulator) — run:
     *   adb reverse tcp:3000 tcp:3000
     * Overridable per device in the in-app Settings (gear button).
     */
    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, "http://127.0.0.1:3000");
    }

    public void setBaseUrl(String url) {
        String value = url == null ? "" : url.trim();
        if (value.length() > 0 && !value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        prefs.edit().putString(KEY_BASE_URL, value).apply();
    }

    /** Last declared safety status; defaults to true ("I am safe"). */
    public boolean isSafe() {
        return prefs.getBoolean(KEY_SAFE, true);
    }

    public void setSafe(boolean safe) {
        prefs.edit().putBoolean(KEY_SAFE, safe).apply();
    }
}
