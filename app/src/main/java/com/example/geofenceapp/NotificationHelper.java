package com.example.geofenceapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "geofence_channel";
    private static final String CHANNEL_NAME = "Geofence Notifications";
    private static final int NOTIFICATION_ID_ENTER = 1001;
    private static final int NOTIFICATION_ID_EXIT = 1002;


    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android Oreo and above
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            // Configure the notification channel
            channel.setDescription("Notifications for geofence transitions");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            // Set the channel to play sound
            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/alert_sound");

            try {
                // Try to get the custom sound resource ID
                int soundResourceId = context.getResources().getIdentifier("alert_sound", "raw", context.getPackageName());

                // Set the sound for the notification channel
                if (soundResourceId != 0) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build();
                    channel.setSound(soundUri, audioAttributes);
                    Log.d(TAG, "Set custom alert sound for notification channel");
                } else {
                    // Set default sound if custom sound is not found
                    channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null);
                    Log.d(TAG, "Set default sound for notification channel");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting sound for notification channel: " + e.getMessage());
                // Use default sound in case of error
                channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null);
            }

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    public void showGeofenceExitNotification(String message) {
        showGeofenceNotification("Geofence Exit Alert", message, NOTIFICATION_ID_EXIT);
    }

    public void showGeofenceEnterNotification(String message) {
        showGeofenceNotification("Geofence Enter Alert", message, NOTIFICATION_ID_ENTER);
    }

    private void showGeofenceNotification(String title, String message, int notificationId) {
        // Create an intent to open the app when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int pendingIntentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, pendingIntentFlags);

        // Get the sound URI
        Uri soundUri = null;
        try {
            int soundResourceId = context.getResources().getIdentifier("alert_sound", "raw", context.getPackageName());
            if (soundResourceId != 0) {
                soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/alert_sound");
                Log.d(TAG, "Using custom alert sound for notification");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting sound URI: " + e.getMessage());
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000});

        // Set sound if we have a custom one, otherwise use default
        if (soundUri != null) {
            builder.setSound(soundUri);
        } else {
            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
        }

        // Show the notification
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification posted successfully with ID: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
        }
    }
}