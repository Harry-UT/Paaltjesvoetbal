package com.example.paaltjesvoetbal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean isPlaying;
    private final SurfaceHolder holder;
    private final int screenX;
    private final int screenY;
    private Bitmap background;
    private final ArrayList<Joystick> joysticks;
    private final ArrayList<Player> players;
    private final ArrayList<Ball> balls;
    private final ArrayList<ShootButton> shootButtons;
    private final int PLAYERSPEED = 40;

    public GameView(Context context, int screenX, int screenY) {
        super(context);
        this.screenX = screenX;
        this.screenY = screenY;
        holder = getHolder();

        // Initialize players and joysticks
        players = new ArrayList<>();
        joysticks = new ArrayList<>();
        shootButtons = new ArrayList<>();

        Player newPlayer = new Player(screenX / 2f, screenY / 1.1f, 50, Color.RED);
        players.add(newPlayer);

        Joystick newJoystick = new Joystick(screenX - 120, screenY - 120);
        newJoystick.setPlayer(newPlayer);
        newPlayer.setJoystick(newJoystick);
        joysticks.add(newJoystick);

        // Initialize the shoot button and add it to the list
        ShootButton shootButton = new ShootButton(screenX - 500, screenY - 200, 50);
        shootButtons.add(shootButton);
        newPlayer.setShootButton(shootButton);

        // Initialize balls
        balls = new ArrayList<>();
        Ball ball = new Ball(screenX / 2f, screenY / 2f, 30, Color.BLUE);
        balls.add(ball);

        // Load background image
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
        for (Ball ball : balls) {
            ball.update(screenX, screenY);
        }
        updatePlayers();
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            // Draw the scaled background
            canvas.drawBitmap(background, 0, 0, null);

            // Draw the players
            for (Player player : players) {
                player.draw(canvas);
            }

            // Draw the balls
            for (Ball ball : balls) {
                ball.draw(canvas);
            }

            // Draw the joysticks
            for (Joystick joystick : joysticks) {
                joystick.draw(canvas);  // Call the draw method for each joystick
            }

            // Draw the shoot buttons
            for (ShootButton button : shootButtons) {
                button.draw(canvas);
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
                // Handle shoot button press
                for (ShootButton button : shootButtons) {
                    if (button.isTouched(touchX, touchY)) {
                        button.setPressed(true); // Set button as pressed
                        button.shoot();         // Perform the shooting action
                        return true;  // Stop further processing if shoot button is touched
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Handle touch input for each joystick
                for (Joystick joystick : joysticks) {
                    float dx = joystick.getBaseCenter().x - touchX;
                    float dy = joystick.getBaseCenter().y - touchY;
                    if (Math.sqrt(dx * dx + dy * dy) <= 300) {
                        joystick.onTouch(touchX, touchY);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                // Reset joystick and shoot button when touch is released
                for (Joystick joystick : joysticks) {
                    joystick.reset();
                }
                for (ShootButton button : shootButtons) {
                    button.setPressed(false); // Reset button press state
                }
                break;
        }
        return true;
    }

    private void updatePlayers() {
        for (Player player : players) {
            float direction = player.getDirection();  // The direction set by joystick
            if (direction != 0) {
                float moveX = PLAYERSPEED * (float) Math.cos(direction);
                float moveY = PLAYERSPEED * (float) Math.sin(direction);
                float newX = player.getX() + moveX;
                float newY = player.getY() + moveY;

                // Clamp player position within screen bounds
                newX = Math.max(player.getRadius(), Math.min(newX, screenX - player.getRadius()));
                newY = Math.max(player.getRadius(), Math.min(newY, screenY - player.getRadius()));

                // Set the final position after checking bounds
                player.setX(newX);
                player.setY(newY);
            }
        }
    }

    private Bitmap scaleBackgroundToFitScreen(Bitmap background) {
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();

        float scaleX = (float) screenX / bgWidth;
        float scaleY = (float) screenY / bgHeight;

        int newWidth = (int) (bgWidth * scaleX);
        int newHeight = (int) (bgHeight * scaleY);

        return Bitmap.createScaledBitmap(background, newWidth, newHeight, true);
    }

    private void sleep() {
        long currentTime = System.nanoTime();
        long frameTime = 16_666_667;  // Target frame time for 60 FPS
        long elapsedTime = System.nanoTime() - currentTime;
        long sleepTime = frameTime - elapsedTime;

        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            } catch (InterruptedException e) {
                Log.e("Error", "Error in sleep");
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
            for (Ball ball : balls) {
                float dx = player.getX() - ball.getX();
                float dy = player.getY() - ball.getY();
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance <= player.getRadius() + ball.getRadius()) {
                    onPlayerHitBall(player, ball);
                }
            }
        }
    }

    private void onPlayerHitBall(Player player, Ball ball) {
        if (player.canTakeBall()) {
            if (ball.getPlayer() != null) {
                ball.getPlayer().setBall(null);
            }
            player.setBall(ball);
            ball.setPlayer(player);

            float deltaX = ball.getX() - player.getX();
            float deltaY = ball.getY() - player.getY();

            float magnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (magnitude > 0) {
                deltaX /= magnitude;
                deltaY /= magnitude;
            }

            float combinedRadius = player.getRadius() + ball.getRadius();
            float newBallX = player.getX() + deltaX * combinedRadius;
            float newBallY = player.getY() + deltaY * combinedRadius;

            ball.setX(newBallX);
            ball.setY(newBallY);
        }
    }
}