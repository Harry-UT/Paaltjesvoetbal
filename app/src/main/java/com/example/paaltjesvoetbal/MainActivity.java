package com.example.paaltjesvoetbal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements SettingsDialog.OnSettingsChangedListener {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the app fullscreen (hides status and navigation bars)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Get screen resolution
        int screenX = getResources().getDisplayMetrics().widthPixels;
        int screenY = getResources().getDisplayMetrics().heightPixels;

        // Initialize the GameView with the Activity context
        gameView = new GameView(this, screenX, screenY);

        // Set the GameView as the content view
        setContentView(gameView);
    }

    @Override
    public void onSettingsChanged(int playerCount) {
        // Handle the updated player count (or any other settings you want to change)
        updateGameSettings(playerCount);
    }

    private void updateGameSettings(int playerCount) {
        // Update the game based on the selected player count
        gameView.changePlayerCount(playerCount);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause(); // Pause the game to save resources
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume(); // Resume the game
        }
    }
}