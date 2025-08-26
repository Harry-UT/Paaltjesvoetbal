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

    public Player getShooter() {
        return this.shooter;
    }

    public void resetShooter() {
        this.shooter = null;
    }

    /** Reflect the ball's velocity based on a normal vector
     * @param normalX X component of the normal vector
     * @param normalY Y component of the normal vector
     */
    public void reflect(double normalX, double normalY) {
        double velocityX = getVelocityX();
        double velocityY = getVelocityY();
        double dotProductVelocity = velocityX * normalX + velocityY * normalY;
        setVelocityX((float) (velocityX - 2 * dotProductVelocity * normalX));
        setVelocityY((float) (velocityY - 2 * dotProductVelocity * normalY));
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

    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }

    public void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }

    public float getVelocityX() {
        return this.velocityX;
    }

    public float getVelocityY() {
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

    public void reset(int resetX, int resetY) {
        this.x = resetX;
        this.y = resetY;
        this.velocityX = 0;
        this.velocityY = 0;
        this.player = null;
        this.shooter = null;
    }
}