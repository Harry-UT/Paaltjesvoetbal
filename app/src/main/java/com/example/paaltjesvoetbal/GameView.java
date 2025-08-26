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

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import android.os.Handler;
import android.os.Looper;

import com.example.paaltjesvoetbal.model.Ball;
import com.example.paaltjesvoetbal.model.FloatingText;
import com.example.paaltjesvoetbal.model.GoalPost;
import com.example.paaltjesvoetbal.model.Joystick;
import com.example.paaltjesvoetbal.model.Player;
import com.example.paaltjesvoetbal.model.ShootButton;
import com.example.paaltjesvoetbal.model.Star;
import com.example.paaltjesvoetbal.model.Team;
import com.example.paaltjesvoetbal.model.Vector;
import com.example.paaltjesvoetbal.networking.Protocol;


/**
 * The GameView class is responsible for updating and rendering the game elements on the screen
 */
@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private final Bitmap settingsIcon = BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon);
    private Bitmap staticLayer;
    private Canvas staticCanvas;
    private Bitmap staticLayerTwovTwo;
    private Canvas staticCanvasTwovTwo;
    private boolean isPlaying;
    private final SurfaceHolder holder;
    private final int screenX;
    private final int screenY;
    private final List<Joystick> joysticks;
    private final List<Player> players;
    private final List<Team> teams = new ArrayList<>();
    private final List<Ball> balls;
    private final List<ShootButton> shootButtons;
    private final int[] playerColors = {Color.BLUE, Color.RED, Color.GREEN, 0xFFFFEB04};
    private final int[][] playerPositions;
    private int PLAYERSPEED = 4;
    private int BALL_SPEED = 18;
    private final int PLAYERRADIUS = 30;
    private final int BALLRADIUS = 15;
    private final int JOYSTICK_OUTERRADIUS = 95;
    private final int JOYSTICK_INNERRADIUS = 30;
    private final int SHOOTBUTTONRADIUS = 50;
    private final int GOALPOSTRADIUS = 5;
    private int PLAYERCOUNT = 4;
    private static final long BALL_BOUNCE_COOLDOWN_MS = 100; // 100ms cooldown
    private static final float BALL_DAMPING_FACTOR = 0.985F;

    private final boolean debug = false;

    private final double goalWidth = 0.5;
    private final List<Vector> diagonalEdges = new ArrayList<>();
    private final List<Vector> verticalGoalEdges = new ArrayList<>();
    private final List<Vector> bounceEdges = new ArrayList<>(); // The lines which the ball bounces off
    private final List<Vector> bounceEdgesTwovTwo = new ArrayList<>(); // The lines which the ball bounces off in 2v2 mode
    private final List<Vector> goalLines = new ArrayList<>(); // The lines which the ball crosses to score a goal
    private final List<Vector> goalLinesTwovTwo = new ArrayList<>(); // The lines which the ball crosses to score a goal in 2v2 mode
    private final List<GoalPost> goalPosts = new ArrayList<>(); // The goal posts at the corners of the goals
    private final List<GoalPost> goalPostsTwovTwo = new ArrayList<>(); // The goal posts at the corners of the goals in 2v2 mode
    private final List<Path> cornerPaths = new ArrayList<>(); // The circumference of each goal area
    private final List<Path> cornerPathsTwovTwo = new ArrayList<>(); // The circumference of each goal area in 2v2 mode
    private final List<Region> goalRegions = new ArrayList<>(); // The goal areas for each player
    private final List<Region> goalRegionsTwovTwo = new ArrayList<>(); // The goal areas for each player in 2v2 mode
    private final ReentrantLock pingLock = new ReentrantLock();
    private final Condition pingReceived = pingLock.newCondition();
    private boolean pingResponse = false;
    private long lastBounceTime = 0;
    private float lastGoalTime;
    private Player lastShooter;
    private int lastGoal;
    private boolean scored = false;
    private final Star[] stars = new Star[40];
    private long splashStartTime = 0;
    private final FloatingText GOALText;
    private FloatingText scoreIncrementText;
    public SoundManager soundManager;
    private final int TARGET_FPS = 60;
    private int fps;
    private boolean onlineMode = false;
    private boolean twoVtwoMode = false;
    private final Paint goalPaintBlue = new Paint();
    private final Paint goalPaintRed = new Paint();
    private final Paint goalPaintGreen = new Paint();
    private final Paint goalPaintYellow = new Paint();
    private final List<Paint> scoresPaints = new ArrayList<>();
    private final Paint teamScoresPaint = new Paint();
    private final Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.bungee);
    private final List<int[]> scorePositions = new ArrayList<>(); // Contains the x and y of the score text per player
    private final List<Float> scoreRotations = new ArrayList<>(); // Contains the rotation of the score text per player
    private final Paint edgePaint = new Paint();
    private final Paint fpsPaint = new Paint();
    private final Paint goalPostPaint = new Paint();
    private final Paint middleCirclePaint = new Paint();
    private final Matrix settingsMatrix = new Matrix();
    private final Paint GOALtextPaint = new Paint();
    private final Paint scoreIncrementPaint = new Paint();
    float outerRadius;
    private String username;
    private ClientConnection clientConnection;
    private InetAddress server;
    private final int port = 3000;
    private boolean needSync = false;
    private final float PPCM;

    /**
     * Constructor for the GameView class
     * @param context the context of the game
     * @param screenX the width of the screen
     * @param screenY the height of the screen
     */
    public GameView(Context context, int screenX, int screenY, int dpi) {
        super(context);
        Log.d("Resolution", "ScreenX: " + screenX + ", ScreenY: " + screenY);
        Log.d("Resolution", "dpi: " + dpi);
        PPCM = dpi / 2.54f; // pixels per centimeter
        Log.d("Resolution", "ppcm: " + PPCM);
        soundManager = SoundManager.getInstance(context);

        setKeepScreenOn(true);
        this.screenX = screenX;
        this.screenY = screenY;
        this.GOALText = new FloatingText((int) (screenX / 2f), (int) (screenY / 2f), 60, 0);
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
        determineGoalRegionsTwovTwo();

        // Determine edges for ball bounce
        determineBounceEdges();
        determineBounceEdgesTwovTwo();

        // Determine the goals
        determineGoalLines();
        determineGoalLinesTwovTwo();

        // Determine goal posts
        determineGoalPosts();
        determineGoalPostsTwovTwo();

        // Determine score text positions
        determineScoreTextPositions();

        // Initialize ball(s)
        balls = new ArrayList<>();
        Ball ball = new Ball(screenX / 2f, screenY / 2f, BALLRADIUS);
        balls.add(ball);

        // Initialize stars for goal animation
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star();
        }

        initPaints(); // Initialize all paint stuff for performance improvement
    }

    /**
     * Initialize all Paint objects used in the game
     */
    private void initPaints() {
        // Initialize goal paints
        goalPaintBlue.setColor(Color.argb(140, 0, 0, 255));
        goalPaintRed.setColor(Color.argb(128, 255, 0, 0));
        goalPaintGreen.setColor(Color.rgb(140, 238, 144));
        goalPaintYellow.setColor(Color.argb(140, 255, 255, 0));

        // Initialize edge paint
        edgePaint.setColor(Color.BLACK);
        edgePaint.setStrokeWidth(5);

        // Initialize fps paint
        fpsPaint.setColor(Color.RED);
        fpsPaint.setTextSize(40);

        // Initialize goal text paint
        float scaleFactor = 0.3f;
        // Desired position for the icon (center of the screen)
        float centerX = screenX / 2f;
        float centerY = screenY / 2f;
        // Apply scaling to the matrix
        settingsMatrix.postScale(scaleFactor, scaleFactor);
        // Compute the dimensions of the scaled icon
        float scaledWidth = settingsIcon.getWidth() * scaleFactor;
        float scaledHeight = settingsIcon.getHeight() * scaleFactor;
        // Adjust the translation to account for the scaled dimensions
        settingsMatrix.postTranslate(centerX - (scaledWidth / 2), centerY - (scaledHeight / 2));
        // Set up the paint for the circle (donut shape)
        middleCirclePaint.setColor(Color.BLACK);
        middleCirclePaint.setStrokeWidth(15);
        middleCirclePaint.setStyle(Paint.Style.STROKE);  // Set it to STROKE to create an open middle circle
        outerRadius = screenX * 0.07f;

        goalPostPaint.setColor(Color.BLACK);

        // Initialize goal text animation paint
        for (int i = 0; i < 4; i++) {
            Paint scorePaint = new Paint();
            scorePaint.setTextSize(60);
            scorePaint.setAntiAlias(true); // Smooth text edges
            scorePaint.setTypeface(typeface); // Set custom font
            scoresPaints.add(scorePaint);
        }

        // Initialize team score paint
        teamScoresPaint.setTextSize(60);
        teamScoresPaint.setTypeface(typeface); // Set custom font

        // Initialize goal text animation paint
        int xText;
        int yText;
        for (int index = 0; index < 4; index++) {
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

            // Calculate text position
            xText = (int) (middleX + dxPerpendicular * screenX * 0.25);
            yText = (int) (middleY + dyPerpendicular * screenX * 0.25);
            // Center text with text measure
            float textWidth = scoresPaints.get(index).measureText(String.valueOf(players.get(index).getScore()));
            switch (index) {
                case 0:
                    xText -= (int) (textWidth / 2);
                    break;
                case 1:
                    xText += (int) (textWidth / 2);
                    break;
                case 2:
                    // Use ascend and descent to center text vertically
                    yText += (int) ((scoresPaints.get(index).descent() + scoresPaints.get(index).ascent()) / 2);
                    xText -= (int) (textWidth / 4);
                    break;
                case 3:
                    // Use ascend and descent to center text vertically
                    yText += (int) ((scoresPaints.get(index).descent() + scoresPaints.get(index).ascent()) / 2 + (int) (0.029f * screenY));
                    xText += (int) (textWidth / 4);
                    break;
                default:
                    break;
            }
            // Set text color
            scoresPaints.get(index).setColor(players.get(index).getColor());
            scorePositions.add(new int[]{xText, yText});
            scoreRotations.add(rotationAngle);
        }

        GOALtextPaint.setTextSize(GOALText.getSize());
        GOALtextPaint.setTypeface(typeface);

        // Measure the width of the text
        scoreIncrementPaint.setTextSize(scoreIncrementText.getSize());

       initStaticBitmap();
       initStaticBitmapTwovTwo();
    }

    /**
     * Initialize the static bitmap layer that contains non-changing elements
     */
    private void initStaticBitmap() {
        // Create a bitmap for the static layer
        staticLayer = Bitmap.createBitmap(screenX, screenY, Bitmap.Config.RGB_565);
        staticCanvas = new Canvas(staticLayer);
        // Draw the beige background
        staticCanvas.drawColor(Color.parseColor("#FFF1E9"));
        // Draw the outer circle in the middle of the field
        staticCanvas.drawCircle((float) screenX / 2, (float) screenY / 2, outerRadius, middleCirclePaint);
        // Draw the settings icon in the middle of the screen
        staticCanvas.drawBitmap(settingsIcon, settingsMatrix, null);
        // Draw the goal regions
        for (int i = 0; i < cornerPaths.size(); i++) {
            switch(i) {
                case 0:
                    staticCanvas.drawPath(cornerPaths.get(i), goalPaintBlue);
                    break;
                case 1:
                    staticCanvas.drawPath(cornerPaths.get(i), goalPaintRed);
                    break;
                case 2:
                    staticCanvas.drawPath(cornerPaths.get(i), twoVtwoMode ? goalPaintBlue : goalPaintGreen);
                    break;
                case 3:
                    staticCanvas.drawPath(cornerPaths.get(i), twoVtwoMode ? goalPaintRed : goalPaintYellow);
                    break;
            }
        }
        // Draw the bounce edges of the goals
        for (Vector edge : bounceEdges) {
            staticCanvas.drawLine((float) edge.getX1(), (float) edge.getY1(), (float) edge.getX2(), (float) edge.getY2(), edgePaint);
            // Draw a little index number for debugging
            if (debug) {
                staticCanvas.drawText(String.valueOf(bounceEdges.indexOf(edge)), (float) ((edge.getX1() + edge.getX2()) / 2), (float) ((edge.getY1() + edge.getY2()) / 2), fpsPaint);
            }
        }
        for (Vector edge : verticalGoalEdges) {
            staticCanvas.drawLine((float) edge.getX1(), (float) edge.getY1(), (float) edge.getX2(), (float) edge.getY2(), edgePaint);
        }
        // Draw goalposts
        drawGoalPosts();
        // Draw the goal lines
        if (PLAYERCOUNT < 4) {
            // Draw unused goal lines when in less than 4 player mode
            if (PLAYERCOUNT == 2) {
                goalLines.get(2).draw(staticCanvas);
                goalLines.get(3).draw(staticCanvas);
            } else if (PLAYERCOUNT == 3) {
                goalLines.get(3).draw(staticCanvas);
            }
        }
    }

    /**
     * Initialize the static bitmap layer for 2v2 mode
     */
    private void initStaticBitmapTwovTwo() {
        // Create a bitmap for the static layer
        staticLayerTwovTwo = Bitmap.createBitmap(screenX, screenY, Bitmap.Config.RGB_565);
        staticCanvasTwovTwo = new Canvas(staticLayerTwovTwo);
        // Draw the beige background
        staticCanvasTwovTwo.drawColor(Color.parseColor("#FFF1E9"));
        // Draw the outer circle in the middle of the field
        staticCanvasTwovTwo.drawCircle((float) screenX / 2, (float) screenY / 2, outerRadius, middleCirclePaint);
        // Draw the settings icon in the middle of the screen
        staticCanvasTwovTwo.drawBitmap(settingsIcon, settingsMatrix, null);
        // Draw the goal regions
        for (int i = 0; i < cornerPathsTwovTwo.size(); i++) {
            switch (i) {
                case 0:
                    staticCanvasTwovTwo.drawPath(cornerPathsTwovTwo.get(i), goalPaintBlue);
                    break;
                case 1:
                    staticCanvasTwovTwo.drawPath(cornerPathsTwovTwo.get(i), goalPaintRed);
                    break;
            }
        }
        // Draw the bounce edges of the goals
        for (Vector edge : bounceEdgesTwovTwo) {
            staticCanvasTwovTwo.drawLine((float) edge.getX1(), (float) edge.getY1(), (float) edge.getX2(), (float) edge.getY2(), edgePaint);
            if (debug) {
                // Draw a little index number for debugging
                staticCanvasTwovTwo.drawText(String.valueOf(bounceEdgesTwovTwo.indexOf(edge)), (float) ((edge.getX1() + edge.getX2()) / 2), (float) ((edge.getY1() + edge.getY2()) / 2), fpsPaint);
            }
        }
        // Draw goalposts
        drawGoalPostsTwovTwo();
    }

    /**
     * Draw the goal posts at the corners of the goals
     */
    private void drawGoalPosts() {
        int postSide = 1;
        int totalPosts = twoVtwoMode ? 4 : PLAYERCOUNT * 2;
        for (int i = 0; i < totalPosts; i++) {
            Vector edge = bounceEdges.get(i);
            float x = (postSide % 2 == 0) ? (float) edge.getX1() : (float) edge.getX2();
            float y = (postSide % 2 == 0) ? (float) edge.getY1() : (float) edge.getY2();

            staticCanvas.drawCircle(x, y, 5, goalPostPaint);
            // Draw a little index number for debugging
            if (debug) {
                staticCanvas.drawText(String.valueOf(i), x + 10, y + 10, fpsPaint);
            }
            postSide++;
        }
    }

    /**
     * Draw the goal posts at the corners of the goals in 2v2 mode
     */
    private void drawGoalPostsTwovTwo() {
        for (int i = 0; i < goalPostsTwovTwo.size(); i++) {
            staticCanvasTwovTwo.drawCircle(goalPostsTwovTwo.get(i).getX(), goalPostsTwovTwo.get(i).getY(), GOALPOSTRADIUS, goalPostPaint);
            // Draw a little index number for debugging
            if (debug) {
                staticCanvasTwovTwo.drawText(String.valueOf(i), goalPostsTwovTwo.get(i).getX() + 10, goalPostsTwovTwo.get(i).getY() + 10, fpsPaint);
            }
        }
    }

    /**
     * Run method for the game thread
     */
    @Override
    public void run() {
        final long OPTIMAL_TIME = 1_000_000_000 / TARGET_FPS;
        long lastLoopTime = System.nanoTime();
        long fpsTimer = System.nanoTime();
        int frames = 0;

        while (isPlaying) {
            long now = System.nanoTime();
            float deltaTime = (now - lastLoopTime) / 1_000_000_000.0f; // seconds
            lastLoopTime = now;

            // Update game objects using deltaTime
            update(deltaTime);

            // Draw frame
            draw();

            frames++;

            // Sleep to maintain target FPS
            long frameTime = System.nanoTime() - now;
            long sleepTime = (OPTIMAL_TIME - frameTime) / 1_000_000; // ms
            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime); } catch (InterruptedException ignored) {}
            }

            // Update FPS every second
            if (System.nanoTime() - fpsTimer >= 1_000_000_000) {
                fps = frames;
                frames = 0;
                fpsTimer = System.nanoTime();
                Log.d("FPSTRACK", "Real FPS: " + fps);
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

    private void determineBounceEdgesTwovTwo() {
        // Bottom-left bounce edge
        Vector bottomLeft = new Vector(0, screenY - screenX * 0.3f, screenX * 0.35, screenY - screenX * 0.3f);
        // Bottom-right bounce edge
        Vector bottomRight = new Vector(screenX * 0.65f, screenY - screenX * 0.3f, screenX, screenY - screenX * 0.3f);
        // Top-left bounce edge
        Vector topLeft = new Vector(0, screenX * 0.3f, screenX * 0.35f, screenX * 0.3f);
        // Top-right bounce edge
        Vector topRight = new Vector(screenX * 0.65f, screenX * 0.3f, screenX, screenX * 0.3f);
        bounceEdgesTwovTwo.add(bottomRight);
        bounceEdgesTwovTwo.add(bottomLeft);
        bounceEdgesTwovTwo.add(topLeft);
        bounceEdgesTwovTwo.add(topRight);
    }

    /**
     * Determine the positions of the goal posts based on the goal lines
     */
    private void determineGoalPosts() {
        double x = goalLines.get(0).getX2();
        double y = goalLines.get(0).getY2();
        goalPosts.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
        x = goalLines.get(0).getX1();
        y = goalLines.get(0).getY1();
        goalPosts.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
        x = goalLines.get(1).getX1();
        y = goalLines.get(1).getY1();
        goalPosts.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
        x = goalLines.get(1).getX2();
        y = goalLines.get(1).getY2();
        goalPosts.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
    }

    /**
     * Determine the positions of the goal posts based on the goal lines in 2v2 mode
     */
    private void determineGoalPostsTwovTwo() {
        double x = goalLinesTwovTwo.get(0).getX2();
        double y = goalLinesTwovTwo.get(0).getY2();
        goalPostsTwovTwo.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
        x = goalLinesTwovTwo.get(0).getX1();
        y = goalLinesTwovTwo.get(0).getY1();
        goalPostsTwovTwo.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
        x = goalLinesTwovTwo.get(1).getX1();
        y = goalLinesTwovTwo.get(1).getY1();
        goalPostsTwovTwo.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
        x = goalLinesTwovTwo.get(1).getX2();
        y = goalLinesTwovTwo.get(1).getY2();
        goalPostsTwovTwo.add(new GoalPost((float) x, (float) y, GOALPOSTRADIUS));
    }

    /**
     * Initialize the first player object
     */
    private void determineGoalLines() {
        // Iterate over all diagonal edges to determine the goals
        for (Vector edge : diagonalEdges) {
            // Get scaled vector
            Vector scaledEdge = edge.getCenteredAndScaledVector(goalWidth);
            goalLines.add(scaledEdge);
        }
    }
    /**
     * Determine the goal regions for each player based on the screen dimensions
     */

    /**
     * Determine the goal regions for each player based on the screen dimensions
     */
    private void determineGoalLinesTwovTwo() {
        // Bottom blue goal
        Vector bottomGoal = new Vector(screenX * 0.35f, screenY - screenX * 0.3f, screenX * 0.65f, screenY - screenX * 0.3f);
        // Top red goal
        Vector topGoal = new Vector(screenX * 0.35f, screenX * 0.3f, screenX * 0.65f, screenX * 0.3f);
        goalLinesTwovTwo.add(bottomGoal);
        goalLinesTwovTwo.add(topGoal);
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
    private void update(float deltaTime) {
        if (onlineMode) {
            updateOnline();
        } else {
            updateBalls();
            checkPlayerBallCollision();
            updatePlayers();
        }
    }

    private void updateOnline() {

    }

    /**
     * Update the positions of the balls
     */
    private void updateBalls() {
        for (Ball ball : balls) {
            if (ball.getShooter() == null) {
                // Check whether the ball has to bounce
                checkHorVertBounce(ball);
                checkEdgeCollision(ball);
                if (PLAYERCOUNT < 4) {
//                    checkGoalLineCollision(ball); Todo: fix
                }

                // Update ball position based on its velocity
                ball.incrementXY();
                // Decrement the ball's velocity
                ball.updateVelocity(BALL_DAMPING_FACTOR);
            } else {
                // Update the ball's position based on the player's direction
                float direction = ball.getShooter().getDirection();  // Get the player's direction (angle in radians)
                if (direction != 0) {
                    // Define a distance to move the ball from the player (e.g., just in front of the player)
                    float combinedRadius = ball.getShooter().getRadius() + ball.getRadius(); // Combine player and ball radii

                    // Calculate new position based on direction, adjusted by combined radius
                    ball.setX((float) (ball.getShooter().getX() + (float) Math.cos(direction) * combinedRadius * 1.1));
                    ball.setY((float) (ball.getShooter().getY() + (float) Math.sin(direction) * combinedRadius * 1.1));
                }
            }

            // If bouncing happened longer than cooldown ago, allow scoring so check for goal
            if (!scored && System.currentTimeMillis() - lastBounceTime > 200) { // Prevent immediate goal after bounce
                checkGoal(ball);
            }
        }
    }

    // Method to check if the deviation between two vectors is greater than 90 degrees
    public static boolean isDeviationGreaterThan90(Vector v1, Vector v2) {
        // Get the direction components of the vectors
        double dx1 = v1.getX2() - v1.getX1();
        double dy1 = v1.getY2() - v1.getY1();

        double dx2 = v2.getX2() - v2.getX1();
        double dy2 = v2.getY2() - v2.getY1();

        // Calculate the dot product of the two direction vectors
        double dotProduct = dx1 * dx2 + dy1 * dy2;

        // If the dot product is negative, the angle is greater than 90 degrees
        return dotProduct < 0;
    }

    /**
     * Check for collisions between the ball and the screen edges (horizontal and vertical)
     * @param ball the ball to check for collisions
     */
    private void checkHorVertBounce(Ball ball) {
        // Check for collision with the left edge
        if (ball.getX() - ball.getRadius() < 0) {
            if (ball.getVelocityX() < 0) {
                ball.setVelocityX(-ball.getVelocityX());  // Reverse horizontal direction
                ball.setLastBouncedEdgeIndex(-1);      // Reset last bounced edge
                Log.d("Bounce", "Ball collided with left edge");
            }
        }

        // Check for collision with the right edge of the screen
        if (ball.getX() + ball.getRadius() > screenX) {
            if (ball.getVelocityX() > 0) {
                ball.setVelocityX(-ball.getVelocityX());  // Reverse horizontal direction
                ball.setLastBouncedEdgeIndex(-1);      // Reset last bounced edge
                Log.d("Bounce", "Ball collided with right edge");
            }
        }

        // Check for collisions with top and bottom edges of the screen
        if (ball.getY() - ball.getRadius() < 0) {
            if (ball.getVelocityY() < 0) {
                ball.setVelocityY(-ball.getVelocityY());  // Reverse vertical direction
                ball.setLastBouncedEdgeIndex(-1); // Reset last bounced edge
                Log.d("Bounce", "Ball collided with top edge");
            }
        }

        if (ball.getY() + ball.getRadius() > screenY) {
            if (ball.getVelocityY() > 0) {
                ball.setVelocityY(-ball.getVelocityY());  // Reverse vertical direction
                ball.setLastBouncedEdgeIndex(-1); // Reset last bounced edge
                Log.d("Bounce", "Ball collided with bottom edge");
            }
        }

        // Check for collisions with the diagonal edges
//        for (int i = 0; i < bounceEdges.size(); i++) {
//            checkEdgeCollision(i, screenX, goalLines);
//        }

        // Check for collisions with the vertical goal edges
        if (!twoVtwoMode) {
            for (Vector edge : verticalGoalEdges) {
                // Get distance from ball to the vector (not infinite line)
                double distance = edge.distanceToPoint(ball.getX(), ball.getY());
                if (distance <= ball.getRadius()) {
                    // Invert ball velocity
                    ball.setVelocityX(-ball.getVelocityX());
                    ball.setLastBouncedEdgeIndex(-1); // Reset last bounced edge
                }
            }
        }
    }

    /**
     * Make the player shoot the ball away from them
     * @param ball the ball to be possibly shot
     */
    public void shoot(Ball ball) {
        Player player = ball.getShooter();
        Log.d("Shoot", player == null ? "Player null for ball" : "Ball has a player " + players.indexOf(player));
        if (player != null) {
            ball.setShooter(player);
//            Log.d("Shoot", "Player is shooting the ball" + (player.getColor() == Color.BLUE));
            // Calculate the direction from ball to player (ballX - playerX, ballY - playerY)
            float dx = ball.getX() - player.getX();  // Ball's position minus Player's position (shoot away from player)
            float dy = ball.getY() - player.getY();  // Ball's position minus Player's position (shoot away from player)
            Log.d("Direction", "Direction before normalization: (" + dx + ", " + dy + ")");
            // Normalize direction vector (dx, dy)
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length != 0) {
                dx /= length;
                dy /= length;
            }

            // Set the ball's velocity to move away from the player
            ball.setVelocityX(dx * BALL_SPEED);
            ball.setVelocityY(dy * BALL_SPEED);

            // Once the ball is shot, release it from the player
            player.releaseBall();
            Log.d("Shoot", "Ball was shot");
        }
    }

    /**
     * Check for collisions between the ball and the defined edge vectors of the goals
     * @param ball the ball to check for collisions
     */
    public void checkEdgeCollision(Ball ball) {
        if (twoVtwoMode) {
            for (Vector edge : bounceEdgesTwovTwo) {
                // Get distance from ball to the vector (not infinite line)
                double distance = edge.distanceToPoint(ball.getX(), ball.getY());
                if (distance <= ball.getRadius()) {
                    // Invert ball velocity
                    ball.setVelocityX(-ball.getVelocityX());
                    ball.setVelocityY(-ball.getVelocityY());
                    ball.setLastBouncedEdgeIndex(-1); // Reset last bounced edge
                }
            }
        } else {
            for (int edgeVectorIndex = 0; edgeVectorIndex < bounceEdges.size(); edgeVectorIndex++) {
                long currentTime = System.currentTimeMillis();
                double postRadius = 6; // Radius of the goalpost

                // If still within the cooldown period, don't allow another bounce
                if ((ball.getLastBouncedEdgeIndex() == edgeVectorIndex && ball.getLastGoalpostIndex() != edgeVectorIndex) && (currentTime - lastBounceTime < BALL_BOUNCE_COOLDOWN_MS)) {
                    return;
                }

                Vector edge = bounceEdges.get(edgeVectorIndex);
                double x1 = edge.getX1();
                double y1 = edge.getY1();
                double x2 = edge.getX2();
                double y2 = edge.getY2();
                double edgeDX = x2 - x1;
                double edgeDY = y2 - y1;

                double ballDX = ball.getX() - x1;
                double ballDY = ball.getY() - y1;
                double dotProduct = ballDX * edgeDX + ballDY * edgeDY;
                double edgeLengthSquared = edgeDX * edgeDX + edgeDY * edgeDY;
                double projection = dotProduct / edgeLengthSquared;

                // Handle goalpost collision (if near the endpoints)
                if (goalLines != null && !((goalLines.size() == 1 && (edgeVectorIndex == 4 || edgeVectorIndex == 5)) || (goalLines.size() == 2 && (edgeVectorIndex == 6 || edgeVectorIndex == 7)))) {
                    double distanceToStart = Math.sqrt(Math.pow(ball.getX() - x1, 2) + Math.pow(ball.getY() - y1, 2));
                    double distanceToEnd = Math.sqrt(Math.pow(ball.getX() - x2, 2) + Math.pow(ball.getY() - y2, 2));

                    double goalpostX = (distanceToStart <= (postRadius + ball.getRadius())) ? x1 : x2;
                    double goalpostY = (distanceToStart <= (postRadius + ball.getRadius())) ? y1 : y2;

                    // Check if the ball is within the goalpost radius
                    // And make sure only the intended goalposts are checked
                    boolean sameGoalPost = (ball.getLastGoalpostIndex() == edgeVectorIndex) || (ball.getLastGoalpostIndex() == 3 && edgeVectorIndex == 6) || (ball.getLastGoalpostIndex() == 6 && edgeVectorIndex == 3) || (ball.getLastGoalpostIndex() == 4 && edgeVectorIndex == 1) || (ball.getLastGoalpostIndex() == 1 && edgeVectorIndex == 4);
                    if (((distanceToStart <= (postRadius + ball.getRadius()) || distanceToEnd <= (postRadius + ball.getRadius())) &&
                            (goalpostX > 0 && goalpostX < screenX)) && !sameGoalPost && (goalpostX > (double) screenX / 2 + 5 || goalpostX < (double) screenX / 2 - 5)) {
                        Log.d("Bounce", "Ball collided with goalpost " + edgeVectorIndex);
                        Log.d("Bounce", "Time since last bounce: " + (System.currentTimeMillis() - lastBounceTime));
                        Log.d("Bounce", "GoalpostX: " + goalpostX);
                        // Log screenx / 2
                        Log.d("Bounce", "Screenx / 2: " + screenX / 2);

                        ball.setLastBouncedEdgeIndex(edgeVectorIndex);
                        ball.setLastGoalpostIndex(edgeVectorIndex);
                        lastBounceTime = currentTime;

                        double normalX = ball.getX() - goalpostX;
                        double normalY = ball.getY() - goalpostY;
                        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY);
                        normalX /= normalLength;
                        normalY /= normalLength;

                        ball.reflect(normalX, normalY);
//                    resolveOverlap(normalX, normalY, postRadius - Math.min(distanceToStart, distanceToEnd));
                        Log.d("Bounce", "Ball velocity: (" + ball.getVelocityX() + ", " + ball.getVelocityY() + ")");
                        return;
                    }
                }

                // Normal edge collision
                double closestX, closestY;
                if (projection < 0) {
                    closestX = x1;
                    closestY = y1;
                } else if (projection > 1) {
                    closestX = x2;
                    closestY = y2;
                } else {
                    closestX = x1 + projection * edgeDX;
                    closestY = y1 + projection * edgeDY;
                }

                double distanceToEdge = Math.sqrt(Math.pow(ball.getX() - closestX, 2) + Math.pow(ball.getY() - closestY, 2));

                if (distanceToEdge <= ball.getRadius()) {
                    Log.d("Bounce", "Ball collided with edge " + edgeVectorIndex);
                    Log.d("Bounce", "Time since last bounce: " + (System.currentTimeMillis() - lastBounceTime));
                    Log.d("Bounce", "Last bounce edge: " + ball.getLastBouncedEdgeIndex());
                    ball.setLastBouncedEdgeIndex(edgeVectorIndex);
                    ball.setLastGoalpostIndex(-1);
                    lastBounceTime = currentTime;

                    double normalX = -edgeDY;
                    double normalY = edgeDX;
                    double normalLength = Math.sqrt(normalX * normalX + normalY * normalY);
                    normalX /= normalLength;
                    normalY /= normalLength;

                    // Ensure the ball bounces in the correct direction (ball passed through the edge case)
                    if ((ball.getVelocityX() * normalX + ball.getVelocityY() * normalY) > 0) {
                        normalX = -normalX;
                        normalY = -normalY;
                    }

                    ball.reflect(normalX, normalY);
//                resolveOverlap(normalX, normalY, radius - distanceToEdge);

                    Log.d("Bounce", "Ball velocity: (" + ball.getVelocityX() + ", " + ball.getVelocityY() + ")");
                }
            }
        }
    }

    private void resolveOverlap(double normalX, double normalY, double overlap) {
//        if (overlap > 0) {
//            setX((float) (ball.getX() + normalX * overlap));
//            setY((float) (ball.getY() + normalY * overlap));
//        }
    }

    /**
     * Handle the scoring of a goal by a player
     * @param goal the goal number
     * @param player the player object
     */
    private void scored(int goal, Player player) {
        soundManager.playGoalSound();
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
        if (shooter == null) return;

        if (twoVtwoMode) {
            for (int i = 0; i < goalRegionsTwovTwo.size(); i++) {

            }
        } else {
            for (int i = 0; i < goalRegions.size(); i++) {
                Region region = goalRegions.get(i);

                if (!region.contains((int) ballX, (int) ballY)) {
                    continue;
                }

                if (lastGoal == i && shooter != null) {
                    if (now - lastGoalTime < 200) { // 200 ms cooldown
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
    }

    /**
     * Draw the normal vectors of the edges for debugging purposes
     */
    public void drawNormalVectorsInwards(Canvas canvas) {
        Paint paint = new Paint();
        // Draw normal vectors of the edges from the middle of screen
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        paint = new Paint();
        for (Vector edge : bounceEdges) {
            // Draw the normal vector or the edge * 50 length for visibility
            Vector normalVector = getNormalVector(edge);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawLine((float) normalVector.getX1(), (float) normalVector.getY1(), (float) (normalVector.getX1() + (normalVector.getX2() - normalVector.getX1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), (float) (normalVector.getY1() + (normalVector.getY2() - normalVector.getY1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), paint);
        }
    }

    /**
     * Draw the normal vectors of the edges for debugging purposes
     */
    public void drawNormalVectorsOutwards(Canvas canvas) {
        Paint paint = new Paint();
        // Draw normal vectors of the edges from the middle of screen
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        paint = new Paint();
        for (Vector edge : bounceEdges) {
            // Draw the normal vector or the edge * 50 length for visibility
            Vector normalVector = getNormalVector(edge);
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(5);
            canvas.drawLine((float) normalVector.getX1(), (float) normalVector.getY1(), (float) (normalVector.getX1() - (normalVector.getX2() - normalVector.getX1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), (float) (normalVector.getY1() - (normalVector.getY2() - normalVector.getY1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), paint);
        }
    }

    /**
     * Draw the normal vectors of the edges for debugging purposes in 2v2 mode
     */
    public void drawNormalVectorsInwardsTwovTwo(Canvas canvas) {
        Paint paint = new Paint();
        // Draw normal vectors of the edges from the middle of screen
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        paint = new Paint();
        for (Vector edge : bounceEdgesTwovTwo) {
            // Draw the normal vector or the edge * 50 length for visibility
            Vector normalVector = getNormalVector(edge);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawLine((float) normalVector.getX1(), (float) normalVector.getY1(), (float) (normalVector.getX1() + (normalVector.getX2() - normalVector.getX1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), (float) (normalVector.getY1() + (normalVector.getY2() - normalVector.getY1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), paint);
        }
    }

    /**
     * Draw the normal vectors of the edges for debugging purposes in 2v2 mode
     */
    public void drawNormalVectorsOutwardsTwovTwo(Canvas canvas) {
        Paint paint = new Paint();
        // Draw normal vectors of the edges from the middle of screen
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        paint = new Paint();
        for (Vector edge : bounceEdgesTwovTwo) {
            // Draw the normal vector or the edge * 50 length for visibility
            Vector normalVector = getNormalVector(edge);
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(5);
            canvas.drawLine((float) normalVector.getX1(), (float) normalVector.getY1(), (float) (normalVector.getX1() - (normalVector.getX2() - normalVector.getX1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), (float) (normalVector.getY1() - (normalVector.getY2() - normalVector.getY1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), paint);
        }
    }

    /**
     * Get the normal vector of a given edge vector
     * @param vector the edge vector
     * @return the normal vector
     */
    private Vector getNormalVector(Vector vector) {
        float edgeX = (float) (vector.getX2() - vector.getX1());
        float edgeY = (float) (vector.getY2() - vector.getY1());
        float edgeLength = (float) Math.sqrt(edgeX * edgeX + edgeY * edgeY);
        float normalX = -edgeY / edgeLength;
        float normalY = edgeX / edgeLength;

        // Draw normal vector from the middle of the edge
        float startX = (float) (vector.getX1() + edgeX / 2);
        float startY = (float) (vector.getY1() + edgeY / 2);
        float endX = startX + normalX;
        float endY = startY + normalY;
        return new Vector(startX, startY, endX, endY);
    }

    /**
     * Display the goal animation on the screen
     */
    public void displayStarsAnimation(Canvas canvas) {
        // Save time
        if (splashStartTime == 0) {
            splashStartTime = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - splashStartTime < 1200) { // Show for 1.2 seconds
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
        long startTime = System.nanoTime();

        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();

        if (twoVtwoMode) {
            canvas.drawBitmap(staticLayerTwovTwo, 0, 0, null);    // Background + static stuff
            if (debug) {
                drawNormalVectorsInwardsTwovTwo(canvas);
                drawNormalVectorsOutwardsTwovTwo(canvas);
            }
        } else {
            canvas.drawBitmap(staticLayer, 0, 0, null);    // Background + static stuff
            if (debug) {
                drawNormalVectorsInwards(canvas);
                drawNormalVectorsOutwards(canvas);
            }
        }

//        Log.d("GameView", "Draw time 1: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

        drawScores(canvas);

//        Log.d("GameView", "Draw time after scores: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
        // Draw the balls
        if (needSync) {
            Log.d("GameView", "Drawing with synchronization");
            synchronized (joysticks) {
                for (Joystick joystick : joysticks) {
                    joystick.draw(canvas);  // Call the draw method for each joystick
                }
            }
            synchronized (balls) {
                for (Ball ball : balls) {
                    ball.draw(canvas);
                }
            }
            synchronized (shootButtons) {
                for (ShootButton button : shootButtons) {
                    button.draw(canvas);
                }
            }
            synchronized (players) {
                for (int i = 0; i < PLAYERCOUNT; i++) {
                    players.get(i).draw(canvas);
                }
            }
        } else  {
//            Log.d("GameView", "Drawing without synchronization: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
            for (Joystick joystick : joysticks) {
//                Log.d("GameView", "Time for first circle: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
                joystick.draw(canvas);  // Call the draw method for each joystick
//                Log.d("GameView", "Time for second circle: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

            }
//            Log.d("GameView", "Draw time after joysticks: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
            for (Ball ball : balls) {
                ball.draw(canvas);
            }
//            Log.d("GameView", "Draw time after balls: " + (System.nanoTime() - startTime) / 1_000_000+ " ms");
            for (ShootButton button : shootButtons) {
                button.draw(canvas);
            }
//            Log.d("GameView", "Draw time after buttons: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
            for (Player player : players) {
                player.draw(canvas);
            }
        }
//        Log.d("GameView", "Draw time after sync: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

        if (scored) {
            displayStarsAnimation(canvas);
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
//                Log.d("GameView", "Not all scores are zero, updating stars");
                for (Star star : stars) {
                    // Fade stars out
                    star.update(canvas, (int) balls.get(0).getX(), (int) balls.get(0).getY(), true);
                    star.bounce(screenX, screenY);
                }
            }
        }

        // Draw fps
        canvas.drawText("FPS: " + fps, 10, 50, fpsPaint);

//        Log.d("GameView", "Draw time before unlocking holder: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

        // Unlock the canvas and post the updates
        holder.unlockCanvasAndPost(canvas);
        Log.d("GameView", "Draw time after holder: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
    }

    /**
     * Draw the scores of the players on the screen
     * @param canvas the canvas to draw on
     */
    private void drawScores(Canvas canvas) {
        // Draw the scores of the players
        if (!twoVtwoMode) {
            for (int i = 0; i < PLAYERCOUNT; i++) {
                // Save the current canvas state
                canvas.save();

                // Rotate around the text position
                canvas.rotate(scoreRotations.get(i), scorePositions.get(i)[0], scorePositions.get(i)[1]);

                // Draw the text
                canvas.drawText(String.valueOf(players.get(i).getScore()), scorePositions.get(i)[0], scorePositions.get(i)[1], scoresPaints.get(i));

                // Restore canvas to avoid affecting other drawings
                canvas.restore();
            }
        } else { // draw scores of the 2 teams
            if (needSync) {
                synchronized (teams) {
                    for (int i = 0; i < teams.size(); i++) {
                        // Set text color
                        teamScoresPaint.setColor(i == 0 ? Color.BLUE : Color.RED);

                        // Save the current canvas state
                        canvas.save();

                        // Rotate around the text position
                        canvas.rotate((i == 0 ? 0 : 180), teams.get(i).getScorePositionX(), teams.get(i).getScorePositionY());

                        // Draw the text
                        Team team = teams.get(i);
                        canvas.drawText(String.valueOf(team.getScore()), team.getScorePositionX(), team.getScorePositionY(), teamScoresPaint);

                        // Restore canvas to avoid affecting other drawings
                        canvas.restore();
                    }
                }
            } else {
                for (int i = 0; i < teams.size(); i++) {
                    // Set text color
                    teamScoresPaint.setColor(i == 0 ? Color.BLUE : Color.RED);

                    // Save the current canvas state
                    canvas.save();

                    // Rotate around the text position
                    canvas.rotate((i == 0 ? 0 : 180), teams.get(i).getScorePositionX(), teams.get(i).getScorePositionY());

                    // Draw the text
                    Team team = teams.get(i);
                    canvas.drawText(String.valueOf(team.getScore()), team.getScorePositionX(), team.getScorePositionY(), teamScoresPaint);

                    // Restore canvas to avoid affecting other drawings
                    canvas.restore();
                }
            }
        }

        if (scored) {
            // Set text size
            GOALtextPaint.setColor(lastShooter.getColor());
            // Draw the centered text
            float textWidth = GOALtextPaint.measureText("GOAL!");
            // Calculate the x position for centering the text
            float x = (screenX - textWidth) / 2;
            // Calculate the y position for centering the text vertically
            // Adjusting based on text ascent and descent which. Done because text is drawn from the baseline
            float y = ((float) screenY / 2) - ((GOALtextPaint.descent() + GOALtextPaint.ascent()) / 2);
            GOALText.increment(3, 0, 0);
            GOALtextPaint.setTextSize(GOALText.getSize());
            canvas.drawText("GOAL!", x, y, GOALtextPaint);


            // Draw a score increment animation above the last shooter score position
            canvas.save();
            canvas.rotate(scoreIncrementText.getRotation(), scoreIncrementText.getX(), scoreIncrementText.getY());
            scoreIncrementPaint.setColor(lastShooter.getColor());
            canvas.drawText("+1", scoreIncrementText.getX(), scoreIncrementText.getY(), scoreIncrementPaint);
            canvas.restore();
            if (scoreIncrementText.getRotation() == 180) {
                scoreIncrementText.increment(0, 0, 1);
            } else {
                scoreIncrementText.increment(0, 0, -1);
            }
        } else {
            GOALText.reset(screenX, screenY);
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
                        needSync = true;
                        settingsDialog.setOnDismissListener(d -> needSync = false);
                    }

                    // Check for joystick touch if no button was pressed
                    if (needSync) {
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
                    } else {
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
                        if (needSync) {
                            synchronized (shootButtons) {
                                for (ShootButton button : shootButtons) {
                                    if (button.isTouched(touchX, touchY)) {
                                        button.setPointerID(pointerId);
                                        break;
                                    }
                                }
                            }
                        } else {
                            for (ShootButton button : shootButtons) {
                                if (button.isTouched(touchX, touchY)) {
                                    button.setPointerID(pointerId);
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
                    if (needSync) {
                        synchronized (joysticks) {
                            for (Joystick joystick : joysticks) {
                                if (joystick.isPressedBy(pointerId)) {
                                    joystick.reset();
                                    pointerHandled = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        for (Joystick joystick : joysticks) {
                            if (joystick.isPressedBy(pointerId)) {
                                joystick.reset();
                                pointerHandled = true;
                                break;
                            }
                        }
                    }

                    // Handle shoot button release
                    if (!pointerHandled) {
                        if (needSync) {
                            synchronized (shootButtons) {
                                for (ShootButton button : shootButtons) {
                                    if (button.wasTouchedBy(pointerId)) {
                                        Player shootingPlayer = players.get(shootButtons.indexOf(button));
                                        if (button.isTouched(event.getX(actionIndex), event.getY(actionIndex)) && shootingPlayer.getBall() != null) {
                                            shoot(shootingPlayer.getBall());
                                            soundManager.playShootSound();
                                        }
                                        button.resetTouchID();
                                        break;
                                    }
                                }
                            }
                        } else {
                            for (ShootButton button : shootButtons) {
                                if (button.wasTouchedBy(pointerId)) {
                                    Player shootingPlayer = players.get(shootButtons.indexOf(button));
                                    if (button.isTouched(event.getX(actionIndex), event.getY(actionIndex)) && shootingPlayer.getBall() != null) {
                                        shoot(shootingPlayer.getBall());
                                        soundManager.playShootSound();
                                    }
                                    button.resetTouchID();
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

                        if (needSync) {
                            synchronized (joysticks) {
                                for (Joystick joystick : joysticks) {
                                    if (joystick.isPressedBy(movePointerId)) {
                                        joystick.onTouch(moveX, moveY);
                                        break;
                                    }
                                }
                            }
                        } else {
                            for (Joystick joystick : joysticks) {
                                if (joystick.isPressedBy(movePointerId)) {
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
                    float dist; // Initialize with a large distance

                    switch (players.indexOf(player)) {
                        case 0:
                            dist = (i == 0 || i == 1) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        case 1:
                            dist = (i == 2 || i == 3) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        case 2:
                            dist = (i == 4 || i == 5) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
                            break;
                        case 3:
                            dist = (i == 6 || i == 7) ? (float) pointToLineSegmentDistOwnGoal(edge, player) : (float) pointToLineDist(edge, player);
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
        if (needSync) {
            synchronized (players) {
                synchronized (balls) {
                    for (Player player : players) {
                        for (Ball ball : balls) {
                            float dx = player.getX() - ball.getX();
                            float dy = player.getY() - ball.getY();
                            float distance = (float) Math.sqrt(dx * dx + dy * dy);

                            // If the distance is less than the sum of the radii, a collision has occurred
                            if (distance <= player.getRadius() + ball.getRadius()) {
                                onPlayerHitBall(player, ball);
                            }
                        }
                    }
                }
            }
        } else {
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
    }

    /**
     * Handle the collision between a player and a ball
     * @param player the player object
     * @param ball the ball object
     */
    private void onPlayerHitBall(Player player, Ball ball) {
        if (player.canTakeBall()) {
            if (ball.getShooter() != null) {
                ball.getShooter().setBall(null);
            }
            ball.setShooter(player);
            player.setBall(ball);
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

    /**
     * Determine the corner areas of the screen for two vs two mode
     */
    private void determineGoalRegionsTwovTwo() {
        // Bottom blue goal
        Path bottomPath = new Path();
        bottomPath.moveTo(0, screenY - screenX * 0.3f);
        bottomPath.lineTo(screenX, screenY - screenX * 0.3f);
        bottomPath.lineTo(screenX, screenY);
        bottomPath.lineTo(0, screenY);
        bottomPath.close();
//        diagonalEdges.add(new Vector(screenX, screenY - screenX * 0.5, screenX * 0.5, screenY * 0.91));

        // Top red goal
        Path topPath = new Path();
        topPath.moveTo(0, 0);
        topPath.lineTo(screenX, 0);
        topPath.lineTo(screenX, screenX * 0.3f);
        topPath.lineTo(0, screenX * 0.3f);
        topPath.close();
//        diagonalEdges.add(new Vector(0, screenX * 0.5, screenX * 0.5, screenY * 0.09));

        // Add the goal paths to the paths list
        cornerPathsTwovTwo.add(bottomPath);
        cornerPathsTwovTwo.add(topPath);

        // Initialize a region with the screen dimensions
        Region region = new Region(0, 0, screenX, screenY);

        // Loop through the list of corner paths and add each as a region
        for (Path path : cornerPathsTwovTwo) {
            Region cornerRegion = new Region();
            // Set the path within the bounding region
            cornerRegion.setPath(path, region);
            // Add the corner region to the list
            goalRegionsTwovTwo.add(cornerRegion);
        }
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
            for (Ball ball : balls) {
                ball.reset((int) (screenX / 2f), (int) (screenY / 2f));
            }
            for (Player player : players) {
                player.reset(playerPositions[players.indexOf(player)][0], playerPositions[players.indexOf(player)][1]);
            }
        }
        Log.d("SettingsDialog", "Change settings called");
        if (!online) {
            if (onlineMode) {
                Log.d("SettingsDialog", "Going in offline mode");
                changePlayerSpeed(playerSpeed);
                changeBallSpeed(ballSpeed);
                for (Player player : players) {
                    player.resetScore();
                }
            }
            if (twoVtwo) {
                PLAYERCOUNT = 4;
                twoVtwoMode = true;
                // If two vs two mode is enabled
                teams.add(new Team(players.get(0), players.get(2)));
                teams.get(0).setColor(Color.BLUE);
                teams.add(new Team(players.get(1), players.get(3)));
                teams.get(1).setColor(Color.RED);

                // Reset player colors for two vs two mode
                for (int i = 0; i < players.size(); i++) {
                    if (i == 0 || i == 2) {
                        players.get(i).setColor(Color.BLUE);
                    } else {
                        players.get(i).setColor(Color.RED);
                    }
                }

                // Reset shoot button colors for two vs two mode
                synchronized (shootButtons) {
                    for (int i = 0; i < shootButtons.size(); i++) {
                        if (i == 0 || i == 2) {
                            shootButtons.get(i).changePaint(Color.BLUE);
                        } else {
                            shootButtons.get(i).changePaint(Color.RED);
                        }
                    }
                }
            } else {
                if (twoVtwoMode) { // If we were in 2v2 mode before
                    // Reset player scores
                    teams.clear();
                    // Reset player colors for FFA mode
                    for (int i = 0; i < players.size(); i++) {
                        if (i == 2) {
                            players.get(i).setColor(Color.GREEN);
                            players.get(i).getShootButton().changePaint(Color.GREEN);
                        }
                        if (i == 3) {
                            players.get(i).setColor(0xFFFFEB04); // Yellow
                            players.get(i).getShootButton().changePaint(0xFFFFEB04); // Yellow
                        }
                    }
                    for (Player player : players) {
                        player.resetScore();
                    }
                    for (Ball ball: balls) {
                        ball.reset((int) (screenX / 2f), (int) (screenY / 2f));
                    }
                }
                twoVtwoMode = false;
            }
            changePlayerSpeed(playerSpeed);
            changeBallSpeed(ballSpeed);
        } else { // Todo: Finish
            if (!onlineMode) {
                // Reset the game state and connect to server
                try {
                    connectToServer();
                } catch (Exception e) {
                    Log.d("ClientConnection", "Error creating client connection: " + e.getMessage());
                }
            }
        }
        this.onlineMode = online;
    }

    /**
     * Ping the server and return the round-trip time in milliseconds
     * @return the ping time in milliseconds, or -1 if not connected
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public int pingServer() throws InterruptedException {
        if (clientConnection == null) return -1;

        int startTime = (int) System.currentTimeMillis();
        pingLock.lock();
        try {
            pingResponse = false;
            clientConnection.sendMessage(Protocol.PING, LocalDateTime.now().toString());
            Log.d("Ping", "Ping message sent to server");
            while (!pingResponse) {
                pingReceived.await();  // waits until signal() is called
            }
        } finally {
            pingLock.unlock();
        }
        return (int) (System.currentTimeMillis() - startTime);
    }

    public void onPingResponse() {
        pingLock.lock();
        try {
            pingResponse = true;
            pingReceived.signalAll();  // wakes up waiting thread
            Log.d("Ping", "Ping response received from server");
        } finally {
            pingLock.unlock();
        }
    }

    /**
     * Connect to the game server
     */
    private void connectToServer() {
        new Thread(() -> {
            try {
                this.server = InetAddress.getByName("192.168.178.31");
                Log.d("Connection", "Server IP: " + server.toString());
                ClientConnection connection = new ClientConnection(server, port);
                Log.d("Connection", "Client connection initialized");
                connection.start();
                this.clientConnection = connection;
                Log.d("Connection", "Client connection created");
                if (clientConnection != null) {
                    clientConnection.setChatClient(this);
                }
            } catch (IOException e) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    Log.d("Connection", "Error creating client connection: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Send a message to the server with a timestamp
     * @param message the message to send
     */
    public void sendMessage(String message) {
        new Thread(() -> {
            if (clientConnection != null) { // Send time.now
                LocalDateTime now = LocalDateTime.now(); // Get current time in ISO format
                String timestamp = now.toString();  // Convert to string
                clientConnection.sendMessage(message, timestamp);
            }
        }).start();
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
        newJoystick = new Joystick(screenX * 0.78f, screenY - JOYSTICK_OUTERRADIUS, JOYSTICK_INNERRADIUS, JOYSTICK_OUTERRADIUS);
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
        newJoystick = new Joystick(screenX * 0.22f, JOYSTICK_OUTERRADIUS, JOYSTICK_INNERRADIUS, JOYSTICK_OUTERRADIUS);
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
        newJoystick = new Joystick(screenX * 0.22f, screenY - JOYSTICK_OUTERRADIUS, JOYSTICK_INNERRADIUS, JOYSTICK_OUTERRADIUS);
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
        newJoystick = new Joystick(screenX * 0.78f, JOYSTICK_OUTERRADIUS, JOYSTICK_INNERRADIUS, JOYSTICK_OUTERRADIUS);
        shootButton = new ShootButton(screenX * 0.91f, screenY * 0.17f, SHOOTBUTTONRADIUS, playerColors[3]);
        newPlayer.setJoystick(newJoystick);
        newPlayer.setShootButton(shootButton);

        addPlayer(newPlayer);
        addJoystick(newJoystick);
        addShootButton(shootButton);
    }

    /**
     * Handle disconnection from the server
     */
    public void handleDisconnect() {

    }

    public void receiveUpdate(String message, String sender, String timestamp) {
        // Handle incoming messages from the server
        // For example, update player positions or scores based on the message
    }

    private String getUsername() {
        return username;
    }
}