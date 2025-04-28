package com.example.geofenceapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String GEOFENCE_ID = "CUSTOM_GEOFENCE";

    private GoogleMap mMap;
    private SeekBar radiusSeekBar;
    private TextView radiusText;
    private Button btnAddGeofence;
    private Button btnRemoveGeofence;

    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private FusedLocationProviderClient fusedLocationClient;

    private float geofenceRadius = 100;
    private LatLng selectedLocation;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> geofenceList = new ArrayList<>();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize UI components
        radiusSeekBar = findViewById(R.id.radius_seekbar);
        radiusText = findViewById(R.id.radius_text);
        btnAddGeofence = findViewById(R.id.btn_add_geofence);
        btnRemoveGeofence = findViewById(R.id.btn_remove_geofence);

        // Initialize Geofencing client and helper
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up the radius seekbar
        radiusSeekBar.setMax(500); // Max radius 500m
        radiusSeekBar.setProgress((int) geofenceRadius);
        radiusText.setText(getString(R.string.radius_value, geofenceRadius));

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                geofenceRadius = progress;
                if (geofenceRadius < 5) geofenceRadius = 5; // Minimum radius is 5
                // m
                radiusText.setText(getString(R.string.radius_value, geofenceRadius));
                // Only update geofence circle if map is ready
                if (mMap != null && selectedLocation != null) {
                    updateGeofenceCircle();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Set click listeners for buttons
        btnAddGeofence.setOnClickListener(v -> addGeofence());
        btnRemoveGeofence.setOnClickListener(v -> removeGeofence());

        // Load saved settings
        loadGeofenceSettings();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set map type to normal
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
            zoomToUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Set long click listener for map
        mMap.setOnMapLongClickListener(this);

        // Check if we have saved location and draw the geofence
        if (selectedLocation != null) {
            drawMarkerWithCircle(selectedLocation);
        }
        // If we don't have a selected location yet, load from settings
        else {
            // Re-apply settings now that the map is ready
            loadGeofenceSettings();

            // Try to draw the geofence again if settings were loaded
            if (selectedLocation != null) {
                drawMarkerWithCircle(selectedLocation);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                if (mMap != null) {
                    enableUserLocation();
                    zoomToUserLocation();
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission is required to use the map", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        selectedLocation = latLng;
        drawMarkerWithCircle(latLng);
    }

    private void drawMarkerWithCircle(LatLng position) {
        // Make sure map is initialized
        if (mMap == null) {
            return;
        }

        try {
            // Clear previous markers
            mMap.clear();

            // Add marker for the selected location
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Geofence Center"));

            // Add circle to represent the geofence radius
            mMap.addCircle(new CircleOptions()
                    .center(position)
                    .radius(geofenceRadius)
                    .strokeColor(Color.RED)
                    .fillColor(Color.parseColor("#220000FF"))
                    .strokeWidth(5));

            // Move camera to the selected location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        } catch (Exception e) {
            // Log any errors that might occur
            Log.e("MapsActivity", "Error drawing marker with circle: " + e.getMessage());
        }
    }

    private void updateGeofenceCircle() {
        if (mMap != null && selectedLocation != null) {
            drawMarkerWithCircle(selectedLocation);
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                        // If no location is selected yet, use current location
                        if (selectedLocation == null) {
                            selectedLocation = userLocation;
                            drawMarkerWithCircle(userLocation);
                        }
                    }
                });
    }

    private void addGeofence() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Background location permission is required for geofence monitoring", Toast.LENGTH_LONG).show();
            return;
        }

        // Create geofence - register for both ENTER and EXIT transitions
        Geofence geofence = geofenceHelper.getGeofence(
                GEOFENCE_ID,
                selectedLocation,
                geofenceRadius,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        );

        // Create geofencing request
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);

        // Get pending intent
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        // Add geofence
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MapsActivity.this, "Geofence added successfully", Toast.LENGTH_SHORT).show();

                    // Save geofence settings
                    saveGeofenceSettings();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = geofenceHelper.getErrorString(e);
                    Toast.makeText(MapsActivity.this, "Failed to add geofence: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
    }

    private void removeGeofence() {
        // Create an array of geofence IDs to remove
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(GEOFENCE_ID);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MapsActivity.this, "Geofence removed successfully", Toast.LENGTH_SHORT).show();

                    // Clear the map if it's initialized
                    if (mMap != null) {
                        mMap.clear();
                    }

                    // Reset selectedLocation
                    selectedLocation = null;

                    // Clear saved geofence settings
                    clearGeofenceSettings();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MapsActivity.this, "Failed to remove geofence", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveGeofenceSettings() {
        if (selectedLocation != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat(GeofenceSettings.KEY_LATITUDE, (float) selectedLocation.latitude);
            editor.putFloat(GeofenceSettings.KEY_LONGITUDE, (float) selectedLocation.longitude);
            editor.putFloat(GeofenceSettings.KEY_RADIUS, geofenceRadius);
            editor.putBoolean(GeofenceSettings.KEY_GEOFENCE_ACTIVE, true);
            editor.apply();
        }
    }

    private void loadGeofenceSettings() {
        float latitude = sharedPreferences.getFloat(GeofenceSettings.KEY_LATITUDE, 0);
        float longitude = sharedPreferences.getFloat(GeofenceSettings.KEY_LONGITUDE, 0);
        geofenceRadius = sharedPreferences.getFloat(GeofenceSettings.KEY_RADIUS, 100);
        boolean isGeofenceActive = sharedPreferences.getBoolean(GeofenceSettings.KEY_GEOFENCE_ACTIVE, false);

        if (isGeofenceActive && latitude != 0 && longitude != 0) {
            selectedLocation = new LatLng(latitude, longitude);
            radiusSeekBar.setProgress((int) geofenceRadius);
            radiusText.setText(getString(R.string.radius_value, geofenceRadius));
        }
    }

    private void clearGeofenceSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GeofenceSettings.KEY_GEOFENCE_ACTIVE, false);
        editor.apply();
    }
}
