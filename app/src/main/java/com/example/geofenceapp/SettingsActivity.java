package com.example.geofenceapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotification;
    private Switch switchSound;
    private Switch switchVibration;
    private Button btnSaveSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)


        // Initialize UI components
        switchNotification = findViewById(R.id.switch_notification);
        switchSound = findViewById(R.id.switch_sound);
        switchVibration = findViewById(R.id.switch_vibration);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        // Load existing settings
        loadSettings();

        // Set up save button click listener
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        // Get current settings from GeofenceSettings class
        boolean notificationEnabled = GeofenceSettings.isNotificationEnabled(this);
        boolean soundEnabled = GeofenceSettings.isSoundEnabled(this);
        boolean vibrationEnabled = GeofenceSettings.isVibrationEnabled(this);

        // Set switch states
        switchNotification.setChecked(notificationEnabled);
        switchSound.setChecked(soundEnabled);
        switchVibration.setChecked(vibrationEnabled);

        // Disable sound and vibration switches if notifications are disabled
        updateSwitchStates();

        // Add listener to notification switch to update states of other switches
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> updateSwitchStates());
    }

    private void updateSwitchStates() {
        boolean notificationEnabled = switchNotification.isChecked();
        switchSound.setEnabled(notificationEnabled);
        switchVibration.setEnabled(notificationEnabled);

        if (!notificationEnabled) {
            switchSound.setChecked(false);
            switchVibration.setChecked(false);
        }
    }

    private void saveSettings() {
        // Get switch states
        boolean notificationEnabled = switchNotification.isChecked();
        boolean soundEnabled = switchSound.isChecked();
        boolean vibrationEnabled = switchVibration.isChecked();

        // Save settings
        GeofenceSettings.saveNotificationSettings(
                this,
                notificationEnabled,
                soundEnabled,
                vibrationEnabled
        );

        // Show confirmation message
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();

        // Go back to main activity
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
