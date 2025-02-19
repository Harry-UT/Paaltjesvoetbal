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
    private final int resetPlayerCount = 2;
    private final int resetPlayerSpeed = 4;
    private final int resetBallSpeed = 18;

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
        TextView playerCountText = findViewById(R.id.currentPlayerCount);
        Button resetButton = findViewById(R.id.resetButton);

        // Initialize SeekBars
        playerCountSeekBar.setProgress(initialPlayerCount);
        playerSpeedSeekBar.setProgress(initialPlayerSpeed);
        ballSpeedSeekBar.setProgress(initialBallSpeed);

        playerSpeedText.setText(String.valueOf(initialPlayerSpeed));
        ballSpeedText.setText(String.valueOf(initialBallSpeed));
        playerCountText.setText(String.valueOf(initialPlayerCount));

        // SeekBar Listeners
        playerCountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int playerCount, boolean fromUser) {
                notifySettingsChanged(playerCount, playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress());
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        playerSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int playerSpeed, boolean fromUser) {
                playerSpeedText.setText(String.valueOf(playerSpeed));
                notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeed, ballSpeedSeekBar.getProgress());
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ballSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int ballSpeed, boolean fromUser) {
                ballSpeedText.setText(String.valueOf(ballSpeed));
                notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeedSeekBar.getProgress(), ballSpeed);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        playerCountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int playerCount, boolean fromUser) {
                playerCountText.setText(String.valueOf(playerCount));
                notifySettingsChanged(playerCount, playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress());
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Reset Button Listener
        // Reset Button Listener
        resetButton.setOnClickListener(v -> {
            // Reset SeekBars to match hardcoded values
            playerCountSeekBar.setProgress(resetPlayerCount);
            playerSpeedSeekBar.setProgress(resetPlayerSpeed);
            ballSpeedSeekBar.setProgress(resetBallSpeed);

            // Update TextViews
            playerSpeedText.setText(String.valueOf(resetPlayerSpeed));
            ballSpeedText.setText(String.valueOf(resetBallSpeed));
            playerCountText.setText(String.valueOf(resetPlayerCount));

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