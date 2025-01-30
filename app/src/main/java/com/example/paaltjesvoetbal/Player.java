package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Player {
    private float x, y;  // Position of the player
    private final int radius;  // Player's radius
    private int color;  // Player's color
    private int score = 0;  // Player's score
    private Ball ball;  // Reference to the ball the player is controlling
    private Joystick joystick;
    private ShootButton shootButton;
    private static final int CONTROL_TIMEOUT = 200;  // Time in milliseconds for collision timeout
    private long lastShootTime;  // Last time the ball was shot
    private int number;

    // Constructor to initialize the player
    public Player(float x, float y, int radius, int color, int playerNumber) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.ball = null;  // Initially, the player is not controlling any ball
        this.lastShootTime = 0;
        this.number = playerNumber;
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

    // Set the ball that the player is controlling
    public void setBall(Ball ball) {
        this.shootButton.setBall(ball);
        lastShootTime = System.currentTimeMillis();
    }

    // Method to release the ball (after shooting it)
    public void releaseBall() {
        this.lastShootTime = System.currentTimeMillis();
    }

    public boolean canTakeBall() {
        return System.currentTimeMillis() - lastShootTime >= CONTROL_TIMEOUT;
    }

    // Draw method for rendering the player
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);  // Draw the player as a circle
    }

    // Setters and getters for Joystick and ShootButton
    public void setJoystick(Joystick joystick) {
        this.joystick = joystick;
    }

    public void setShootButton(ShootButton shootButton) {
        this.shootButton = shootButton;
    }

    public void scored() {
        score++;
    }

    public int getScore() {
        return score;
    }

    public void resetScore() {
        score = 0;
    }

    public int getNumber() {
        return number;
    }
}