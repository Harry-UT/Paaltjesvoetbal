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
import android.text.TextWatcher;
import android.text.Editable;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

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
        Button okButton = findViewById(R.id.okButton);
        Button pingButton = findViewById(R.id.pingButton);
        TextView pingText = findViewById(R.id.ping);

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

        pingButton.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    int ping = listener.pingServer(); // run ping in background
                    Log.d("SettingsDialog", "Ping result: " + ping + " ms");
                    pingText.post(() -> pingText.setText(String.valueOf(ping))); // update UI safely
                } catch (InterruptedException e) {
                    Log.e("SettingsDialog", "Ping interrupted", e);
                    pingText.post(() -> pingText.setText("Error")); // update UI safely on error
                }
            }).start();
        });

        onlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            online = isChecked;
            Log.d("SettingsDialog", "Online switch toggled: " + isChecked);
            if (isChecked) {
                playerCountSeekBar.setEnabled(true);
                playerSpeedSeekBar.setEnabled(true);
                ballSpeedSeekBar.setEnabled(true);
                twovtwomodeSwitch.setEnabled(true);
                String username = usernameInput.getText().toString();
                if (username.isEmpty()) {
                    usernameInput.setError("Please enter a username to play online");
                    onlineSwitch.setChecked(false);
                } else {
                    notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress(), isChecked, twovtwomodeSwitch.isChecked(), false);
                }
            } else {
                playerCountSeekBar.setEnabled(false);
                playerSpeedSeekBar.setEnabled(false);
                ballSpeedSeekBar.setEnabled(false);
                twovtwomodeSwitch.setEnabled(false);
            }
        });

        twovtwomodeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                playerCountSeekBar.setProgress(4);
                playerCountSeekBar.setEnabled(false);
            } else {
                playerCountSeekBar.setEnabled(true);
            }
            SoundManager.getInstance(getContext()).playTwovTwoTick();
            Log.d("SettingsDialog", "Two vs Two mode toggled: " + isChecked);
            notifySettingsChanged(playerCountSeekBar.getProgress(), playerSpeedSeekBar.getProgress(), ballSpeedSeekBar.getProgress(), online, twovtwomodeSwitch.isChecked(), true);
        });

        usernameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (usernameInput.getError() != null) {
                    usernameInput.setError(null);
                }
                usernameInput.clearFocus();
                usernameInput.setSelection(0, 0); // Clear text selection

                // Hide keyboard and remove cursor
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(usernameInput.getWindowToken(), 0);
            }
        });

        usernameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {

                usernameInput.clearFocus();
                usernameInput.setSelection(0, 0); // Clear text selection
                return true;
            }
            return false;
        });

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (online) {
                    Toast.makeText(getContext(), "Username will be used after reconnecting", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        okButton.setOnClickListener(v -> {
            usernameInput.clearFocus();
            usernameInput.setSelection(0, 0); // Clear text selection
            // Your existing OK button logic here
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