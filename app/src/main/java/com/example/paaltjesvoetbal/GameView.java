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
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Matrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

/**
 * The GameView class is responsible for updating and rendering the game elements on the screen
 */
@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private final Bitmap settingsIcon = BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon);
    private Bitmap[] starTypes = new Bitmap[4]; // Array to store the 4 star PNGs
    private List<ImageView> stars = new ArrayList<>(); // List to store stars
    private boolean isPlaying;
    private final SurfaceHolder holder;
    private final int screenX;
    private final int screenY;
    private final List<Joystick> joysticks;
    private List<Player> players;
    private final List<Ball> balls;
    private final List<ShootButton> shootButtons;
    private int PLAYERSPEED = 6;
    private int BALL_SPEED = 20;
    private final int PLAYERRADIUS = 40;
    private final int BALLRADIUS = 20;
    private final int JOYSTICKRADIUS = 95;
    private final int SHOOTBUTTONRADIUS = 50;
    private final int PLAYERCOUNT = 2;
    private final double goalWidth = 0.5;
    private final List<Vector> diagonalEdges = new ArrayList<>();
    private final List<Vector> verticalGoalEdges = new ArrayList<>();
    private final List<Vector> bounceEdges = new ArrayList<>();
    private final List<Vector> goals = new ArrayList<>();
    private final List<Path> cornerPaths = new ArrayList<>();
    private final List<Region> goalRegions = new ArrayList<>();
    private float lastGoalTime;
    private int lastGoal;
    private final int TARGET_FPS = 65;

    public GameView(Context context, int screenX, int screenY) {
        super(context);
        setKeepScreenOn(true);
        this.screenX = screenX;
        this.screenY = screenY;
        holder = getHolder();

        // Initialize players, joysticks and shoot buttons
        players = new ArrayList<>();
        joysticks = new ArrayList<>();
        shootButtons = new ArrayList<>();

        for (int i = 0; i < PLAYERCOUNT; i++) {
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
        // Determine player corner areas
        determineGoalEdges();

        // Determine edges for ball bounce
        determineBounceEdges();

        // Determine the goals
        determineGoals(goalWidth);

        // Initialize ball(s)
        balls = new ArrayList<>();
        Ball ball = new Ball(screenX / 2f, screenY / 2f, BALLRADIUS, bounceEdges, verticalGoalEdges);
        balls.add(ball);
    }

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
     * Determine the goal regions based on the screen dimensions
     * @param goalWidth the width of the goal region
     */
    private void determineGoals(double goalWidth) {
        // Iterate over all diagonal edges to determine the goals
        for (Vector edge : diagonalEdges) {
            // Get scaled vector
            Vector scaledEdge = edge.getCenteredAndScaledVector(goalWidth);
            goals.add(scaledEdge);
        }
    }

    private void update() {
        synchronized (balls) {
            for (Ball ball : balls) {
                ball.update(screenX, screenY);
                // Log presence of shooter for ball
                Log.d("Ball", "Ball shooter: " + ball.getShooter());
                checkGoal(ball);
            }
        }
        checkPlayerBallCollision();
        updatePlayers();
    }

    // Check if the ball went into a goal
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
                            scored(i, ball.getShooter());
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

    private void scored(int goal, Player player) {
        // Scored in goal by player
        player.scored();
        switch (player.getColor()) {
            case Color.BLUE:
                Log.d("Goal", "Player 0 scored in goal " + goal);
                break;
            case Color.RED:
                Log.d("Goal", "Player 1 scored in goal " + goal);
                break;
            case Color.GREEN:
                Log.d("Goal", "Player 2 scored in goal " + goal);
                break;
            case Color.YELLOW:
                Log.d("Goal", "Player 3 scored in goal " + goal);
                break;
            default:
                break;
        }
        Log.d("Goal", "Player " + player + " scored in goal " + goal);
        displayGoalAnimation();
    }

    public void displayGoalAnimation() {}

    /**
     * Draw the game elements on the canvas
     */
    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            drawPlayground(canvas);

//            synchronized (goals) {
//                for (Vector goal : goals) {
//                    goal.draw(canvas);
//                }
//            }

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

            holder.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Draw the scores of the players on the screen
     * @param canvas the canvas to draw on
     */
    private void drawScores(Canvas canvas) {
        Paint paint = new Paint();
        paint.setTextSize(50);
        int xText = 0;
        int yText = 0;
        for (Player player : players) {
            paint.setColor(player.getColor());
            switch (players.indexOf(player)) {
                case 0:
                    xText = (int) (0.7 * screenX);
                    yText = (int) (screenY * 0.7);
                    break;
                case 1:
                    xText = (int) (0.3 * screenX);
                    yText = (int) (screenY * 0.3);
                    break;
                case 2:
                    xText = (int) (0.3 * screenX);
                    yText = (int) (0.7 * screenY);
                    break;
                case 3:
                    xText = (int) (0.7 * screenX);
                    yText = (int) (0.3 * screenY);
                    break;
                default:
                    break;
            }
            canvas.drawText(""+player.getScore(), xText, yText, paint);
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
        canvas.drawColor(Color.parseColor("#FFEBCD"));

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
        // Top-left corner (triangle)
        Path topLeftPath = new Path();
        topLeftPath.moveTo(0, 0);
        topLeftPath.lineTo(screenX * 0.5f, 0);
        topLeftPath.lineTo(screenX * 0.5f, screenY * 0.09f);
        topLeftPath.lineTo(0, screenX * 0.5f);
        topLeftPath.close();
        // Draw the path on the canvas
        diagonalEdges.add(new Vector(0, screenX * 0.5, screenX * 0.5, screenY * 0.09));

        verticalGoalEdges.add(new Vector(screenX * 0.5, 0, screenX * 0.5, screenY * 0.09));

        // Top-right corner (triangle)
        Path topRightPath = new Path();
        topRightPath.moveTo(screenX * 0.5f, 0);
        topRightPath.lineTo(screenX, 0);
        topRightPath.lineTo(screenX, screenX * 0.5f);
        topRightPath.lineTo(screenX * 0.5f, screenY * 0.09f);
        topRightPath.close();
        diagonalEdges.add(new Vector(screenX * 0.5, screenY * 0.09, screenX, screenX * 0.5));

        // Bottom-right corner (triangle)
        Path bottomRightPath = new Path();
        bottomRightPath.moveTo(screenX, screenY - screenX * 0.5f);
        bottomRightPath.lineTo(screenX, screenY);
        bottomRightPath.lineTo(screenX * 0.5f, screenY);
        bottomRightPath.lineTo(screenX * 0.5f, screenY * 0.91f);
        bottomRightPath.close();
        diagonalEdges.add(new Vector(screenX, screenY - screenX * 0.5, screenX * 0.5, screenY * 0.91));

        verticalGoalEdges.add(new Vector(screenX * 0.5, screenY * 0.91, screenX * 0.5, screenY));

        // Bottom-left corner (triangle)
        Path bottomLeftPath = new Path();
        bottomLeftPath.moveTo(screenX * 0.5f, screenY);
        bottomLeftPath.lineTo(0, screenY);
        bottomLeftPath.lineTo(0, screenY - screenX * 0.5f);
        bottomLeftPath.lineTo(screenX * 0.5f, screenY * 0.91f);
        bottomLeftPath.close();
        diagonalEdges.add(new Vector(screenX * 0.5, screenY * 0.91, 0, screenY - screenX * 0.5));

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
    
    private void drawMiddleCircle(Canvas canvas) {
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
    private void changePlayerSpeed(int playerSpeed) {
        this.PLAYERSPEED = playerSpeed;
    }

    private void changeBallSpeed(int ballSpeed) {
        this.BALL_SPEED = ballSpeed;
    }

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
        newPlayer = new Player(screenX * 0.75f, screenY * 0.78f, PLAYERRADIUS, Color.BLUE, 0);
        newJoystick = new Joystick(screenX * 0.78f, screenY - screenX * 0.13f, JOYSTICKRADIUS);
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
        newPlayer = new Player(screenX * 0.25f, screenY * 0.22f, PLAYERRADIUS, Color.RED, 1);
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
        newPlayer = new Player(screenX * 0.25f, screenY * 0.78f, PLAYERRADIUS, Color.GREEN, 2);
        newJoystick = new Joystick(screenX * 0.22f, screenY - screenX * 0.13f, JOYSTICKRADIUS);
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
        newPlayer = new Player(screenX * 0.75f, screenY * 0.22f, PLAYERRADIUS, Color.YELLOW, 3);
        newJoystick = new Joystick(screenX * 0.78f, JOYSTICKRADIUS, JOYSTICKRADIUS);
        shootButton = new ShootButton(screenX * 0.91f, screenY * 0.17f, SHOOTBUTTONRADIUS, Color.YELLOW);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }
}