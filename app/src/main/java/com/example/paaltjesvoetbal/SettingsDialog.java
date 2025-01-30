package com.example.paaltjesvoetbal;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Objects;

public class SettingsDialog extends Dialog {

    private final OnSettingsChangedListener listener;
    private final int initialPlayerCount;
    private final int initialPlayerSpeed;
    private final int initialBallSpeed;

    // Mapping functions
    private int mapPlayerSpeed(int progress) {
        return 1 + (progress * 14) / 100; // Maps 0–100 to 1–15
    }

    private int mapBallSpeed(int progress) {
        return 5 + (progress * 25) / 100; // Maps 0–100 to 5–30
    }

    // Callback interface
    public interface OnSettingsChangedListener {
        void onSettingsChanged(int playerCount, int playerSpeed, int ballSpeed);
    }

    // Constructor with listener and initial settings
    public SettingsDialog(Context context, OnSettingsChangedListener listener, int playerCount, int playerSpeed, int ballSpeed) {
        super(context);
        this.listener = listener;
        this.initialPlayerCount = playerCount;
        this.initialPlayerSpeed = playerSpeed;
        this.initialBallSpeed = ballSpeed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_dialog);

        // Apply the rounded corners background
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(R.drawable.rounded_dialog_background);

        // Get references to UI elements
        SeekBar playerCountSeekBar = findViewById(R.id.playerCount);
        SeekBar playerSpeedSeekBar = findViewById(R.id.playerSpeed);
        SeekBar ballSpeedSeekBar = findViewById(R.id.ballSpeed);
        TextView playerSpeedText = findViewById(R.id.currentPlayerSpeed);
        TextView ballSpeedText = findViewById(R.id.currentBallSpeed);
        Button resetButton = findViewById(R.id.resetButton);

        // Initialize SeekBars
        playerCountSeekBar.setProgress(initialPlayerCount - 2);
        playerSpeedSeekBar.setProgress((initialPlayerSpeed - 1) * 100 / 14); // Reverse map to SeekBar range
        ballSpeedSeekBar.setProgress((initialBallSpeed - 5) * 100 / 25); // Reverse map to SeekBar range

        playerSpeedText.setText(String.valueOf(initialPlayerSpeed));
        ballSpeedText.setText(String.valueOf(initialBallSpeed));

        // SeekBar Listeners
        playerCountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                notifySettingsChanged(progress + 2, mapPlayerSpeed(playerSpeedSeekBar.getProgress()), mapBallSpeed(ballSpeedSeekBar.getProgress()));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        playerSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int mappedSpeed = mapPlayerSpeed(progress);
                playerSpeedText.setText(String.valueOf(mappedSpeed));
                notifySettingsChanged(playerCountSeekBar.getProgress() + 2, mappedSpeed, mapBallSpeed(ballSpeedSeekBar.getProgress()));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ballSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int mappedSpeed = mapBallSpeed(progress);
                ballSpeedText.setText(String.valueOf(mappedSpeed));
                notifySettingsChanged(playerCountSeekBar.getProgress() + 2, mapPlayerSpeed(playerSpeedSeekBar.getProgress()), mappedSpeed);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Reset Button Listener
        // Reset Button Listener
        resetButton.setOnClickListener(v -> {
            // Hardcoded values
            int resetPlayerCount = 2;
            int resetPlayerSpeed = 6;
            int resetBallSpeed = 20;

            // Reset SeekBars to match hardcoded values
            playerCountSeekBar.setProgress(0);
            playerSpeedSeekBar.setProgress((resetPlayerSpeed - 1) * 100 / 14);
            ballSpeedSeekBar.setProgress((resetBallSpeed - 5) * 100 / 25);

            // Update TextViews
            playerSpeedText.setText(String.valueOf(resetPlayerSpeed));
            ballSpeedText.setText(String.valueOf(resetBallSpeed));

            // Notify listener with hardcoded values
            notifySettingsChanged(resetPlayerCount, resetPlayerSpeed, resetBallSpeed);
        });
    }

    private void notifySettingsChanged(int playerCount, int playerSpeed, int ballSpeed) {
        if (listener != null) {
            listener.onSettingsChanged(playerCount, playerSpeed, ballSpeed);
        }
    }

    @Override
    public void show() {
        super.show();

        View contentView = findViewById(android.R.id.content);
        contentView.setVisibility(View.INVISIBLE);

        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0f, 1f,
                0f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(300);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                contentView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        contentView.startAnimation(scaleAnimation);
    }
}