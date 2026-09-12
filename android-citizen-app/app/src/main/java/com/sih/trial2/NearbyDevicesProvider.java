package com.sih.trial2;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Estimates how many other devices are physically near the user, using the
 * last Wi-Fi scan results as a proxy signal for the confidence engine.
 *
 * Honest scope (mirrors PRODUCT_AUDIT.md): this is NOT a BLE mesh and it
 * cannot see phones with Wi-Fi disabled. We count distinct access points
 * whose BSSID looks like a client device (locally administered MAC — typical
 * of phone hotspots and randomized SoftAPs). A coarse evidence signal, not a
 * people counter.
 */
public final class NearbyDevicesProvider {

    private NearbyDevicesProvider() {
    }

    /** @return distinct nearby hotspot-style APs (0 when none or scan data unavailable). */
    public static int getNearbyDeviceCount(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return 0;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            return 0;
        }
        Set<String> deviceBssids = new HashSet<>();
        for (ScanResult result : results) {
            if (isLocallyAdministered(result.BSSID)) {
                deviceBssids.add(result.BSSID);
            }
        }
        return deviceBssids.size();
    }

    /**
     * Locally administered MAC: the U/L bit of the first octet is set, i.e.
     * the second hex digit is 2, 6, A, or E. Phone hotspots and privacy
     * randomization use this range; infrastructure routers mostly don't.
     */
    private static boolean isLocallyAdministered(String bssid) {
        if (bssid == null || bssid.length() < 2) {
            return false;
        }
        char nibble = Character.toUpperCase(bssid.charAt(1));
        return nibble == '2' || nibble == '6' || nibble == 'A' || nibble == 'E';
    }
}
