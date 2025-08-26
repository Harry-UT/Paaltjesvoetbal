package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Player {
    private float x, y;  // Position of the player
    private final int radius;  // Player's radius
    private int color;  // Player's color
    private int score = 0;  // Player's score
    private Joystick joystick;
    private ShootButton shootButton;
    private Ball ball;
    private static final int CONTROL_TIMEOUT = 200;  // Time in milliseconds for collision timeout
    private long lastShootTime;  // Last time the ball was shot
    private final int[] scorePosition = new int[] { 0, 0 };

    // Constructor to initialize the player
    public Player(float x, float y, int radius, int color, int playerNumber) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.lastShootTime = System.currentTimeMillis();
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getRadius() {
        return radius;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    // Getter for the player's direction from the joystick
    public float getDirection() {
        return joystick.getDirection();
    }

    public ShootButton getShootButton() {
        return shootButton;
    }

    public Ball getBall() {
        return ball;
    }

    // Set the ball that the player is controlling
    public void setBall(Ball ball) {
        this.ball = ball;
        lastShootTime = System.currentTimeMillis();
    }

    // Method to release the ball (after shooting it)
    public void releaseBall() {
        lastShootTime = System.currentTimeMillis();
        ball = null;
    }

    /** Check if the player can take the ball (based on the timeout)
     * @return true if the player can take the ball, false otherwise
     */
    public boolean canTakeBall() {
        return System.currentTimeMillis() - lastShootTime >= CONTROL_TIMEOUT;
    }

    /** Draw the player on the canvas
     * @param canvas the canvas to draw on
     */
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        canvas.drawCircle(x, y, radius, paint);  // Draw the player as a circle
    }

    // Getters and setters for Joystick and ShootButton
    public void setJoystick(Joystick joystick) {
        this.joystick = joystick;
    }

    public void setShootButton(ShootButton shootButton) {
        this.shootButton = shootButton;
    }

    public void incrementScore() {
        score++;
    }

    public int getScore() {
        return score;
    }

    public void resetScore() {
        score = 0;
    }

    public void setScorePosition(int x, int y) {
        scorePosition[0] = x;
        scorePosition[1] = y;
    }

    public int[] getScorePosition() {
        return scorePosition;
    }

    /** Reset player position
     * @param x new x position
     * @param y new y position
     */
    public void resetPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Reset player position and score
     * @param x new x position
     * @param y new y position
     */
    public void reset(int x, int y) {
        this.x = x;
        this.y = y;
        this.score = 0;
    }
}