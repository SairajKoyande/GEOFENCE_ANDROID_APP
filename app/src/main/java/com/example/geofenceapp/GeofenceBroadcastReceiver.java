package com.example.geofenceapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceBroadcast";

    @Override
    public void onReceive(Context context, Intent intent) {
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

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Check if the transition type is EXIT
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofences that were triggered
            List<Geofence> triggeredGeofences = geofencingEvent.getTriggeringGeofences();
            
            if (triggeredGeofences != null && !triggeredGeofences.isEmpty()) {
                // Extract details about the triggered geofences
                String geofenceId = triggeredGeofences.get(0).getRequestId();
                
                // Log the event
                Log.i(TAG, "Geofence EXIT event detected: " + geofenceId);
                
                // Get the location that triggered the geofence
                Location triggeringLocation = geofencingEvent.getTriggeringLocation();
                String locationInfo = "";
                
                if (triggeringLocation != null) {
                    locationInfo = " at location " + 
                            triggeringLocation.getLatitude() + ", " + 
                            triggeringLocation.getLongitude();
                }
                
                // Create a notification message
                String notificationMessage = "You have exited the geofence area" + locationInfo;
                
                // Create and show notification
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.showGeofenceExitNotification(notificationMessage);
                
                // Play alert sound
                playAlertSound(context);
                
                // Vibrate the device
                vibrate(context);
            }
        }
    }

    private void playAlertSound(Context context) {
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.alert_sound);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    private void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Vibrate for 1 second
            long[] pattern = {0, 1000, 500, 1000};
            vibrator.vibrate(pattern, -1);
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
