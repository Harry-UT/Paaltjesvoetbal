package com.example.paaltjesvoetbal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean isPlaying;
    private SurfaceHolder holder;
    private Paint paint;
    private int screenX, screenY;
    private Bitmap background;
    private ArrayList<Joystick> joysticks;  // List of joysticks for multiple players
    private ArrayList<Player> players;  // Multiple players, each with its own position
    private ArrayList<Ball> ballList;  // List to hold the balls

    public GameView(Context context, int screenX, int screenY) {
        super(context);
        this.screenX = screenX;
        this.screenY = screenY;
        holder = getHolder();
        paint = new Paint();

        // Initialize the player and joystick list
        players = new ArrayList<>();
        joysticks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            // Initialize the player at a specific position
            Player newPlayer = new Player((float) (screenX / 2), (float) (screenY / 1.1), 50, Color.RED);
            players.add(newPlayer);

            // Initialize the joystick for each player
            Joystick newJoystick = new Joystick(screenX, screenY, newPlayer);
            joysticks.add(newJoystick);
        }

        // Initialize the ball list
        ballList = new ArrayList<>();
        Ball ball = new Ball((float) screenX / 2, (float) screenY / 2, 30, Color.BLUE);
        ballList.add(ball);

        // Load and scale the background image
        background = BitmapFactory.decodeResource(context.getResources(), R.drawable.background);
        background = scaleBackgroundToFitScreen(background);
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            sleep();
        }
    }

    private void update() {
        checkPlayerBallCollision();
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            // Draw the scaled background
            canvas.drawBitmap(background, 0, 0, paint);

            // Draw the players
            for (Player player : players) {
                player.draw(canvas, paint);
            }

            // Draw the balls
            for (Ball ball : ballList) {
                ball.draw(canvas, paint);
            }

            // Draw the joysticks
            for (Joystick joystick : joysticks) {
                joystick.draw(canvas);
            }

            holder.unlockCanvasAndPost(canvas);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Handle touch input for each joystick
                for (Joystick joystick : joysticks) {
                    joystick.onTouchEvent(touchX, touchY);
                }

                // Move players based on joystick positions
                movePlayers();
                break;
            case MotionEvent.ACTION_UP:
                // Reset all joysticks on touch up
                for (Joystick joystick : joysticks) {
                    joystick.reset();
                }
                break;
        }
        return true;
    }

    private void movePlayers() {
        // Iterate over each joystick and move the corresponding player
        for (int i = 0; i < joysticks.size(); i++) {
            Joystick joystick = joysticks.get(i);
            Player controlledPlayer = players.get(i);

            // Calculate movement direction from joystick stick position
            float deltaX = joystick.getStickPosition().x - joystick.getBaseCenter().x;
            float deltaY = joystick.getStickPosition().y - joystick.getBaseCenter().y;

            // Normalize the direction vector
            float magnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (magnitude > 0) {
                deltaX /= magnitude;
                deltaY /= magnitude;
            }

            // Move the controlled player based on joystick direction
            controlledPlayer.setX(controlledPlayer.getX() + deltaX * 6);
            controlledPlayer.setY(controlledPlayer.getY() + deltaY * 6);

            // Constrain the player to screen bounds
            controlledPlayer.setX(Math.max(controlledPlayer.getRadius(), Math.min(screenX - controlledPlayer.getRadius(), controlledPlayer.getX())));
            controlledPlayer.setY(Math.max(controlledPlayer.getRadius(), Math.min(screenY - controlledPlayer.getRadius(), controlledPlayer.getY())));
        }
    }

    private Bitmap scaleBackgroundToFitScreen(Bitmap background) {
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();

        // Calculate the scaling factor to fit the screen's width and height
        float scaleX = (float) screenX / bgWidth;
        float scaleY = (float) screenY / bgHeight;

        // Use these factors directly to scale the image to fit the screen
        int newWidth = (int) (bgWidth * scaleX);  // Stretch width
        int newHeight = (int) (bgHeight * scaleY);  // Stretch height

        // Create and return the scaled background
        return Bitmap.createScaledBitmap(background, newWidth, newHeight, true);
    }

    private void sleep() {
        long currentTime = System.nanoTime();
        long frameTime = 16_666_667; // Target for 60 FPS
        long sleepTime = frameTime - (currentTime - System.nanoTime());
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            } catch (InterruptedException e) {
                System.out.println("Sleep went wrong");
            }
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        try {
            isPlaying = false;
            thread.join();
        } catch (InterruptedException e) {
            System.out.println("Pause went wrong");
        }
    }

    private void checkPlayerBallCollision() {
        for (Player player : players) {
            for (Ball ball : ballList) {
                // Calculate the distance between the player and the ball
                float dx = player.getX() - ball.getX();
                float dy = player.getY() - ball.getY();
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // Check if the distance is less than or equal to the sum of the radii
                if (distance <= player.getRadius() + ball.getRadius()) {
                    // Collision detected
                    onPlayerHitBall(player, ball);
                }
            }
        }
    }

    private void onPlayerHitBall(Player player, Ball ball) {
        player.setBall(ball);
        // Calculate the direction from player to ball
        float dx = ball.getX() - player.getX();
        float dy = ball.getY() - player.getY();

        // Normalize the direction vector
        float magnitude = (float) Math.sqrt(dx * dx + dy * dy);
        if (magnitude > 0) {
            dx /= magnitude;
            dy /= magnitude;
        }

        // Move the ball in the direction of the player
        ball.setX(player.getX() + dx * 10);
        ball.setY(player.getY() + dy * 10);
    }
}