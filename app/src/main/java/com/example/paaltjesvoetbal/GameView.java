package com.example.paaltjesvoetbal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.Typeface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Matrix;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Handler;
import android.os.Looper;


/**
 * The GameView class is responsible for updating and rendering the game elements on the screen
 */
@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private final Bitmap settingsIcon = BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon);
    private boolean isPlaying;
    private final SurfaceHolder holder;
    private final int screenX;
    private final int screenY;
    private final List<Joystick> joysticks;
    private List<Player> players;
    private final List<Ball> balls;
    private final List<ShootButton> shootButtons;
    private final int[] playerColors = {Color.BLUE, Color.RED, Color.GREEN, 0xFFFFEB04};
    private int[][] playerPositions;
    private int PLAYERSPEED = 4;
    private int BALL_SPEED = 18;
    private final int PLAYERRADIUS = 30;
    private final int BALLRADIUS = 15;
    private final int JOYSTICKRADIUS = 95;
    private final int SHOOTBUTTONRADIUS = 50;
    private final int PLAYERCOUNT = 4;
    private final double goalWidth = 0.5;
    private final List<Vector> diagonalEdges = new ArrayList<>();
    private final List<Vector> verticalGoalEdges = new ArrayList<>();
    private final List<Vector> bounceEdges = new ArrayList<>();
    private final List<Vector> goals = new ArrayList<>();
    private final List<Path> cornerPaths = new ArrayList<>();
    private final List<Region> goalRegions = new ArrayList<>();
    private long lastBounceTime = 0;
    private float lastGoalTime;
    private Player lastShooter;
    private int lastGoal;
    private boolean scored = false;
    private final Star[] stars = new Star[50];
    private long splashStartTime = 0;
    private FloatingText goalText;
    private FloatingText scoreIncrementText;
    private final int TARGET_FPS = 60;
    private int fps;

    /**
     * Constructor for the GameView class
     * @param context the context of the game
     * @param screenX the width of the screen
     * @param screenY the height of the screen
     */
    public GameView(Context context, int screenX, int screenY) {
        super(context);
        setKeepScreenOn(true);
        this.screenX = screenX;
        this.screenY = screenY;
        this.goalText = new FloatingText((int) (screenX / 2f), (int) (screenY / 2f), 60, 0);
        this.scoreIncrementText = new FloatingText(0,0, 40, 0);
        playerPositions = new int[][]{{(int) (screenX * 0.75f), (int) (screenY * 0.78f)}, {(int) (screenX * 0.25f), (int) (screenY * 0.22f)}, {(int) (screenX * 0.25f), (int) (screenY * 0.78f)}, {(int) (screenX * 0.75f), (int) (screenY * 0.22f)}};
        holder = getHolder();

        // Initialize players, joysticks and shoot buttons
        players = new ArrayList<>();
        joysticks = new ArrayList<>();
        shootButtons = new ArrayList<>();

        for (int i = 0; i < PLAYERCOUNT; i++) {
            switch (i) {
                case 0: // Bottom-right (blue)
                    initializePlayer1();
                    break;
                case 1: // Top-left (red)
                    initializePlayer2();
                    break;
                case 2: // Bottom-left (green)
                    initializePlayer3();
                    break;
                case 3: // Top-right (yellow)
                    initializePlayer4();
                    break;
                default:
                    break;
            }
        }
        // Determine player corner areas
        determineGoalEdges();

        // Determine edges for ball bounce
        determineBounceEdges();

        // Determine the goals
        determineGoals();

        // Determine score text positions
        determineScorePositions();

        // Initialize ball(s)
        balls = new ArrayList<>();
        Ball ball = new Ball(screenX / 2f, screenY / 2f, BALLRADIUS, bounceEdges, verticalGoalEdges);
        balls.add(ball);

        // Initialize balls for goal animation
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star();
        }
    }

    /**
     * Run method for the game thread
     */
    @Override
    public void run() {
        final long OPTIMAL_TIME = 1000000000 / TARGET_FPS; // Time per frame in nanoseconds (1 second / 30 FPS)

        long lastLoopTime = System.nanoTime();
        long lastTime = System.nanoTime();
        int frames = 0;

        while (isPlaying) {
            long now = System.nanoTime();
            long updateLength = now - lastLoopTime;
            long sleepTime = OPTIMAL_TIME - updateLength; // Time to sleep to maintain the target FPS

            if (sleepTime > 0) {
                // Convert sleep time to milliseconds and sleep to maintain FPS
                try {
                    Thread.sleep(sleepTime / 1000000); // Sleep in milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Busy-wait to sync the loop without overshooting the target FPS
            while (System.nanoTime() - lastLoopTime < OPTIMAL_TIME) {
                // Empty loop for busy-wait, ensuring frame time is as close to optimal as possible
            }

            lastLoopTime = System.nanoTime();

            update(); // Update game state
            draw();   // Render game frame

            frames++; // Count frames

            // Calculate and log real FPS every second
            if (System.nanoTime() - lastTime >= 1_000_000_000) {
                Log.d("FPSTRACK", "Real FPS: " + frames); // Log the real FPS
                fps = frames; // Save the real FPS
                frames = 0; // Reset frame count
                lastTime = System.nanoTime();
            }
        }
    }

    /**
     * Determine the edges for ball bounce based on the screen dimensions
     */
    private void determineBounceEdges() {
        for (Vector edge : diagonalEdges) {
            Vector[] bounceVectors = edge.split(goalWidth);
            bounceEdges.addAll(Arrays.asList(bounceVectors));
        }
    }

    /**
     * Initialize the first player object
     */
    private void determineGoals() {
        // Iterate over all diagonal edges to determine the goals
        for (Vector edge : diagonalEdges) {
            // Get scaled vector
            Vector scaledEdge = edge.getCenteredAndScaledVector(goalWidth);
            goals.add(scaledEdge);
        }
    }

    /**
     * Determine the positions for the score text based on the screen dimensions
     */
    private void determineScorePositions() {
        for (Player player : players) {
            int index = players.indexOf(player);

            // Calculate text rotation angle
            Vector goal = goals.get(index);

            float dx = (float) (goal.getX2() - goal.getX1());
            float dy = (float) (goal.getY2() - goal.getY1());
            float scale = 1.0f / (Math.abs(dx) + Math.abs(dy)); // A crude approximation
            dx *= scale;
            dy *= scale;

            float middleX = goal.getMidX();
            float middleY = goal.getMidY();

            float dxPerpendicular = -dy;
            float dyPerpendicular = dx;

            int xText = (int) (middleX + dxPerpendicular * screenX * 0.25);
            int yText = (int) (middleY + dyPerpendicular * screenX * 0.25);
            player.setScorePosition(xText, yText);
        }
    }

    /**
     * Update the game state
     */
    private void update() {
        synchronized (balls) {
            for (Ball ball : balls) {
                lastBounceTime = ball.update(screenX, screenY);
                // Log presence of shooter for ball
                Log.d("Ball", "Ball shooter: " + ball.getShooter());
                if (!scored && System.currentTimeMillis() - lastBounceTime > 200) {
                    checkGoal(ball);
                }
            }
        }
        checkPlayerBallCollision();
        updatePlayers();
    }

    /**
     * Handle the scoring of a goal by a player
     * @param goal the goal number
     * @param player the player object
     */
    private void scored(int goal, Player player) {
        // Scored in goal by player
        player.scored();

        int playerColor = player.getColor();

        if (playerColor == playerColors[0]) {
            Log.d("Goal", "Player 0 scored in goal " + goal);
        } else if (playerColor == playerColors[1]) {
            Log.d("Goal", "Player 1 scored in goal " + goal);
        } else if (playerColor == playerColors[2]) {
            Log.d("Goal", "Player 2 scored in goal " + goal);
        } else if (playerColor == playerColors[3]) {
            Log.d("Goal", "Player 3 scored in goal " + goal);
        } else {
            Log.d("Goal", "Unknown player color");
        }

        Log.d("Goal", "Player " + player + " scored in goal " + goal);
        scored = true;
    }

    /**
     * Check if a goal has been scored by a player
     * @param ball the ball object
     */
    private void checkGoal(Ball ball) {
        // Loop through all goal regions to check if the ball is within any of them
        for (int i = 0; i < goalRegions.size(); i++) {
            Region region = goalRegions.get(i);

            // Check if the ball's current position is inside the goal region
            if (region.contains((int) ball.getX(), (int) ball.getY())) {

                // Check if it's the same region as the last goal and scored quickly
                if (lastGoal == i && ball.getShooter() != null) {
                    if (System.currentTimeMillis() - lastGoalTime < 200) {
                        // A goal has been scored in this region by the shooter
                        if (i != ball.getShooter().getNumber() && i <= players.size() - 1) {
                            // Log something
                            scored(i, ball.getShooter());
                            lastShooter = ball.getShooter();
                            int rotation = 0;
                            if (players.indexOf(lastShooter) == 1 || players.indexOf(lastShooter) == 3) {
                                rotation = 180;
                            }
                            this.scoreIncrementText = new FloatingText(ball.getShooter().getScorePosition()[0], ball.getShooter().getScorePosition()[1], 40, rotation);
                            // Log text coordinates from Floatint text

                            for (Star star : stars) {
                                star.setColor(ball.getShooter().getColor());
                            }
                        }
                        lastGoal = -1; // Reset the last goal
                        ball.resetShooter(); // Reset the shooter information
                        break;
                    }
                } else {
                    // A different region has been scored, so update the goal region and time
                    lastGoalTime = System.currentTimeMillis();
                    lastGoal = i;
                }
            }
        }
    }

    /**
     * Display the goal animation on the screen
     */
    public void displayGoalAnimation(Canvas canvas) {
        // Save time
        if (splashStartTime == 0) {
            splashStartTime = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - splashStartTime < 1200) {
            for (Star star : stars) {
                star.update(canvas, (int) balls.get(0).getX(), (int) balls.get(0).getY(), false);
                star.bounce(screenX, screenY);
            }
        } else {
            scored = false;
            splashStartTime = 0;
            lastShooter = null;
            for (Star star : stars) {
                star.resetStartTime();
            }
            balls.get(0).reset((int) (screenX / 2f), (int) (screenY / 2f));
            for (int i = 0; i < players.size(); i++) {
                players.get(i).resetPosition(playerPositions[i][0], playerPositions[i][1]);
            }
        }
    }

    /**
     * Draw the game elements on the canvas
     */
    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            drawPlayground(canvas);

            // Draw the corner paths
            synchronized (cornerPaths) {
                Paint paint = new Paint();
                for (int i = 0; i < cornerPaths.size(); i++) {
                    switch(i) {
                        case 0:
                            paint.setColor(Color.argb(140, 0, 0, 255)); // Light blue
                            canvas.drawPath(cornerPaths.get(i), paint);
                            break;
                        case 1:
                            paint.setColor(Color.argb(128, 255, 0, 0)); // Light red
                            canvas.drawPath(cornerPaths.get(i), paint);
                            break;
                        case 2:
                            paint.setColor(Color.rgb(140, 238, 144)); // Light green
                            canvas.drawPath(cornerPaths.get(i), paint);
                            break;
                        case 3:
                            paint.setColor(Color.argb(140, 255, 255, 0)); // Light yellow
                            canvas.drawPath(cornerPaths.get(i), paint);
                            break;
                    }
                }
            }

            // Draw the bounce edges black
            synchronized (bounceEdges) {
                Paint paint = new Paint();
//                paint.setColor(Color.rgb(255, 165, 0));  // Orange color
//                paint.setColor(Color.rgb(255, 255, 255));  // White color
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(5);
                for (Vector edge : bounceEdges) {
                    canvas.drawLine((float) edge.getX1(), (float) edge.getY1(), (float) edge.getX2(), (float) edge.getY2(), paint);
                }
            }

            // Draw the vertical goal edges black
            synchronized (verticalGoalEdges) {
                Paint paint = new Paint();
//                paint.setColor(Color.rgb(255, 255, 255));  // White color
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(5);
                for (Vector edge : verticalGoalEdges) {
                    canvas.drawLine((float) edge.getX1(), (float) edge.getY1(), (float) edge.getX2(), (float) edge.getY2(), paint);
                }
            }

            // Draw goalposts
            drawGoalPosts(canvas);

            // Draw the joysticks
            synchronized (joysticks) {
                for (Joystick joystick : joysticks) {
                    joystick.draw(canvas);  // Call the draw method for each joystick
                }
            }

            drawScores(canvas);

            // Draw the balls
            synchronized (balls) {
                for (Ball ball : balls) {
                    ball.draw(canvas);
                }
            }

            // Draw the shoot buttons
            synchronized (shootButtons) {
                for (ShootButton button : shootButtons) {
                    button.draw(canvas);
                }
            }

            // Draw the players
            synchronized (players) {
                for (Player player : players) {
                    player.draw(canvas);
                }
            }

//            synchronized (goals) {
//                for (Vector goal : goals) {
//                    goal.draw(canvas);
//                }
//            }

            if (scored) {
                displayGoalAnimation(canvas);
            } else {
                // Check if all scores are 0
                boolean allZero = true;
                for (Player player : players) {
                    if (player.getScore() != 0) {
                        allZero = false;
                        break;
                    }
                }
                if (!allZero) {
                    for (Star star : stars) {
                        // Fade stars out
                        star.update(canvas, (int) balls.get(0).getX(), (int) balls.get(0).getY(), true);
                        star.bounce(screenX, screenY);
                    }
                }
            }

            // Draw fps
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            canvas.drawText("FPS: " + fps, 10, 50, paint);

            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawGoalPosts(Canvas canvas) {
        // Use mod 2 to alternate between first or second coordinate of edge
        int postSide = 1;
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        for (Vector bounceEdge : bounceEdges) {
            // Get the goalpost coordinates
            if (postSide % 2 == 0) {
                float x1 = (float) bounceEdge.getX1();
                float y1 = (float) bounceEdge.getY1();
                // Draw a little black dot
                canvas.drawCircle(x1, y1, 6, paint);
            } else {
                float x2 = (float) bounceEdge.getX2();
                float y2 = (float) bounceEdge.getY2();
                // Draw a little black dot
                canvas.drawCircle(x2, y2, 5, paint);
            }
            postSide++;
        }
    }

    /**
     * Draw the scores of the players on the screen
     * @param canvas the canvas to draw on
     */
    private void drawScores(Canvas canvas) {
        Paint paint = new Paint();
        paint.setTextSize(60);
        paint.setAntiAlias(true); // Smooth text edges

        Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.bungee);
        paint.setTypeface(typeface); // Set custom font

        int xText;
        int yText;

        synchronized (players) {
            for (Player player : players) {
                int index = players.indexOf(player);

                // Calculate text rotation angle
                Vector goal = goals.get(index);

                float dx = (float) (goal.getX2() - goal.getX1());
                float dy = (float) (goal.getY2() - goal.getY1());
                float scale = 1.0f / (Math.abs(dx) + Math.abs(dy));
                dx *= scale;
                dy *= scale;

                float middleX = goal.getMidX();
                float middleY = goal.getMidY();
                float rotationAngle = (float) Math.toDegrees(Math.atan2(dy, dx));


                float dxPerpendicular = -dy;
                float dyPerpendicular = dx;

//            float lineLength = 500; // Length of the perpendicular line
//            float startX = middleX;
//            float startY = middleY;
//            float endX = middleX + (dxPerpendicular * lineLength / 2);
//            float endY = middleY + (dyPerpendicular * lineLength / 2);
//
//            canvas.drawLine(startX, startY, endX, endY, paint);

                switch (index) {
                    case 0: // Bottom-right (blue)
                        if (rotationAngle > 0) {
                            rotationAngle -= 180;
                        }
                        break;
                    case 1: // Top-left (red)
                        if (rotationAngle < 0) {
                            rotationAngle += 180;
                        }
                        break;
                    case 2: // Bottom-left (green)
                        if (rotationAngle < 0) {
                            rotationAngle += 180;
                        }
                        break;
                    case 3: // Top-right (yellow)
                        if (rotationAngle > 0) {
                            rotationAngle -= 180;
                        }
                        break;
                    default:
                        return;
                }

//            xText = (int) middleX;
//            yText = (int) middleY;
                xText = (int) (middleX + dxPerpendicular * screenX * 0.25);
                yText = (int) (middleY + dyPerpendicular * screenX * 0.25);

                // Center text with textmeasure
                float textWidth = paint.measureText(String.valueOf(player.getScore()));

                switch (index) {
                    case 0:
                        xText -= (int) (textWidth / 2);
                        break;
                    case 1:
                        xText += (int) (textWidth / 2);
                        break;
                    case 2:
                        // Use ascend and descent to center text vertically
                        yText += (int) ((paint.descent() + paint.ascent()) / 2);
                        xText -= (int) (textWidth / 4);
                        break;
                    case 3:
                        // Use ascend and descent to center text vertically
                        yText += (int) ((paint.descent() + paint.ascent()) / 2 + (int) (0.029f * screenY));
                        xText += (int) (textWidth / 4);
                        break;
                    default:
                        break;
                }

                // Set text color
                paint.setColor(player.getColor());

                // Save the current canvas state
                canvas.save();

                // Rotate around the text position
                canvas.rotate(rotationAngle, xText, yText);

                // Draw the text
                canvas.drawText(String.valueOf(player.getScore()), xText, yText, paint);

                // Restore canvas to avoid affecting other drawings
                canvas.restore();
            }
        }

        if (scored) {
            // Set text size
            paint.setTextSize(goalText.getSize());
            paint.setColor(lastShooter.getColor());
            goalText.increment(3, 0, 0);

            // Measure the width of the text
            float textWidth = paint.measureText("GOAL!");

            // Calculate the x position for centering the text
            float x = (screenX - textWidth) / 2;

            // Calculate the y position for centering the text vertically
            // Adjusting based on text ascent and descent
            float y = ((float) screenY / 2) - ((paint.descent() + paint.ascent()) / 2);

            // Draw the centered text
            canvas.drawText("GOAL!", x, y, paint);

            paint.setColor(lastShooter.getColor());
            paint.setTextSize(scoreIncrementText.getSize());
            // Draw a score increment animation above the last shooter score position
            canvas.save();
            canvas.rotate(scoreIncrementText.getRotation(), scoreIncrementText.getX(), scoreIncrementText.getY());
            canvas.drawText("+1", scoreIncrementText.getX(), scoreIncrementText.getY(), paint);
            canvas.restore();
            if (scoreIncrementText.getRotation() == 180) {
                scoreIncrementText.increment(0, 0, 1);
            } else {
                scoreIncrementText.increment(0, 0, -1);
            }
            } else {
            goalText.reset(screenX, screenY);
        }
    }

    /**
     * Handle touch events on the screen
     * @param event the touch event
     * @return true if the event was handled, false otherwise
     */
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
                        SettingsDialog settingsDialog = new SettingsDialog(getContext(), (SettingsDialog.OnSettingsChangedListener) getContext(), players.size(), PLAYERSPEED, BALL_SPEED);
                        settingsDialog.show();
                    }

                    // Check for joystick touch if no button was pressed
                    synchronized (joysticks) {
                        for (Joystick joystick : joysticks) {
                            if (joystick.isTouched(touchX, touchY) && joystick.getTouchID() == -1) {
                                joystick.setPointerID(pointerId);
                                joystick.onTouch(touchX, touchY);
                                touchAssigned = true;
                                break;
                            }
                        }
                    }

                    if (!touchAssigned) {
                        // Check for shoot button touch
                        synchronized (shootButtons) {
                            for (ShootButton button : shootButtons) {
                                if (button.isTouched(touchX, touchY)) {
                                    button.setTouchID(pointerId);
                                    button.setPressed(true);
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
                                        button.shoot(BALL_SPEED);
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

    /**
     * Update the player positions based on joystick input
     */
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
        determineScorePositions();
    }

    /**
     * Start the game thread
     */
    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Pause the game thread
     */
    public void pause() {
        try {
            isPlaying = false;
            thread.join();
        } catch (InterruptedException e) {
            System.out.println("Pause went wrong");
        }
    }

    /**
     * Check for collisions between players and balls
     */
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

    /**
     * Handle the collision between a player and a ball
     * @param player the player object
     * @param ball the ball object
     */
    private void onPlayerHitBall(Player player, Ball ball) {
        if (player.canTakeBall()) {
            if (ball.getPlayer() != null) {
                ball.getPlayer().setBall(null);
            }
            player.setBall(ball);
            ball.setPlayer(player);
            ball.resetShooter();

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

    /**
     * Draw the game playground on the canvas
     * @param canvas the canvas to draw on
     */
    private void drawPlayground(Canvas canvas) {
        // Draw the background
        canvas.drawColor(Color.parseColor("#FFF1E9"));

        // Draw settings icon in the middle
        drawSettingsIcon(canvas);

        // Draw the bottom UI bar (black)
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, screenY, screenX, screenY + 40, paint);

        paint.setStrokeWidth(5);

        // Draw circle in the middle
        drawMiddleCircle(canvas);

        // Draw the side lines
//        canvas.drawLine(screenX * 0.95f, screenY * 0.2f, screenX * 0.95f, screenY * 0.8f, paint);
//        canvas.drawLine(0, 0, 0, screenY, paint);
//        canvas.drawLine(screenX, 0, screenX, screenY, paint);
//        canvas.drawLine(0, 0, screenX, 0, paint);
    }

    /**
     * Determine the corner areas of the screen
     */
    private void determineGoalEdges() {
        // Bottom-right corner (triangle)
        Path bottomRightPath = new Path();
        bottomRightPath.moveTo(screenX, screenY - screenX * 0.5f);
        bottomRightPath.lineTo(screenX, screenY);
        bottomRightPath.lineTo(screenX * 0.5f, screenY);
        bottomRightPath.lineTo(screenX * 0.5f, screenY * 0.91f);
        bottomRightPath.close();
        diagonalEdges.add(new Vector(screenX, screenY - screenX * 0.5, screenX * 0.5, screenY * 0.91));

        verticalGoalEdges.add(new Vector(screenX * 0.5, screenY * 0.91, screenX * 0.5, screenY));

        // Top-left corner (triangle)
        Path topLeftPath = new Path();
        topLeftPath.moveTo(0, 0);
        topLeftPath.lineTo(screenX * 0.5f, 0);
        topLeftPath.lineTo(screenX * 0.5f, screenY * 0.09f);
        topLeftPath.lineTo(0, screenX * 0.5f);
        topLeftPath.close();
        diagonalEdges.add(new Vector(0, screenX * 0.5, screenX * 0.5, screenY * 0.09));

        verticalGoalEdges.add(new Vector(screenX * 0.5, 0, screenX * 0.5, screenY * 0.09));

        // Bottom-left corner (triangle)
        Path bottomLeftPath = new Path();
        bottomLeftPath.moveTo(screenX * 0.5f, screenY);
        bottomLeftPath.lineTo(0, screenY);
        bottomLeftPath.lineTo(0, screenY - screenX * 0.5f);
        bottomLeftPath.lineTo(screenX * 0.5f, screenY * 0.91f);
        bottomLeftPath.close();
        diagonalEdges.add(new Vector(screenX * 0.5, screenY * 0.91, 0, screenY - screenX * 0.5));

        // Top-right corner (triangle)
        Path topRightPath = new Path();
        topRightPath.moveTo(screenX * 0.5f, 0);
        topRightPath.lineTo(screenX, 0);
        topRightPath.lineTo(screenX, screenX * 0.5f);
        topRightPath.lineTo(screenX * 0.5f, screenY * 0.09f);
        topRightPath.close();
        diagonalEdges.add(new Vector(screenX * 0.5, screenY * 0.09, screenX, screenX * 0.5));

        // Add the corner paths to the paths list
        cornerPaths.add(bottomRightPath);
        cornerPaths.add(topLeftPath);
        cornerPaths.add(bottomLeftPath);
        cornerPaths.add(topRightPath);

        // Initialize a region with the screen dimensions
        Region region = new Region(0, 0, screenX, screenY);

        // Loop through the list of corner paths and add each as a region
        for (Path path : cornerPaths) {
            Region cornerRegion = new Region();
            // Set the path within the bounding region
            cornerRegion.setPath(path, region);
            // Add the corner region to the list
            goalRegions.add(cornerRegion);
        }
    }

    /**
     * Draw the middle circle on the canvas
     * @param canvas the canvas to draw on
     */
    private void drawMiddleCircle(Canvas canvas) {
        // Set up the paint for the circle (donut shape)
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);  // Set the circle's outline color
        paint.setStyle(Paint.Style.STROKE);  // Set it to STROKE to create an open middle circle
        paint.setStrokeWidth(15);  // Set the width of the stroke

        // Get the center of the screen
        float centerX = screenX / 2f;
        float centerY = screenY / 2f;

        // Radius of the circle
        float outerRadius = screenX * 0.07f;

        // Draw the outer circle
        canvas.drawCircle(centerX, centerY, outerRadius, paint);
    }

    /**
     * Draw the settings icon in the middle of the screen
     * @param canvas the canvas to draw on
     */
    private void drawSettingsIcon(Canvas canvas) {
        // Create a new matrix for scaling and translation
        Matrix matrix = new Matrix();
        float scaleFactor = 0.3f;

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

    /**
     * Change the settings of the game
     * @param playerCount the number of players
     * @param playerSpeed the speed of the players
     * @param ballSpeed the speed of the ball
     */
    public void changeSettings(int playerCount, int playerSpeed, int ballSpeed) {
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
        changePlayerSpeed(playerSpeed);
        changeBallSpeed(ballSpeed);
    }

    /**
     * Change the speed of the players
     * @param playerSpeed the new player speed
     */
    private void changePlayerSpeed(int playerSpeed) {
        this.PLAYERSPEED = playerSpeed;
    }

    /**
     * Change the speed of the ball
     * @param ballSpeed the new ball speed
     */
    private void changeBallSpeed(int ballSpeed) {
        this.BALL_SPEED = ballSpeed;
    }

    /**
     * Clear the lists of players, joysticks and shoot buttons
     */
    private void clearLists() {
        synchronized (players) {
            players = new ArrayList<>();
        }
        synchronized (joysticks) {
            joysticks.clear();
        }
        synchronized (shootButtons) {
            shootButtons.clear();
        }
    }

    /**
     * Add a player to the list of players
     * @param player the player to add
     */

    private void addPlayer(Player player) {
        synchronized (players) {
            players.add(player);
        }
    }

    /**
     * Add a joystick to the list of joysticks
     * @param joystick the joystick to add
     */
    private void addJoystick(Joystick joystick) {
        synchronized (joysticks) {
            joysticks.add(joystick);
        }
    }

    /**
     * Add a shoot button to the list of shoot buttons
     * @param shootButton the shoot button to add
     */
    public void addShootButton(ShootButton shootButton) {
        synchronized (shootButtons) {
            shootButtons.add(shootButton);
        }
    }

    /**
     * Initialize player 1 with a joystick and shoot button
     */
    public void initializePlayer1() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(playerPositions[0][0], playerPositions[0][1], PLAYERRADIUS, playerColors[0], 0);
        newJoystick = new Joystick(screenX * 0.78f, screenY - screenX * 0.13f, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.92f, screenY * 0.83f, SHOOTBUTTONRADIUS, playerColors[0]);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    /**
     * Initialize player 2 with a joystick and shoot button
     */

    public void initializePlayer2() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(playerPositions[1][0], playerPositions[1][1], PLAYERRADIUS, playerColors[1], 1);
        newJoystick = new Joystick(screenX * 0.22f, JOYSTICKRADIUS, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.08f, screenY * 0.17f, SHOOTBUTTONRADIUS, playerColors[1]);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    /**
     * Initialize player 3 with a joystick and shoot button
     */
    public void initializePlayer3() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(playerPositions[2][0], playerPositions[2][1], PLAYERRADIUS, playerColors[2], 2);
        newJoystick = new Joystick(screenX * 0.22f, screenY - screenX * 0.13f, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.08f, screenY * 0.83f, SHOOTBUTTONRADIUS, playerColors[2]);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    /**
     * Initialize player 4 with a joystick and shoot button
     */
    public void initializePlayer4() {
        Player newPlayer;
        Joystick newJoystick;
        ShootButton shootButton;
        newPlayer = new Player(playerPositions[3][0], playerPositions[3][1], PLAYERRADIUS, playerColors[3], 3);
        newJoystick = new Joystick(screenX * 0.78f, JOYSTICKRADIUS, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.91f, screenY * 0.17f, SHOOTBUTTONRADIUS, playerColors[3]);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }
}