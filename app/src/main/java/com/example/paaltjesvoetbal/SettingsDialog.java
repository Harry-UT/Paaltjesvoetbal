package com.example.paaltjesvoetbal;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.SeekBar;

import java.util.Objects;

public class SettingsDialog extends Dialog {

    private OnSettingsChangedListener listener;

    // Callback interface
    public interface OnSettingsChangedListener {
        void onSettingsChanged(int playerCount);  // Notify MainActivity of the player count
    }

    // Constructor with listener and initial player count
    public SettingsDialog(Context context, OnSettingsChangedListener listener, int playerCount) {
        super(context);
        this.listener = listener;  // Set the listener
        setContentView(R.layout.settings_dialog); // Set the layout for the settings dialog

        // Apply the rounded corners background to the dialog window
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(R.drawable.rounded_dialog_background);

        // Get references to the SeekBar element
        SeekBar playerCountSeekBar = findViewById(R.id.playerCount);

        // Initialize the SeekBar progress to reflect the current player count
        playerCountSeekBar.setProgress(playerCount - 2); // Map 2 -> 0 players, 3 -> 1 players, 4 -> 2 players

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

    // Override the show method to apply the scale-up animation
    @Override
    public void show() {
        super.show();

        // Make the content of the dialog initially INVISIBLE
        findViewById(android.R.id.content).setVisibility(View.INVISIBLE);

        // Apply the scale animation to the dialog's content view
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0f, 1f,  // From 0 (invisible) to 1 (full size)
                0f, 1f,  // Scale both X and Y axis
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,  // Pivot point (center of dialog)
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(300);  // Set the duration of the animation (in milliseconds)
        scaleAnimation.setFillAfter(true);  // Maintain the final state of the animation (i.e., full size)

        // Set the content view to VISIBLE after the animation starts
        findViewById(android.R.id.content).setVisibility(View.VISIBLE);

        // Apply the animation to the dialog's content view
        findViewById(android.R.id.content).startAnimation(scaleAnimation);
    }
}