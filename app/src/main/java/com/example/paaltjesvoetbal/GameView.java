package com.example.paaltjesvoetbal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private final ArrayList<Joystick> joysticks;
    private final ArrayList<Player> players;
    private final ArrayList<Ball> balls;
    private final ArrayList<ShootButton> shootButtons;
    private final int PLAYERSPEED = 10;
    private final int BALLRADIUS = 23;
    private final int PLAYERCOUNT = 4;

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
            Player newPlayer;
            Joystick newJoystick;
            ShootButton shootButton;

            switch (i) {
                case 0: // Bottom-right
                    newPlayer = new Player(screenX * 0.75f, screenY * 0.8f, 45, Color.BLUE);
                    newJoystick = new Joystick(screenX - 120, screenY - 120);
                    shootButton = new ShootButton(screenX * 0.6f, screenY * 0.96f, 50, Color.BLUE);
                    newPlayer.setJoystick(newJoystick);
                    newPlayer.setShootButton(shootButton);

                    players.add(newPlayer);
                    joysticks.add(newJoystick);
                    shootButtons.add(shootButton);
                    break;
                case 1: // Top-left
                    newPlayer = new Player(screenX * 0.25f, screenY * 0.2f, 45, Color.RED);
                    newJoystick = new Joystick(120, 120);
                    shootButton = new ShootButton(screenX * 0.4f, screenY * 0.04f, 50, Color.RED);
                    newPlayer.setJoystick(newJoystick);
                    newPlayer.setShootButton(shootButton);

                    players.add(newPlayer);
                    joysticks.add(newJoystick);
                    shootButtons.add(shootButton);
                    break;
                case 2: // Bottom-left
                    newPlayer = new Player(screenX * 0.25f, screenY * 0.8f, 45, Color.GREEN);
                    newJoystick = new Joystick(120, screenY - 120);
                    shootButton = new ShootButton(screenX * 0.4f, screenY * 0.96f, 50, Color.GREEN);
                    newPlayer.setJoystick(newJoystick);
                    newPlayer.setShootButton(shootButton);

                    players.add(newPlayer);
                    joysticks.add(newJoystick);
                    shootButtons.add(shootButton);
                    break;
                case 3: // Top-right
                    newPlayer = new Player(screenX * 0.75f, screenY * 0.2f, 45, Color.YELLOW);
                    newJoystick = new Joystick(screenX - 120, 120);
                    shootButton = new ShootButton(screenX * 0.6f, screenY * 0.04f, 50, Color.YELLOW);
                    newPlayer.setJoystick(newJoystick);
                    newPlayer.setShootButton(shootButton);

                    players.add(newPlayer);
                    joysticks.add(newJoystick);
                    shootButtons.add(shootButton);
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
        for (Ball ball : balls) {
            ball.update(screenX, screenY);
        }
        updatePlayers();
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            drawPlayground(canvas);

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
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex(); // The specific pointer for non-move actions
        int pointerId = event.getPointerId(actionIndex); // Get the pointer ID

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float touchX = event.getX(actionIndex);
                float touchY = event.getY(actionIndex);
                boolean touchAssigned = false;

                // Check for shoot button touch
                for (ShootButton button : shootButtons) {
                    if (button.isTouched(touchX, touchY)) {
                        button.setTouchID(pointerId);
                        button.setPressed(true);
                        touchAssigned = true;
                        break;
                    }
                }

                // Check for joystick touch if no button was pressed
                if (!touchAssigned) {
                    for (Joystick joystick : joysticks) {
                        if (joystick.isControlledBy(touchX, touchY) && joystick.getTouchID() == -1) {
                            joystick.setPointerID(pointerId);
                            joystick.onTouch(touchX, touchY);
                            break;
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                boolean pointerHandled = false;

                // Handle joystick reset
                for (Joystick joystick : joysticks) {
                    if (joystick.isTouchedBy(pointerId)) {
                        joystick.reset();
                        pointerHandled = true;
                        break;
                    }
                }

                // Handle shoot button release
                if (!pointerHandled) {
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
                break;

            case MotionEvent.ACTION_MOVE:
                // Handle move for all active pointers
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int movePointerId = event.getPointerId(i);
                    float moveX = event.getX(i);
                    float moveY = event.getY(i);

                    for (Joystick joystick : joysticks) {
                        if (joystick.isTouchedBy(movePointerId)) {
                            joystick.onTouch(moveX, moveY);
                            break;
                        }
                    }
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

    private void drawPlayground(Canvas canvas) {
        // Draw the background
        canvas.drawColor(Color.parseColor("#FFEBCD"));

        // Draw the side lines
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);  // Side lines in black
        drawCorners(canvas, paint);

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
        topLeftPath.moveTo(0, 0);
        topLeftPath.lineTo(screenX * 0.8f, 0);
        topLeftPath.lineTo(0, screenX * 0.4f);
        topLeftPath.close();
        canvas.drawPath(topLeftPath, paint);

        // Top-right corner (triangle)
        Path topRightPath = new Path();
        topRightPath.moveTo(screenX, 0);
        topRightPath.lineTo(screenX, screenX * 0.4f);
        topRightPath.lineTo(screenX * 0.2f, 0);
        topRightPath.close();
        canvas.drawPath(topRightPath, paint);

        // Bottom-right corner (triangle)
        Path bottomRightPath = new Path();
        bottomRightPath.moveTo(screenX, screenY);
        bottomRightPath.lineTo(screenX, screenY - screenX * 0.4f);
        bottomRightPath.lineTo(screenX * 0.2f, screenY);
        bottomRightPath.close();
        canvas.drawPath(bottomRightPath, paint);

        // Bottom-left corner (triangle)
        Path bottomLeftPath = new Path();
        bottomLeftPath.moveTo(0, screenY);
        bottomLeftPath.lineTo(screenX * 0.8f, screenY);
        bottomLeftPath.lineTo(0, screenY - screenX * 0.4f);
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
}