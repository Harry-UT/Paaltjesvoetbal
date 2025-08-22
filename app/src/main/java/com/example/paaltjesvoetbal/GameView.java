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
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Matrix;
import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Handler;
import android.os.Looper;

import com.example.paaltjesvoetbal.model.Ball;
import com.example.paaltjesvoetbal.model.FloatingText;
import com.example.paaltjesvoetbal.model.Joystick;
import com.example.paaltjesvoetbal.model.Player;
import com.example.paaltjesvoetbal.model.ShootButton;
import com.example.paaltjesvoetbal.model.Star;
import com.example.paaltjesvoetbal.model.Team;
import com.example.paaltjesvoetbal.model.Vector;


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
    private List<Team> teams = new ArrayList<>();
    private final List<Ball> balls;
    private final List<ShootButton> shootButtons;
    private final int[] playerColors = {Color.BLUE, Color.RED, Color.GREEN, 0xFFFFEB04};
    private final int[][] playerPositions;
    private int PLAYERSPEED = 4;
    private int BALL_SPEED = 18;
    private final int PLAYERRADIUS = 30;
    private final int BALLRADIUS = 15;
    private final int JOYSTICKRADIUS = 95;
    private final int SHOOTBUTTONRADIUS = 50;
    private int PLAYERCOUNT = 4;

    private final double goalWidth = 0.5;
    private final List<Vector> diagonalEdges = new ArrayList<>();
    private final List<Vector> verticalGoalEdges = new ArrayList<>();
    private final List<Vector> bounceEdges = new ArrayList<>(); // The lines which the ball bounces off
    private final List<Vector> goalLines = new ArrayList<>(); // The lines which the ball crosses to score a goal
    private final List<Path> cornerPaths = new ArrayList<>(); // The circumference of each goal area
    private final List<Region> goalRegions = new ArrayList<>(); // The goal areas for each player

    private long lastBounceTime = 0;
    private float lastGoalTime;
    private Player lastShooter;
    private int lastGoal;
    private boolean scored = false;
    private final Star[] stars = new Star[40];
    private long splashStartTime = 0;
    private final FloatingText goalText;
    private FloatingText scoreIncrementText;
    public SoundManager soundManager;
    private final int TARGET_FPS = 60;
    private int fps;
    private boolean onlineMode = false;
    private boolean twoVtwoMode = false;
    private String username;
    private ClientConnection clientConnection;
    private InetAddress server;
    private int port = 3000;

    /**
     * Constructor for the GameView class
     * @param context the context of the game
     * @param screenX the width of the screen
     * @param screenY the height of the screen
     */
    public GameView(Context context, int screenX, int screenY) {
        super(context);
        soundManager = SoundManager.getInstance(context);

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
        determineGoalRegions();

        // Determine edges for ball bounce
        determineBounceEdges();

        // Determine the goals
        determineGoals();

        // Determine score text positions
        determineScoreTextPositions();

        // Initialize ball(s)
        balls = new ArrayList<>();
        Ball ball = new Ball(screenX / 2f, screenY / 2f, BALLRADIUS, bounceEdges, verticalGoalEdges);
        balls.add(ball);

        // Initialize stars for goal animation
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
        bounceEdges.clear();
        for (Vector edge : diagonalEdges) {
            Vector[] bounceVectors = edge.split(goalWidth);
            bounceEdges.addAll(Arrays.asList(bounceVectors));
        }
    }

    private void determineBounceEdgesTwovTwo() {
        bounceEdges.clear();
        // Bottom-left bounce edge
        Vector bottomLeft = new Vector(0, screenY - screenX * 0.3f, screenX * 0.35, screenY - screenX * 0.3f);
        // Bottom-right bounce edge
        Vector bottomRight = new Vector(screenX * 0.65f, screenY - screenX * 0.3f, screenX, screenY - screenX * 0.3f);
        // Top-left bounce edge
        Vector topLeft = new Vector(0, screenX * 0.3f, screenX * 0.35f, screenX * 0.3f);
        // Top-right bounce edge
        Vector topRight = new Vector(screenX * 0.65f, screenX * 0.3f, screenX, screenX * 0.3f);
        bounceEdges.add(bottomLeft);
        bounceEdges.add(bottomRight);
        bounceEdges.add(topLeft);
        bounceEdges.add(topRight);
    }

    /**
     * Initialize the first player object
     */
    private void determineGoals() {
        goalLines.clear();
        // Iterate over all diagonal edges to determine the goals
        for (Vector edge : diagonalEdges) {
            // Get scaled vector
            Vector scaledEdge = edge.getCenteredAndScaledVector(goalWidth);
            goalLines.add(scaledEdge);
        }
    }

    private void determineGoalsTwovTwo() {
        // Bottom blue goal
        Vector bottomGoal = new Vector(screenX * 0.35f, screenY - screenX * 0.3f, screenX * 0.65f, screenY - screenX * 0.3f);
        Vector topGoal = new Vector(screenX * 0.35f, screenX * 0.3f, screenX * 0.65f, screenX * 0.3f);
        goalLines.clear();
        goalLines.add(bottomGoal);
        goalLines.add(topGoal);
    }

    /**
     * Determine the positions for the score text based on the screen dimensions
     */
    private void determineScoreTextPositions() {
        for (Player player : players) {
            int index = players.indexOf(player);

            // Calculate text rotation angle
            Vector goal = goalLines.get(index);

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

    private void determineScoreTextPositionsTwovTwo() {
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            int xText = screenX / 2;
            int yText;
            if (i == 1) {
                yText = (int) (screenX * 0.4f);
            } else {
                yText = (int) (screenY - screenX * 0.4f);
            }
            team.setScorePosition(xText, yText);
        }
    }

    /**
     * Update the game state
     */
    private void update() {
        synchronized (balls) {
            for (Ball ball : balls) {
                lastBounceTime = ball.update(screenX, screenY, PLAYERCOUNT < 4 ? goalLines.subList(2,4) : null, twoVtwoMode);
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
        soundManager.playGoalSound();
        // Scored in goal by player
        player.incrementScore();

        if (twoVtwoMode) {
            int teamIndex = (players.indexOf(player) == 0 || players.indexOf(player) == 2) ? 0 : 1;
            // Only allow scoring in the opponent's goal
            if ((teamIndex == 0 && goal == 0) || (teamIndex == 1 && goal == 1)) {
                // Prevent scoring in own goal
                return;
            }

            if (players.indexOf(player) == 0 || players.indexOf(player) == 2) {
                teams.get(0).incrementScore();
            } else {
                teams.get(1).incrementScore();
            }
        } else {
            player.incrementScore();
        }

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
     * @param ball the ball object to check
     */
    private void checkGoal(Ball ball) {
        long now = System.currentTimeMillis();
        float ballX = ball.getX();
        float ballY = ball.getY();
        Player shooter = ball.getShooter();

        for (int i = 0; i < goalRegions.size(); i++) {
            Region region = goalRegions.get(i);

            if (!region.contains((int) ballX, (int) ballY)) {
                continue;
            }

            if (lastGoal == i && shooter != null) {
                if (now - lastGoalTime < 200) {
                    int shooterIndex = players.indexOf(shooter);

                    // Check if the shooter is not the same as the last shooter and within bounds
                    if (i != shooter.getNumber() && i < players.size()) {
                        // Scored in goal by player!
                        scored(i, shooter);
                        lastShooter = shooter;

                        int rotation = (shooterIndex == 1 || shooterIndex == 3) ? 180 : 0;
                        scoreIncrementText = new FloatingText(
                                shooter.getScorePosition()[0],
                                shooter.getScorePosition()[1],
                                40,
                                rotation
                        );

                        for (Star star : stars) {
                            star.setColor(shooter.getColor());
                        }
                    }

                    lastGoal = -1;
                    ball.resetShooter();
                    break;
                }
            } else {
                lastGoalTime = now;
                lastGoal = i;
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

            // Draw the goal regions
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
                            if (twoVtwoMode) {
                                paint.setColor(Color.argb(140, 0, 0, 255)); // Light blue
                            } else {
                                paint.setColor(Color.rgb(140, 238, 144)); // Light green
                            }
                            canvas.drawPath(cornerPaths.get(i), paint);
                            break;
                        case 3:
                            if (twoVtwoMode) {
                                paint.setColor(Color.argb(128, 255, 0, 0)); // Light red
                            } else {
                                paint.setColor(Color.argb(140, 255, 255, 0)); // Light yellow
                            }
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
            if (!twoVtwoMode) {
                synchronized (verticalGoalEdges) {
                    Paint paint = new Paint();
//                paint.setColor(Color.rgb(255, 255, 255));  // White color
                    paint.setColor(Color.BLACK);
                    paint.setStrokeWidth(5);
                    for (Vector edge : verticalGoalEdges) {
                        canvas.drawLine((float) edge.getX1(), (float) edge.getY1(), (float) edge.getX2(), (float) edge.getY2(), paint);
                    }
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

//            synchronized (goalLines) {
//                for (Vector goal : goalLines) {
//                    goal.draw(canvas);
//                }
//            }

            if (PLAYERCOUNT < 4) {
                // Draw unused goal lines when in less than 4 player mode
                if (PLAYERCOUNT == 2) {
                    goalLines.get(2).draw(canvas);
                    goalLines.get(3).draw(canvas);
                } else if (PLAYERCOUNT == 3) {
                    goalLines.get(3).draw(canvas);
                }
            }

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
        int postSide = 1; // To alternate between the two posts of each goal
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        for (int i = 0; i < PLAYERCOUNT * 2; i++) {
            Vector bounceEdge = bounceEdges.get(i);
            // Get the goalpost coordinates
            // Use mod 2 to alternate between first or second coordinate of edge
            if (postSide % 2 == 0) {
                float x1 = (float) bounceEdge.getX1();
                float y1 = (float) bounceEdge.getY1();
                // Draw a little black dot
                canvas.drawCircle(x1, y1, 5, paint);
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

        // Draw the scores of the players
        if (!twoVtwoMode) {
            synchronized (players) {
                for (Player player : players) {
                    int index = players.indexOf(player);

                    // Calculate text rotation angle
                    Vector goal = goalLines.get(index);

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
        } else { // draw scores of the 2 teams
            synchronized (teams) {
                for (Team team : teams) {
                    // Set text color
                    paint.setColor(team.getColor());

                    // Save the current canvas state
                    canvas.save();

                    // Rotate around the text position
                    canvas.rotate((teams.indexOf(team) == 0 ? 0 : 180), team.getScorePositionX(), team.getScorePositionY());

                    // Draw the text
                    canvas.drawText(String.valueOf(team.getScore()), team.getScorePositionX(), team.getScorePositionY(), paint);

                    // Restore canvas to avoid affecting other drawings
                    canvas.restore();
                }
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
                        SettingsDialog settingsDialog = new SettingsDialog(getContext(), this, players.size(), PLAYERSPEED, BALL_SPEED, onlineMode, twoVtwoMode);
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
                                        soundManager.playShootSound();
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
     * Calculate the shortest distance from a point (player) to a line (edge)
     * @param edge the line represented as a Vector
     * @param player the player object representing the point
     * @return the shortest distance from the point to the line
     */
    private double pointToLineDist(Vector edge, Player player) {
        double x0 = player.getX();
        double y0 = player.getY();
        double x1 = edge.getX1();
        double y1 = edge.getY1();
        double x2 = edge.getX2();
        double y2 = edge.getY2();
        double numerator = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1);
        double denominator = Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
        return numerator / denominator;
    }

    /**
     * Calculate the shortest distance from a point (player) to a line segment (edge)
     * @param edge the line segment represented as a Vector
     * @param player the player object representing the point
     * @return the shortest distance from the point to the line segment
     */
    private double pointToLineSegmentDistOwnGoal(Vector edge, Player player) {
        double x0 = player.getX(), y0 = player.getY();
        double x1 = edge.getX1(), y1 = edge.getY1();
        double x2 = edge.getX2(), y2 = edge.getY2();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) return Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1));

        double t = ((x0 - x1) * dx + (y0 - y1) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t)); // Clamp t to [0,1]

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.sqrt((x0 - projX) * (x0 - projX) + (y0 - projY) * (y0 - projY));
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

                // Clamp player position within field bounds (not in corners)
                for (int i = 0; i < bounceEdges.size(); i++) {
                    Vector edge = bounceEdges.get(i);
                    float dist = 1000f; // Initialize with a large distance
                    int edgeIndex = i;

                    switch (players.indexOf(player)) {
                        case 0:
                            dist = (edgeIndex == 0 || edgeIndex == 1) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        case 1:
                            dist = (edgeIndex == 2 || edgeIndex == 3) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        case 2:
                            dist = (edgeIndex == 4 || edgeIndex == 5) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        case 3:
                            dist = (edgeIndex == 6 || edgeIndex == 7) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        default:
                            dist = (float) pointToLineDist(edge, player);
                            break;
                    }
                    dist = (float) pointToLineSegmentDistOwnGoal(edge, player);

                    if (dist < player.getRadius()) {
//                        Log.d("EdgeClamp", "Player " + players.indexOf(player) + " clamped at edge " + i + ", distance: " + dist);

                        double dx = edge.getX2() - edge.getX1();
                        double dy = edge.getY2() - edge.getY1();
                        double len = Math.sqrt(dx * dx + dy * dy);

                        // Wall normal
                        double nx = -dy / len;
                        double ny = dx / len;

                        // Flip normal for specific edges
                        if ((i == 0 || i == 1) && twoVtwoMode) {
                            nx = -nx;
                            ny = -ny;
                        }

                        // Only project if moving INTO the wall
                        double moveDot = moveX * nx + moveY * ny;
                        if (moveDot < 0) {
                            moveX -= (float) (moveDot * nx);
                            moveY -= (float) (moveDot * ny);
                        }

                        newX = player.getX() + (float) moveX;
                        newY = player.getY() + (float) moveY;
                    }
                }


                // Clamp player position within screen bounds
                newX = Math.max(player.getRadius(), Math.min(newX, screenX - player.getRadius()));
                newY = Math.max(player.getRadius(), Math.min(newY, screenY - player.getRadius()));

                // Set the final position after checking bounds
                player.setX(newX);
                player.setY(newY);
            }
        }
//        if (!twoVtwoMode) {
//            determineScoreTextPositions();
//        } else {
//            determineScoreTextPositionsTwovTwo();
//        }
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
    }

    /**
     * Determine the corner areas of the screen
     */
    private void determineGoalRegions() {
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
        cornerPaths.clear();
        cornerPaths.add(bottomRightPath);
        cornerPaths.add(topLeftPath);
        cornerPaths.add(bottomLeftPath);
        cornerPaths.add(topRightPath);

        // Initialize a region with the screen dimensions
        Region region = new Region(0, 0, screenX, screenY);

        // Loop through the list of corner paths and add each as a region
        goalRegions.clear();
        for (Path path : cornerPaths) {
            Region cornerRegion = new Region();
            // Set the path within the bounding region
            cornerRegion.setPath(path, region);
            // Add the corner region to the list
            goalRegions.add(cornerRegion);
        }
    }

    private void determineGoalRegionsTwovTwo() {
        // Bottom blue goal
        Path bottomPath = new Path();
        bottomPath.moveTo(0, screenY - screenX * 0.3f);
        bottomPath.lineTo(screenX, screenY - screenX * 0.3f);
        bottomPath.lineTo(screenX, screenY);
        bottomPath.lineTo(0, screenY);
        bottomPath.close();
        diagonalEdges.add(new Vector(screenX, screenY - screenX * 0.5, screenX * 0.5, screenY * 0.91));

        // Top red goal
        Path topPath = new Path();
        topPath.moveTo(0, 0);
        topPath.lineTo(screenX, 0);
        topPath.lineTo(screenX, screenX * 0.3f);
        topPath.lineTo(0, screenX * 0.3f);
        topPath.close();
        diagonalEdges.add(new Vector(0, screenX * 0.5, screenX * 0.5, screenY * 0.09));

        // Add the goal paths to the paths list
        cornerPaths.clear();
        cornerPaths.add(bottomPath);
        cornerPaths.add(topPath);

        // Initialize a region with the screen dimensions
        Region region = new Region(0, 0, screenX, screenY);

        // Loop through the list of corner paths and add each as a region
        goalRegions.clear();
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
    public void changeSettings(int playerCount, int playerSpeed, int ballSpeed, boolean online, boolean twoVtwo, boolean isReset) {
        PLAYERCOUNT = playerCount;
        if (isReset) {
            for (Ball ball: balls) {
                ball.reset((int) (screenX / 2f), (int) (screenY / 2f));
            }
        }
        Log.d("SettingsDialog", "Change settings called");
        if (!online) {
            if (twoVtwo) {
                twoVtwoMode = true;
                // If two vs two mode is enabled, we need to ensure there are 4 players
                clearLists();
                initializePlayer1();
                initializePlayer2();
                initializePlayer3();
                initializePlayer4();
                teams.add(new Team(players.get(0), players.get(2)));
                teams.get(0).setColor(Color.BLUE);
                teams.add(new Team(players.get(1), players.get(3)));
                teams.get(1).setColor(Color.RED);

                // Reset player colors for two vs two mode
                for (Player player : players) {
                    if (player.getColor() == Color.GREEN) {
                        player.setColor(Color.BLUE);
                    }
                    if (player.getColor() == 0xFFFFEB04) {
                        player.setColor(Color.RED);
                    }
                }
                // Reset shoot button colors for two vs two mode
                for (ShootButton button : shootButtons) {
                    if (button.getColor() == Color.GREEN) {
                        button.setColor(Color.BLUE);
                    }
                    if (button.getColor() == 0xFFFFEB04) {
                        button.setColor(Color.RED);
                    }
                }

                determineScoreTextPositionsTwovTwo();
                determineGoalRegionsTwovTwo();
                determineBounceEdgesTwovTwo();
                determineGoalsTwovTwo();
                playerCount = 4;
            } else {
                if (twoVtwoMode) {
                    // Reset player scores
                    determineGoalRegions();
                    determineBounceEdges();
                    determineGoals();
                    for (Team team : teams) {
                        team.resetScore();
                    }
                    for (Player player : players) {
                        player.resetScore();
                    }
                }
                twoVtwoMode = false;
                for (Player player : players) {
                    player.setTeam(false);
                }
                for (int i = 0; i < players.size(); i++) {
                    if (i == 2) {
                        players.get(i).setColor(Color.GREEN);
                        players.get(i).getShootButton().setColor(Color.GREEN);
                    }
                    if (i == 3) {
                        players.get(i).setColor(0xFFFFEB04); // Yellow
                        players.get(i).getShootButton().setColor(0xFFFFEB04); // Yellow
                    }
                }
            }
            // Enter offline mode
            Log.d("SettingsDialog", "Offline mode");
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
        } else if (!onlineMode && online) {
            // Reset the game state for online mode
            try {
                server = InetAddress.getByName("192.168.56.1");
                this.clientConnection = new ClientConnection(server, port);
            } catch (IOException e) {
                Log.d("ClientConnection", "Error creating client connection: " + e.getMessage());
            }
            if (clientConnection != null) {
                clientConnection.setChatClient(this);
                login();
            } else {
                Log.d("ClientConnection", "Client connection is null, cannot set chat client");
            }
        }
        this.onlineMode = online;
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

    public void handleDisconnect() {

    }

    public void receiveUpdate(String message, String sender, String timestamp) {
        // Handle incoming messages from the server
        // For example, update player positions or scores based on the message
    }

    private void login() {

    }

    public void determineUsername() {
        System.out.print("Enter your username: ");
        boolean set = false;
        while (!set) {
//            String username = input.nextLine();
            if (username.length() > 1) {
                if (clientConnection.login(username)) {
                    set = true;
                } else {
                    System.out.print(
                            "Username already existed or was too short, enter a new one: ");
                }
            }
        }
    }

    public void sendMessage() {
        String message = "";
        System.out.print("Enter desired receiver: ");
//        String receiver = input.nextLine();
        System.out.print("Enter message: ");
//        String message = input.nextLine();
        System.out.println("Sending message");
        System.out.println(getUsername());
        LocalDateTime now = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            now = LocalDateTime.now();
        }
        assert now != null;
        String timestamp = now.toString();  // Converts to ISO format
        clientConnection.sendMessage(message, timestamp); // gets the current timestamp in ISO format
    }

    private String getUsername() {
        return username;
    }
}