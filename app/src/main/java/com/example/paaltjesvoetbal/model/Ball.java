package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.graphics.Color;

import java.util.List;

public class Ball {
    private float x;
    private float y;
    private final float radius;
    private float velocityX, velocityY;
    private static final float DAMPING_FACTOR = 0.985F;
    private Player player;
    private final List<Vector> bounceEdges;
    private final List<Vector> verticalGoalEdges;
    private Paint ballPaint;
    private Player shooter = null;
    private int lastBouncedEdgeIndex = -1;
    private int lastGoalpostIndex = -1;
    private long lastBounceTime = 0;
    private static final long BOUNCE_COOLDOWN_MS = 100; // 100ms cooldown
    public Ball(float x, float y, float radius, List<Vector> bounceEdges, List<Vector> verticalGoalEdges) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.velocityX = 0;
        this.velocityY = 0;
        this.bounceEdges = bounceEdges;
        this.verticalGoalEdges = verticalGoalEdges;
        initializePaint();
    }

    private void initializePaint() {
        // Create a paint object for the ball
        ballPaint = new Paint();
        ballPaint.setAntiAlias(true);

        // Create a shader to fill the ball with a gradient from black to white
//        Shader shader = new LinearGradient(
//                x - radius, y,       // Start at the left side of the circle
//                x + radius, y,           // End at the right side of the circle
//                Color.BLACK,             // Left side color (black)
//                Color.WHITE,             // Right side color (white)
//                Shader.TileMode.CLAMP    // The gradient will not repeat, it will clamp the color at the edges
//        );

        // Apply the shader to the paint
//        ballPaint.setShader(shader);
        ballPaint.setColor(Color.DKGRAY);
    }

    public void draw(Canvas canvas) {
        // Draw the ball with the gradient effect
        canvas.drawCircle(x, y, radius, ballPaint);
//        drawNormalVectors(canvas);
        // Log presence of shooter
        if (shooter != null) {
            Log.d("Shoot", "Ball has a shooter");
        }
    }

    public void drawNormalVectors(Canvas canvas) {
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

    public long update(int screenX, int screenY, List<Vector> goalLines, boolean twovTwo) {
        if (this.player == null) {
            // Check for screen boundary collisions
            bounce(screenX, screenY, goalLines, twovTwo);

            // Update ball position based on its velocity
            x += velocityX;
            y += velocityY;

            // reduce velocity
            setVelocityX(getVelocityX() * DAMPING_FACTOR);
            setVelocityY(getVelocityY() * DAMPING_FACTOR);
        } else {
            // Update the ball's position based on the player's direction
            float direction = this.player.getDirection();  // Get the player's direction (angle in radians)
            if (direction != 0) {
                // Define a distance to move the ball from the player (e.g., just in front of the player)
                float combinedRadius = this.player.getRadius() + this.radius; // Combine player and ball radii

                // Calculate new position based on direction, adjusted by combined radius
                setX((float) (this.player.getX() + (float) Math.cos(direction) * combinedRadius * 1.1));
                setY((float) (this.player.getY() + (float) Math.sin(direction) * combinedRadius * 1.1));
            }
        }
        return lastBounceTime;
    }

    public Player getShooter() {
        return this.shooter;
    }

    public void resetShooter() {
        this.shooter = null;
    }

    private void bounce(float screenX, float screenY, List<Vector> goalLines, boolean twovTwo) {
        // Check for collision with the left edge
        if (getX() - radius < 0) {
            if (getVelocityX() < 0) {
                setVelocityX(-getVelocityX());  // Reverse horizontal direction
                lastBouncedEdgeIndex = -1;      // Reset last bounced edge
                Log.d("Bounce", "Ball collided with left edge");
            }
        }

        // Check for collision with the right edge
        if (getX() + radius > screenX) {
            if (getVelocityX() > 0) {
                setVelocityX(-getVelocityX());  // Reverse horizontal direction
                lastBouncedEdgeIndex = -1;      // Reset last bounced edge
                Log.d("Bounce", "Ball collided with right edge");
            }
        }

        // Check for collisions with top and bottom edges
        if (getY() - radius < 0) {
            if (getVelocityY() < 0) {
                setVelocityY(-getVelocityY());  // Reverse vertical direction
                lastBouncedEdgeIndex = -1; // Reset last bounced edge
                Log.d("Bounce", "Ball collided with top edge");
            }
        }

        if (getY() + radius > screenY) {
            if (getVelocityY() > 0) {
                setVelocityY(-getVelocityY());  // Reverse vertical direction
                lastBouncedEdgeIndex = -1; // Reset last bounced edge
                Log.d("Bounce", "Ball collided with bottom edge");
            }
        }

        // Check for collisions with the diagonal edges
        for (int i = 0; i < bounceEdges.size(); i++) {
            checkEdgeCollision(i, screenX, goalLines);
        }

        // Check for collisions with unused goalLines
        if (goalLines != null) {
            for (Vector edge : goalLines) {
                checkGoalLineCollission(edge, screenX);
            }
        }

        // Check for collisions with the vertical goal edges
        if (!twovTwo) {
            for (Vector edge : verticalGoalEdges) {
                // Get distance from ball to the vector (not infinite line)
                double distance = edge.distanceToPoint(getX(), getY());
                if (distance <= radius) {
                    // Invert ball velocity
                    setVelocityX(-getVelocityX());
                    lastBouncedEdgeIndex = -1; // Reset last bounced edge
                }
            }
        }
    }

    public void checkGoalLineCollission(Vector edge, float screenX) {
        long currentTime = System.currentTimeMillis();
        double postRadius = 6; // Radius of the goalpost

        double x1 = edge.getX1();
        double y1 = edge.getY1();
        double x2 = edge.getX2();
        double y2 = edge.getY2();
        double edgeDX = x2 - x1;
        double edgeDY = y2 - y1;

        // Handle goalpost collision (if near the endpoints)
        double distanceToStart = Math.hypot(getX() - x1, getY() - y1);
        double distanceToEnd = Math.hypot(getX() - x2, getY() - y2);

        double goalpostX = (distanceToStart <= (postRadius + radius)) ? x1 : x2;
        double goalpostY = (distanceToStart <= (postRadius + radius)) ? y1 : y2;

        if ((distanceToStart <= (postRadius + radius) || distanceToEnd <= (postRadius + radius)) &&
                goalpostX > 0 && goalpostX < screenX &&
                (goalpostX > screenX / 2 + 5 || goalpostX < screenX / 2 - 5)) {

            double normalX = getX() - goalpostX;
            double normalY = getY() - goalpostY;
            double normalLength = Math.hypot(normalX, normalY);
            normalX /= normalLength;
            normalY /= normalLength;

            reflectBall(normalX, normalY);
            resolveOverlap(normalX, normalY, postRadius - Math.min(distanceToStart, distanceToEnd));
            lastBounceTime = currentTime;

            return;
        }

        // Normal edge collision
        double ballDX = getX() - x1;
        double ballDY = getY() - y1;
        double projection = (ballDX * edgeDX + ballDY * edgeDY) / (edgeDX * edgeDX + edgeDY * edgeDY);

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

        double distanceToEdge = Math.hypot(getX() - closestX, getY() - closestY);

        if (distanceToEdge <= radius) {
            double normalX = -edgeDY;
            double normalY = edgeDX;
            double normalLength = Math.hypot(normalX, normalY);
            normalX /= normalLength;
            normalY /= normalLength;

            if ((getVelocityX() * normalX + getVelocityY() * normalY) > 0) {
                normalX = -normalX;
                normalY = -normalY;
            }

            reflectBall(normalX, normalY);
            resolveOverlap(normalX, normalY, radius - distanceToEdge);
            lastBounceTime = currentTime;
        }
    }


    public void checkEdgeCollision(int edgeVectorIndex, float screenX, List<Vector> goalLines) {
        long currentTime = System.currentTimeMillis();
        double postRadius = 6; // Radius of the goalpost

        // If still within the cooldown period, don't allow another bounce
        if ((lastBouncedEdgeIndex == edgeVectorIndex && lastGoalpostIndex != edgeVectorIndex) && (currentTime - lastBounceTime < BOUNCE_COOLDOWN_MS)) {
            return;
        }

        Vector edge = bounceEdges.get(edgeVectorIndex);
        double x1 = edge.getX1();
        double y1 = edge.getY1();
        double x2 = edge.getX2();
        double y2 = edge.getY2();
        double edgeDX = x2 - x1;
        double edgeDY = y2 - y1;

        double ballDX = getX() - x1;
        double ballDY = getY() - y1;
        double dotProduct = ballDX * edgeDX + ballDY * edgeDY;
        double edgeLengthSquared = edgeDX * edgeDX + edgeDY * edgeDY;
        double projection = dotProduct / edgeLengthSquared;

        // Handle goalpost collision (if near the endpoints)
        if (goalLines != null && !((goalLines.size() == 1 && (edgeVectorIndex == 4 || edgeVectorIndex == 5)) || (goalLines.size() == 2 && (edgeVectorIndex == 6 || edgeVectorIndex == 7)))) {
            double distanceToStart = Math.sqrt(Math.pow(getX() - x1, 2) + Math.pow(getY() - y1, 2));
            double distanceToEnd = Math.sqrt(Math.pow(getX() - x2, 2) + Math.pow(getY() - y2, 2));

            double goalpostX = (distanceToStart <= (postRadius + radius)) ? x1 : x2;
            double goalpostY = (distanceToStart <= (postRadius + radius)) ? y1 : y2;

            // Check if the ball is within the goalpost radius
            // And make sure only the intended goalposts are checked
            boolean sameGoalPost = (lastGoalpostIndex == edgeVectorIndex) || (lastGoalpostIndex == 3 && edgeVectorIndex == 6) || (lastGoalpostIndex == 6 && edgeVectorIndex == 3) || (lastGoalpostIndex == 4 && edgeVectorIndex == 1) || (lastGoalpostIndex == 1 && edgeVectorIndex == 4);
            if (((distanceToStart <= (postRadius + radius) || distanceToEnd <= (postRadius + radius)) &&
                    (goalpostX > 0 && goalpostX < screenX)) && !sameGoalPost && (goalpostX > screenX / 2 + 5 || goalpostX < screenX / 2 - 5)) {
                Log.d("Bounce", "Ball collided with goalpost " + edgeVectorIndex);
                Log.d("Bounce", "Time since last bounce: " + (System.currentTimeMillis() - lastBounceTime));
                Log.d("Bounce", "GoalpostX: " + goalpostX);
                // Log screenx / 2
                Log.d("Bounce", "Screenx / 2: " + screenX / 2);

                lastBouncedEdgeIndex = edgeVectorIndex;
                lastGoalpostIndex = edgeVectorIndex;
                lastBounceTime = currentTime;

                double normalX = getX() - goalpostX;
                double normalY = getY() - goalpostY;
                double normalLength = Math.sqrt(normalX * normalX + normalY * normalY);
                normalX /= normalLength;
                normalY /= normalLength;

                reflectBall(normalX, normalY);
                resolveOverlap(normalX, normalY, postRadius - Math.min(distanceToStart, distanceToEnd));
                Log.d("Bounce", "Ball velocity: (" + getVelocityX() + ", " + getVelocityY() + ")");
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

        double distanceToEdge = Math.sqrt(Math.pow(getX() - closestX, 2) + Math.pow(getY() - closestY, 2));

        if (distanceToEdge <= radius) {
            Log.d("Bounce", "Ball collided with edge " + edgeVectorIndex);
            Log.d("Bounce", "Time since last bounce: " + (System.currentTimeMillis() - lastBounceTime));
            Log.d("Bounce", "Last bounce edge: " + lastBouncedEdgeIndex);
            lastBouncedEdgeIndex = edgeVectorIndex;
            lastGoalpostIndex = -1;
            lastBounceTime = currentTime;

            double normalX = -edgeDY;
            double normalY = edgeDX;
            double normalLength = Math.sqrt(normalX * normalX + normalY * normalY);
            normalX /= normalLength;
            normalY /= normalLength;

            // Ensure the ball bounces in the correct direction (ball passed through the edge case)
            if ((getVelocityX() * normalX + getVelocityY() * normalY) > 0) {
                normalX = -normalX;
                normalY = -normalY;
            }

            reflectBall(normalX, normalY);
            resolveOverlap(normalX, normalY, radius - distanceToEdge);

            Log.d("Bounce", "Ball velocity: (" + getVelocityX() + ", " + getVelocityY() + ")");
        }
    }

    private void reflectBall(double normalX, double normalY) {
        double velocityX = getVelocityX();
        double velocityY = getVelocityY();
        double dotProductVelocity = velocityX * normalX + velocityY * normalY;
        setVelocityX((float) (velocityX - 2 * dotProductVelocity * normalX));
        setVelocityY((float) (velocityY - 2 * dotProductVelocity * normalY));
    }

    private void resolveOverlap(double normalX, double normalY, double overlap) {
        if (overlap > 0) {
            setX((float) (getX() + normalX * overlap));
            setY((float) (getY() + normalY * overlap));
        }
    }

    public Vector getUnitDirectionVector(double dx, double dy) {
        return new Vector(0, 0, dx, dy);
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

    public float getX() {
        return x;
    }

    public synchronized void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public synchronized void setY(float y) {
        this.y = y;
    }

    public float getRadius() {
        return radius;
    }

    public synchronized void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }

    public synchronized void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }

    public synchronized float getVelocityX() {
        return this.velocityX;
    }

    public synchronized float getVelocityY() {
        return this.velocityY;
    }

    public Player getPlayer() {
        return this.player;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
    public void shoot(int shootSpeed) {
        Log.d("Shoot", player == null ? "Player null for ball" : "Ball has a player");
        if (player != null) {
            shooter = player;
            // Calculate the direction from ball to player (ballX - playerX, ballY - playerY)
            float dx = this.x - player.getX();  // Ball's position minus Player's position (shoot away from player)
            float dy = this.y - player.getY();  // Ball's position minus Player's position (shoot away from player)

            // Normalize direction vector (dx, dy)
            float magnitude = (float) Math.sqrt(dx * dx + dy * dy);

            // Only normalize if magnitude is significant
            if (magnitude > 0.001f) {
                dx /= magnitude;
                dy /= magnitude;
            } else {
                // Set a default direction if magnitude is too small
                dx = 1;
                dy = 0;
            }

            // Set the ball's velocity to move away from the player
            this.velocityX = dx * shootSpeed;
            this.velocityY = dy * shootSpeed;

            // Once the ball is shot, release it from the player
            this.player.releaseBall();
            this.player = null;
            Log.d("Shoot", "Ball was shot");
        }
    }

    public void reset(int resetX, int resetY) {
        this.x = resetX;
        this.y = resetY;
        this.velocityX = 0;
        this.velocityY = 0;
        this.player = null;
        this.shooter = null;
    }
}