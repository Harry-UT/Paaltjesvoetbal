package com.example.paaltjesvoetbal;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the app fullscreen (hides status and navigation bars)
        this.getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Get display metrics to determine screen size and density
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int screenX = metrics.widthPixels;
        int screenY = metrics.heightPixels;

        float xInches = screenX / metrics.xdpi;
        float yInches = screenY / metrics.ydpi;

        float diagonalPx = (float)Math.sqrt(screenX*screenX + screenY*screenY);
        float diagonalIn = (float)Math.sqrt(xInches*xInches + yInches*yInches);

        float dpi = diagonalPx / diagonalIn;

        int navBarHeight = 0;
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            navBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        int drawableX = getResources().getDisplayMetrics().widthPixels;
        int drawableY = getResources().getDisplayMetrics().heightPixels + navBarHeight;


        // Initialize the GameView with the Activity context
        gameView = new GameView(this, drawableX, drawableY, (int) dpi);
        gameView.setOnApplyWindowInsetsListener((v, insets) -> {
            insets.consumeSystemWindowInsets();
            return insets;
        });

        // Set the GameView as the content view
        setContentView(gameView);
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }


//    @Override
//    public void onSettingsChanged(int playerCount, int playerSpeed, int ballSpeed, boolean online) {
//        gameView.changeSettings(playerCount, playerSpeed, ballSpeed, online);
//    }
}