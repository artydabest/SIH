package com.sih.trial2;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Disaster alert UX: an in-app dialog with safety instructions + a system
 * notification so the user is alerted even with the app in background.
 */
public final class AlertNotifier {

    private static final String CHANNEL_ID = "disaster_alerts";
    private static final int NOTIFICATION_ID = 4242;

    private AlertNotifier() {
    }

    /** Must be called on the main thread. */
    public static void showAlertDialog(Activity activity, DisasterAlert alert) {
        StringBuilder text = new StringBuilder(alert.message);
        if (alert.instructions != null && !alert.instructions.isEmpty()) {
            text.append("\\n\\n");
            for (String line : alert.instructions) {
                text.append("• ").append(line).append("\\n");
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(alert.type + " ALERT")
                .setMessage(text.toString())
                .setPositiveButton("I understand", null)
                .setOnDismissListener(dialog -> { /* stays dismissible */ })
                .show();
    }

    /** System notification — works when the app is in the background. */
    public static void showNotification(Context context, DisasterAlert alert) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Disaster alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Emergency disaster notifications");
            manager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // permission not granted yet; the dialog still shows in-app
        }

        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String firstInstruction = alert.instructions != null && !alert.instructions.isEmpty()
                ? alert.instructions.get(0)
                : "Open the app for safety instructions";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(alert.type + " ALERT")
                .setContentText(firstInstruction)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(alert.message + "\\n\\n" + firstInstruction))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Notification permission denied — in-app dialog still warns the user.
        }
    }

    /** Call from the launcher activity to request notification permission once. */
    public static void requestPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            String permission = Manifest.permission.POST_NOTIFICATIONS;
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{permission}, 9001);
            }
        }
    }
}
