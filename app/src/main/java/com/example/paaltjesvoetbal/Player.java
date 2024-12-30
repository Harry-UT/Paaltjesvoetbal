package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Player {
    private float x, y;  // Position of the player
    private int radius;  // Player's radius
    private int color;  // Player's color
    private Ball ball;  // Reference to the ball the player is controllin

    private Joystick joystick;

    // Constructor to initialize the player
    public Player(float x, float y, int radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.ball = null;  // Initially, the player is not controlling any ball
    }

    // Getter and setter for the player's position
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

    // Getter and setter for the player's radius
    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    // Getter and setter for the player's color
    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    // Getter and setter for the player's direction
    public float getDirection() {
        return joystick.getDirection();
    }

    // Getter for the ball the player is controlling
    public Ball getBall() {
        return ball;
    }

    // Set the ball that the player is controlling
    public void setBall(Ball ball) {
        this.ball = ball;
    }

    // Draw method for rendering the player
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);  // Draw the player as a circle
    }

    public void setJoystick(Joystick joystick) {
        this.joystick = joystick;
    }
}