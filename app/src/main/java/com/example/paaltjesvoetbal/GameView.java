package com.example.paaltjesvoetbal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Matrix;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private Thread updateThread;
    private Thread drawThread;
    Bitmap settingsIcon = BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon);
    private boolean isPlaying;
    private final SurfaceHolder holder;
    private final int screenX;
    private final int screenY;
    private final List<Joystick> joysticks;
    private final List<Player> players;
    private final List<Ball> balls;
    private final List<ShootButton> shootButtons;
    private final int PLAYERSPEED = 10;
    private final int PLAYERRADIUS = 40;
    private final int BALLRADIUS = 20;
    private final int JOYSTICKRADIUS = 95;
    private final int SHOOTBUTTONRADIUS = 50;
    private final int PLAYERCOUNT = 2;

    public GameView(Context context, int screenX, int screenY) {
        super(context);
        setKeepScreenOn(true);
        this.screenX = screenX;
        this.screenY = screenY;
        holder = getHolder();

        // Initialize players and joysticks
        players = new ArrayList<>();
        joysticks = new ArrayList<>();
        shootButtons = new ArrayList<>();

        for (int i = 0; i < PLAYERCOUNT; i++) {
            if (i >= 4) break;
            switch (i) {
                case 0: // Bottom-right
                    initializePlayer1();
                    break;
                case 1: // Top-left
                    initializePlayer2();
                    break;
                case 2: // Bottom-left
                    initializePlayer3();
                    break;
                case 3: // Top-right
                    initializePlayer4();
                    break;
                default:
                    break;
            }
        }

        // Initialize ball(s)
        balls = new ArrayList<>();
        Ball ball = new Ball(screenX / 2f, screenY / 2f, BALLRADIUS);
        balls.add(ball);
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
        synchronized (balls) {
            for (Ball ball : balls) {
                ball.update(screenX, screenY);
            }
        }
        updatePlayers();
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            drawPlayground(canvas);

            // Draw the players
            synchronized (players) {
                for (Player player : players) {
                    player.draw(canvas);
                }
            }

            // Draw the balls
            synchronized (balls) {
                for (Ball ball : balls) {
                    ball.draw(canvas);
                }
            }
            // Draw the joysticks
            synchronized (joysticks) {
                for (Joystick joystick : joysticks) {
                    joystick.draw(canvas);  // Call the draw method for each joystick
                }
            }

            // Draw the shoot buttons
            synchronized (shootButtons) {
                for (ShootButton button : shootButtons) {
                    button.draw(canvas);
                }
            }

            holder.unlockCanvasAndPost(canvas);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Create a new Runnable to handle the touch event logic in a separate thread
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex(); // The specific pointer for non-move actions
            int pointerId = event.getPointerId(actionIndex); // Get the pointer ID

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    float touchX = event.getX(actionIndex);
                    float touchY = event.getY(actionIndex);
                    boolean touchAssigned = false;

                    // Check for settings icon touch
                    float scaleFactor = 0.40f;
                    // Compute the dimensions of the scaled icon
                    float scaledWidth = settingsIcon.getWidth() * scaleFactor;
                    float scaledHeight = settingsIcon.getHeight() * scaleFactor;
                    // Calculate the icon's bounding box
                    float centerX = screenX / 2f;
                    float centerY = screenY / 2f;
                    float left = centerX - (scaledWidth / 2);
                    float top = centerY - (scaledHeight / 2);
                    float right = left + scaledWidth;
                    float bottom = top + scaledHeight;
                    // Check if the touch event is within the icon's bounding box
                    if (touchX >= left && touchX <= right && touchY >= top && touchY <= bottom) {
                        SettingsDialog settingsDialog = new SettingsDialog(getContext(), (SettingsDialog.OnSettingsChangedListener) getContext());
                        settingsDialog.show();
                    }

                    // Check for shoot button touch
                    synchronized (shootButtons) {
                        for (ShootButton button : shootButtons) {
                            if (button.isTouched(touchX, touchY)) {
                                button.setTouchID(pointerId);
                                button.setPressed(true);
                                touchAssigned = true;
                                break;
                            }
                        }
                    }

                    // Check for joystick touch if no button was pressed
                    if (!touchAssigned) {
                        synchronized (joysticks) {
                            for (Joystick joystick : joysticks) {
                                if (joystick.isTouched(touchX, touchY) && joystick.getTouchID() == -1) {
                                    joystick.setPointerID(pointerId);
                                    joystick.onTouch(touchX, touchY);
                                    break;
                                }
                            }
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    boolean pointerHandled = false;

                    // Handle joystick reset
                    synchronized (joysticks) {
                        for (Joystick joystick : joysticks) {
                            if (joystick.isTouchedBy(pointerId)) {
                                joystick.reset();
                                pointerHandled = true;
                                break;
                            }
                        }
                    }

                    // Handle shoot button release
                    if (!pointerHandled) {
                        synchronized (shootButtons) {
                            for (ShootButton button : shootButtons) {
                                if (button.wasTouchedBy(pointerId)) {
                                    if (button.isTouched(event.getX(actionIndex), event.getY(actionIndex))) {
                                        button.shoot();
                                        button.resetBall();
                                    }
                                    button.resetTouchID();
                                    button.setPressed(false);
                                    break;
                                }
                            }
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Handle move for all active pointers
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        int movePointerId = event.getPointerId(i);
                        float moveX = event.getX(i);
                        float moveY = event.getY(i);

                        synchronized (joysticks) {
                            for (Joystick joystick : joysticks) {
                                if (joystick.isTouchedBy(movePointerId)) {
                                    joystick.onTouch(moveX, moveY);
                                    break;
                                }
                            }
                        }
                    }
                    break;
            }
        });
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
        synchronized (players) {
            for (Player player : players) {
                synchronized (balls) {
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

    private void drawPlayground(Canvas canvas) {
        // Draw the background
        canvas.drawColor(Color.parseColor("#FFEBCD"));

        // Draw the side lines
        Paint paint = new Paint();
        drawCorners(canvas, paint);

        // Draw settings icon in the middle
        drawSettingsIcon(canvas);

        // Draw the bottom UI bar (black)
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, screenY, screenX, screenY + 40, paint);

        paint.setStrokeWidth(5);

        // Draw circle in the middle
        drawMiddle(canvas);

        // Draw the side lines
//        canvas.drawLine(screenX * 0.95f, screenY * 0.2f, screenX * 0.95f, screenY * 0.8f, paint);
//        canvas.drawLine(0, 0, 0, screenY, paint);
//        canvas.drawLine(screenX, 0, screenX, screenY, paint);
//        canvas.drawLine(0, 0, screenX, 0, paint);
    }

    private void drawCorners(Canvas canvas, Paint paint) {
        // Top-left corner (triangle)
        Path topLeftPath = new Path();
        paint.setColor(Color.argb(128, 255, 0, 0)); // Light red
        topLeftPath.moveTo(0, 0);
        topLeftPath.lineTo(screenX * 0.5f, 0);
        topLeftPath.lineTo(screenX * 0.5f, screenY * 0.09f);
        topLeftPath.lineTo(0, screenX * 0.5f);
        topLeftPath.close();
        canvas.drawPath(topLeftPath, paint);

        // Top-right corner (triangle)
        Path topRightPath = new Path();
        paint.setColor(Color.argb(140, 255, 255, 0)); // Light yellow
        topRightPath.moveTo(screenX * 0.5f, 0);
        topRightPath.lineTo(screenX, 0);
        topRightPath.lineTo(screenX, screenX * 0.5f);
        topRightPath.lineTo(screenX * 0.5f, screenY * 0.09f);
        topRightPath.close();
        canvas.drawPath(topRightPath, paint);

        // Bottom-right corner (triangle)
        Path bottomRightPath = new Path();
        paint.setColor(Color.argb(140, 0, 0, 255)); // Light blue
        bottomRightPath.moveTo(screenX, screenY - screenX * 0.5f);
        bottomRightPath.lineTo(screenX, screenY);
        bottomRightPath.lineTo(screenX * 0.5f, screenY);
        bottomRightPath.lineTo(screenX * 0.5f, screenY * 0.91f);
        bottomRightPath.close();
        canvas.drawPath(bottomRightPath, paint);

        // Bottom-left corner (triangle)
        Path bottomLeftPath = new Path();
        paint.setColor(Color.rgb(140, 238, 144)); // Light green
        bottomLeftPath.moveTo(screenX * 0.5f, screenY);
        bottomLeftPath.lineTo(0, screenY);
        bottomLeftPath.lineTo(0, screenY - screenX * 0.5f);
        bottomLeftPath.lineTo(screenX * 0.5f, screenY * 0.91f);
        bottomLeftPath.close();
        canvas.drawPath(bottomLeftPath, paint);
    }

    private void drawMiddle(Canvas canvas) {
        // Set up the paint for the circle (donut shape)
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);  // Set the circle's outline color
        paint.setStyle(Paint.Style.STROKE);  // Set it to STROKE to create an open middle circle
        paint.setStrokeWidth(20);  // Set the width of the stroke

        // Get the center of the screen
        float centerX = screenX / 2f;
        float centerY = screenY / 2f;

        // Radius of the circle
        float outerRadius = screenX * 0.08f;

        // Draw the outer circle
        canvas.drawCircle(centerX, centerY, outerRadius, paint);
    }

    private void drawSettingsIcon(Canvas canvas) {
        // Create a new matrix for scaling and translation
        Matrix matrix = new Matrix();
        float scaleFactor = 0.37f;

        // Desired position for the icon (center of the screen)
        float centerX = screenX / 2f;
        float centerY = screenY / 2f;

        // Apply scaling to the matrix
        matrix.postScale(scaleFactor, scaleFactor);

        // Compute the dimensions of the scaled icon
        float scaledWidth = settingsIcon.getWidth() * scaleFactor;
        float scaledHeight = settingsIcon.getHeight() * scaleFactor;

        // Adjust the translation to account for the scaled dimensions
        matrix.postTranslate(centerX - (scaledWidth / 2), centerY - (scaledHeight / 2));

        // Draw the scaled and translated bitmap at the desired position
        canvas.drawBitmap(settingsIcon, matrix, null);
    }

    public void changePlayerCount(int playerCount) {
        switch (playerCount) {
            case 2:
                if (players.size() == 2) {
                    break;
                }
                clearLists();
                initializePlayer1();
                initializePlayer2();
                break;
            case 3:
                if (players.size() == 3) {
                    break;
                }
                clearLists();
                initializePlayer1();
                initializePlayer2();
                initializePlayer3();
                break;
            case 4:
                if (players.size() == 4) {
                    break;
                }
                clearLists();
                initializePlayer1();
                initializePlayer2();
                initializePlayer3();
                initializePlayer4();
                break;
            default:
                break;
        }
    }

    private void clearLists() {
        synchronized (players) {
            players.clear();
        }
        synchronized (joysticks) {
            joysticks.clear();
        }
        synchronized (shootButtons) {
            shootButtons.clear();
        }
    }

    private void addPlayer(Player player) {
        synchronized (players) {
            players.add(player);
        }
    }

    private void addJoystick(Joystick joystick) {
        synchronized (joysticks) {
            joysticks.add(joystick);
        }
    }

    public void addBall(Ball ball) {
        synchronized (balls) {
            balls.add(ball);
        }
    }

    public void addShootButton(ShootButton shootButton) {
        synchronized (shootButtons) {
            shootButtons.add(shootButton);
        }
    }

    public void initializePlayer1() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(screenX * 0.75f, screenY * 0.78f, PLAYERRADIUS, Color.BLUE);
        newJoystick = new Joystick(screenX * 0.78f, screenY - screenX * 0.1f, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.92f, screenY * 0.83f, SHOOTBUTTONRADIUS, Color.BLUE);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    public void initializePlayer2() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(screenX * 0.25f, screenY * 0.22f, PLAYERRADIUS, Color.RED);
        newJoystick = new Joystick(screenX * 0.22f, JOYSTICKRADIUS, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.08f, screenY * 0.17f, SHOOTBUTTONRADIUS, Color.RED);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    public void initializePlayer3() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(screenX * 0.25f, screenY * 0.78f, PLAYERRADIUS, Color.GREEN);
        newJoystick = new Joystick(screenX * 0.22f, screenY - screenX * 0.1f, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.08f, screenY * 0.83f, SHOOTBUTTONRADIUS, Color.GREEN);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    public void initializePlayer4() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(screenX * 0.75f, screenY * 0.22f, PLAYERRADIUS, Color.YELLOW);
        newJoystick = new Joystick(screenX * 0.78f, JOYSTICKRADIUS, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.91f, screenY * 0.17f, SHOOTBUTTONRADIUS, Color.YELLOW);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }
}