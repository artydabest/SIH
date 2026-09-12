package com.sih.trial2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationHelper {
    public static final int LOCATION_PERMISSION_REQUEST = 1001;

    private final Activity activity;
    private final FusedLocationProviderClient fusedLocationClient;

    public LocationHelper(Activity activity) {
        this.activity = activity;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST
        );
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission is not granted.");
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        ).addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocationReceived(location);
            } else {
                callback.onLocationError("Could not get a GPS location. Make sure Location is turned on and try again.");
            }
        }).addOnFailureListener(e ->
                callback.onLocationError("Location error: " + e.getMessage())
        );
    }

    public interface LocationCallback {
        void onLocationReceived(@NonNull Location location);
        void onLocationError(@NonNull String message);
    }
}
