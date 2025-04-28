package com.example.geofenceapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceBroadcast";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() called");

        if (intent == null) {
            Log.e(TAG, "Intent is null in GeofenceBroadcastReceiver");
            return;
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing Event is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofencing Error: " + errorMessage);
            return;
        }

        // Get and log the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.d(TAG, "Geofence transition type: " + getTransitionString(geofenceTransition));

        // Check if the transition type is EXIT or ENTER
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered
            List<Geofence> triggeredGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeredGeofences != null && !triggeredGeofences.isEmpty()) {
                // Extract details about the triggered geofences
                String geofenceId = triggeredGeofences.get(0).getRequestId();

                // Get the transition type (ENTER or EXIT)
                String transitionType = getTransitionString(geofenceTransition);

                // Log the event
                Log.i(TAG, "Geofence event detected: " + transitionType + " for " + geofenceId);

                // Get the location that triggered the geofence
                Location triggeringLocation = geofencingEvent.getTriggeringLocation();
                String locationInfo = "";

                if (triggeringLocation != null) {
                    locationInfo = " at location " +
                            triggeringLocation.getLatitude() + ", " +
                            triggeringLocation.getLongitude();
                }

                // Check if notifications are enabled in settings
                if (GeofenceSettings.isNotificationEnabled(context)) {
                    // Create a notification message based on transition type
                    String notificationMessage;
                    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        notificationMessage = "You have exited the geofence area" + locationInfo;
                    } else {
                        notificationMessage = "You have entered the geofence area" + locationInfo;
                    }

                    // Create and show notification
                    NotificationHelper notificationHelper = new NotificationHelper(context);

                    // Use the appropriate notification method based on transition type
                    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        notificationHelper.showGeofenceExitNotification(notificationMessage);
                    } else {
                        notificationHelper.showGeofenceEnterNotification(notificationMessage);
                    }
                }

                // Check if sound alerts are enabled
                if (GeofenceSettings.isSoundEnabled(context)) {
                    // Play alert sound
                    playAlertSound(context);
                }

                // Check if vibration is enabled
                if (GeofenceSettings.isVibrationEnabled(context)) {
                    // Vibrate the device
                    vibrate(context);
                }
            }
        }
    }

    private void playAlertSound(Context context) {
        try {
            // Try playing the sound from raw resources first
            int soundResourceId = context.getResources().getIdentifier("alert_sound", "raw", context.getPackageName());

            MediaPlayer mediaPlayer;
            if (soundResourceId != 0) {
                // Use the sound from raw resources
                mediaPlayer = MediaPlayer.create(context, soundResourceId);
                Log.d(TAG, "Using custom alert sound from resources");
            } else {
                // Fallback to system notification sound
                mediaPlayer = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
                Log.d(TAG, "Using system default notification sound");
            }

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
                Log.d(TAG, "Alert sound playing successfully");
            } else {
                Log.e(TAG, "Failed to create MediaPlayer for alert sound");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alert sound: " + e.getMessage());

            // Fallback method for playing sound
            try {
                MediaPlayer fallbackPlayer = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
                if (fallbackPlayer != null) {
                    fallbackPlayer.setOnCompletionListener(MediaPlayer::release);
                    fallbackPlayer.start();
                    Log.d(TAG, "Fallback alert sound playing successfully");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Fallback sound also failed: " + ex.getMessage());
            }
        }
    }

    private void vibrate(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Vibrate pattern: wait 0ms, vibrate 1000ms, pause 500ms, vibrate 1000ms
                long[] pattern = {0, 1000, 500, 1000};

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    // For older devices
                    vibrator.vibrate(pattern, -1);
                }
                Log.d(TAG, "Device vibration triggered");
            } else {
                Log.d(TAG, "Device does not support vibration");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering vibration: " + e.getMessage());
        }
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "GEOFENCE_TRANSITION_ENTER";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "GEOFENCE_TRANSITION_EXIT";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "GEOFENCE_TRANSITION_DWELL";
            default:
                return "UNKNOWN_TRANSITION_TYPE: " + transitionType;
        }
    }
}

class GeofenceStatusCodes {
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case 1:
                return "GEOFENCE_NOT_AVAILABLE";
            case 2:
                return "GEOFENCE_TOO_MANY_GEOFENCES";
            case 3:
                return "GEOFENCE_TOO_MANY_PENDING_INTENTS";
            default:
                return "UNKNOWN_ERROR_CODE: " + statusCode;
        }
    }
}