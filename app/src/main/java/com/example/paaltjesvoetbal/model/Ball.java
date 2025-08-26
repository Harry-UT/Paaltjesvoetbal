package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class Ball {
    private float x;
    private float y;
    private final float radius;
    private float velocityX, velocityY;
    private Paint ballPaint;
    private Player shooter = null;
    private boolean shot = true;
    private int lastBouncedEdgeIndex = -1;
    private int lastGoalpostIndex = -1;
    public Ball(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.velocityX = 0;
        this.velocityY = 0;
        initializePaint();
    }

    private void initializePaint() {
        // Create a paint object for the ball
        ballPaint = new Paint();
        ballPaint.setAntiAlias(true);
        ballPaint.setColor(Color.DKGRAY);
    }

    /** Draw the ball on the provided canvas
     * @param canvas Canvas to draw the ball on
     */
    public void draw(Canvas canvas) {
        // Draw the ball with the gradient effect
        canvas.drawCircle(x, y, radius, ballPaint);
    }

    public Player getShooter() {
        return this.shooter;
    }

    public void setShooter(Player shooter) {
        this.shooter = shooter;
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

    public float getX() {
        return x;
    }

    public  void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRadius() {
        return radius;
    }

    public void updatePosition() {
        this.x += velocityX;
        this.y += velocityY;
    }

    public void decreaseVelocity(float dampingFactor) {
        this.velocityX *= dampingFactor;
        this.velocityY *= dampingFactor;
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

    public boolean isShot() {
        return this.shot;
    }

    public void setShot(boolean shot) {
        this.shot = shot;
    }

    /** Resets the ball to a specified position and clears its state
     * @param resetX X coordinate to reset the ball to
     * @param resetY Y coordinate to reset the ball to
     */
    public void reset(int resetX, int resetY) {
        this.x = resetX;
        this.y = resetY;
        this.velocityX = 0;
        this.velocityY = 0;
        this.shooter = null;
    }
}