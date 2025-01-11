package com.example.paaltjesvoetbal;

import android.app.Dialog;
import android.content.Context;
import android.widget.SeekBar;

public class SettingsDialog extends Dialog {

    private OnSettingsChangedListener listener;

    // Callback interface
    public interface OnSettingsChangedListener {
        void onSettingsChanged(int playerCount);  // Notify MainActivity of the player count
    }

    // Constructor with listener
    public SettingsDialog(Context context, OnSettingsChangedListener listener) {
        super(context);
        this.listener = listener;  // Set the listener
        setContentView(R.layout.settings_dialog); // Set the layout for the settings dialog

        // Get references to the SeekBar and EditText elements
        SeekBar playerCountSeekBar = findViewById(R.id.playerCount);

        // Set a listener to handle the SeekBar changes
        playerCountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Pass the selected player count back to the listener
                if (listener != null) {
                    listener.onSettingsChanged(progress + 2); // Map 0 -> 2 players, 1 -> 3 players, 2 -> 4 players
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}