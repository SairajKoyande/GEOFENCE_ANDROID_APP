package com.example.geofenceapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002;

    private Button btnOpenMap;
    private Button btnSettings;
    private TextView txtGeofenceStatus;
    private TextView txtCurrentSettings;

    private GeofenceHelper geofenceHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        btnOpenMap = findViewById(R.id.btn_open_map);
        btnSettings = findViewById(R.id.btn_settings);
        txtGeofenceStatus = findViewById(R.id.txt_geofence_status);
        txtCurrentSettings = findViewById(R.id.txt_current_settings);

        // Initialize GeofenceHelper
        geofenceHelper = new GeofenceHelper(this);
        
        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set click listeners
        btnOpenMap.setOnClickListener(v -> openMapActivity());
        btnSettings.setOnClickListener(v -> openSettingsActivity());

        // Check location permissions
        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGeofenceStatus();
        updateCurrentSettings();
    }

    private void openMapActivity() {
        if (checkLocationPermission()) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
        }
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Background location permission not needed for Android 9 and below
    }

    private void checkAndRequestPermissions() {
        if (!checkLocationPermission()) {
            requestLocationPermission();
        } else if (!checkBackgroundLocationPermission()) {
            requestBackgroundLocationPermission();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs location permissions to work properly.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_PERMISSION_REQUEST_CODE
                        );
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Background Location Permission Needed")
                        .setMessage("This app needs background location permissions to monitor geofences even when the app is not in use.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                                );
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, now request background location if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundLocationPermission()) {
                    requestBackgroundLocationPermission();
                }
            } else {
                Toast.makeText(this, "Location permission is required for this app", Toast.LENGTH_LONG).show();
                // Show option to open app settings
                showSettingsDialog();
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Background location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Background location permission is required for geofence monitoring", Toast.LENGTH_LONG).show();
                // Show option to open app settings
                showSettingsDialog();
            }
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs location permissions to work properly. Go to settings to grant permissions.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void updateGeofenceStatus() {
        boolean isGeofenceActive = GeofenceSettings.isGeofenceActive(this);
        
        if (isGeofenceActive) {
            txtGeofenceStatus.setText(getString(R.string.geofence_status_active));
            txtGeofenceStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            txtGeofenceStatus.setText(getString(R.string.geofence_status_inactive));
            txtGeofenceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void updateCurrentSettings() {
        double latitude = sharedPreferences.getFloat(GeofenceSettings.KEY_LATITUDE, 0);
        double longitude = sharedPreferences.getFloat(GeofenceSettings.KEY_LONGITUDE, 0);
        float radius = sharedPreferences.getFloat(GeofenceSettings.KEY_RADIUS, 100);
        
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.current_location))
          .append(": ")
          .append(String.format("%.6f, %.6f", latitude, longitude))
          .append("\n")
          .append(getString(R.string.radius))
          .append(": ")
          .append(String.format("%.0f meters", radius));
        
        txtCurrentSettings.setText(sb.toString());
    }
}
