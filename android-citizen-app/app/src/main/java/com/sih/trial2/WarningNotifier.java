package com.sih.trial2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * System notifications for simulated meteorological warnings (DEMO DATA).
 * Severity drives urgency: CRITICAL uses HEADS_UP-grade importance, others
 * use the default channel so not every warning looks like a catastrophe.
 */
public final class WarningNotifier {

    private static final String CHANNEL_CRITICAL = "warning_critical";
    private static final String CHANNEL_DEFAULT = "warning_default";
    private static final int NOTIFICATION_ID_BASE = 5100;

    private WarningNotifier() {
    }

    public static void showNotification(Context context, Warning warning) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        boolean critical = "CRITICAL".equals(warning.severity);
        String channelId = critical ? CHANNEL_CRITICAL : CHANNEL_DEFAULT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    critical ? "Critical warnings" : "Weather warnings",
                    critical ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Simulated meteorological warnings (demo)");
            manager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // permission not granted yet; in-app banner still shows
        }

        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                context, warning.id.hashCode(), openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String firstAction = !warning.immediateActions.isEmpty()
                ? warning.immediateActions.get(0)
                : "Open the app for safety instructions";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(warning.title)
                .setContentText(firstAction)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(warning.description + "\n\n" + firstAction))
                .setPriority(critical
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pending)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID_BASE + warning.id.hashCode(), builder.build());
        } catch (SecurityException ignored) {
            // Notification permission denied — in-app banner still shows.
        }
    }
}
