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

    public Player getShooter() {
        return this.shooter;
    }

    public void resetShooter() {
        this.shooter = null;
    }

    public void reflect(double normalX, double normalY) {
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

    public void incrementXY() {
        this.x += velocityX;
        this.y += velocityY;
    }

    public void updateVelocity() {
        this.velocityX *= DAMPING_FACTOR;
        this.velocityY *= DAMPING_FACTOR;
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

    public int getLastBouncedEdgeIndex() {
        return this.lastBouncedEdgeIndex;
    }

    public void setLastBouncedEdgeIndex(int index) {
        this.lastBouncedEdgeIndex = index;
    }

    public int getLastGoalpostIndex() {
        return this.lastGoalpostIndex;
    }

    public void setLastGoalpostIndex(int index) {
        this.lastGoalpostIndex = index;
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