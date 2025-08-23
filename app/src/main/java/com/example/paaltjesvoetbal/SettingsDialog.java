package com.example.paaltjesvoetbal;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class SettingsDialog extends Dialog {

    private final GameView listener;
    private final int initialPlayerCount;
    private final int initialPlayerSpeed;
    private final int initialBallSpeed;
    private final int resetPlayerCount = 2;
    private final int resetPlayerSpeed = 4;
    private final int resetBallSpeed = 18;
    private boolean online;
    private boolean twovTwoMode;

    // Constructor with listener and initial settings
    public SettingsDialog(Context context, GameView listener, int playerCount, int playerSpeed, int ballSpeed, boolean online, boolean twoVTwo) {
        super(context);
        this.listener = listener;
        this.initialPlayerCount = playerCount;
        this.initialPlayerSpeed = playerSpeed;
        this.initialBallSpeed = ballSpeed;
        this.online = online;
        this.twovTwoMode = twoVTwo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_dialog);
        Log.d("SettingsDialog", "Dialog created");

        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(R.drawable.rounded_dialog_background);

        SeekBar playerCountSeekBar = findViewById(R.id.playerCount);
        SeekBar playerSpeedSeekBar = findViewById(R.id.playerSpeed);
        SeekBar ballSpeedSeekBar = findViewById(R.id.ballSpeed);
        TextView playerSpeedText = findViewById(R.id.currentPlayerSpeed);
        TextView ballSpeedText = findViewById(R.id.currentBallSpeed);
        TextView playerCountText = findViewById(R.id.currentPlayerCount);
        Button resetButton = findViewById(R.id.resetButton);
        CheckBox twovtwomodeSwitch = findViewById(R.id.twoVtwoMode);
        TextInputEditText usernameInput = findViewById(R.id.usernameInput);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch onlineSwitch = findViewById(R.id.onlineSwitch);

        playerCountSeekBar.setProgress(initialPlayerCount);
        playerSpeedSeekBar.setProgress(initialPlayerSpeed);
        ballSpeedSeekBar.setProgress(initialBallSpeed);
        playerSpeedText.setText(String.valueOf(initialPlayerSpeed));
        ballSpeedText.setText(String.valueOf(initialBallSpeed));
        playerCountText.setText(String.valueOf(initialPlayerCount));
        onlineSwitch.setChecked(online);
        twovtwomodeSwitch.setChecked(twovTwoMode);

        Log.d("SettingsDialog", "Initial settings applied: playerCount=" + initialPlayerCount +
                ", playerSpeed=" + initialPlayerSpeed + ", ballSpeed=" + initialBallSpeed +
                ", online=" + online);

        onlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            online = isChecked;
            Log.d("SettingsDialog", "Online switch toggled: " + isChecked);
            notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress(), isChecked, twovtwomodeSwitch.isChecked(), false);
        });

        ballSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int ballSpeed, boolean fromUser) {
                ballSpeedText.setText(String.valueOf(ballSpeed));
                Log.d("SettingsDialog", "Ball speed changed: " + ballSpeed);
                notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeedSeekBar.getProgress(), ballSpeed, online, twovtwomodeSwitch.isChecked(), false);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("SettingsDialog", "Started changing ball speed");
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("SettingsDialog", "Stopped changing ball speed");
            }
        });

        playerCountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int playerCount, boolean fromUser) {
                playerCountText.setText(String.valueOf(playerCount));
                Log.d("SettingsDialog", "Player count changed: " + playerCount);
                notifySettingsChanged(playerCount, playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress(), online, twovtwomodeSwitch.isChecked(), false);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("SettingsDialog", "Started changing player count");
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("SettingsDialog", "Stopped changing player count");
            }
        });

        playerSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int playerSpeed, boolean fromUser) {
                playerSpeedText.setText(String.valueOf(playerSpeed));
                Log.d("SettingsDialog", "Player speed changed: " + playerSpeed);
                notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeed, ballSpeedSeekBar.getProgress(), online, twovtwomodeSwitch.isChecked(), false);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("SettingsDialog", "Started changing player speed");
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("SettingsDialog", "Stopped changing player speed");
            }
        });

        resetButton.setOnClickListener(v -> {
            Log.d("SettingsDialog", "Reset button clicked");
            playerCountSeekBar.setProgress(resetPlayerCount);
            playerSpeedSeekBar.setProgress(resetPlayerSpeed);
            ballSpeedSeekBar.setProgress(resetBallSpeed);
            playerSpeedText.setText(String.valueOf(resetPlayerSpeed));
            ballSpeedText.setText(String.valueOf(resetBallSpeed));
            playerCountText.setText(String.valueOf(resetPlayerCount));
            onlineSwitch.setChecked(false);
            online = false;
            twovtwomodeSwitch.setChecked(false);
            twovTwoMode = false;
            Log.d("SettingsDialog", "Settings reset to default");
            notifySettingsChanged(resetPlayerCount, resetPlayerSpeed, resetBallSpeed, online, twovTwoMode, true);
        });

        twovtwomodeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundManager.getInstance(getContext()).playTwovTwoTick();
            Log.d("SettingsDialog", "Two vs Two mode toggled: " + isChecked);
            notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress(), online, twovtwomodeSwitch.isChecked(), true);
        });
    }

    private void notifySettingsChanged(int playerCount, int playerSpeed, int ballSpeed, boolean online, boolean twoVTwo, boolean isReset) {
        if (listener != null) {
            Log.d("SettingsDialog", "Notifying settings changed: playerCount=" + playerCount +
                    ", playerSpeed=" + playerSpeed + ", ballSpeed=" + ballSpeed +
                    ", online=" + online);
            listener.changeSettings(playerCount, playerSpeed, ballSpeed, online, twoVTwo, isReset);
        }
    }

    @Override
    public void show() {
        super.show();
        Log.d("SettingsDialog", "Dialog shown");

        View contentView = findViewById(android.R.id.content);
        contentView.setVisibility(View.INVISIBLE);

        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0f, 1f, 0f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(300);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d("SettingsDialog", "Animation start");
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                contentView.setVisibility(View.VISIBLE);
                Log.d("SettingsDialog", "Animation end");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        contentView.startAnimation(scaleAnimation);
    }
}