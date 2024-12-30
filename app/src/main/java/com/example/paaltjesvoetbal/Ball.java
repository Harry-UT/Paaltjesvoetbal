package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Ball {
    private float x, y, radius;
    private int color;
    private float velocityX, velocityY;

    // Constructor
    public Ball(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    // Draw the ball
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);
    }

    // Getters and setters for position, radius, and velocity
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

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    // Getters and setters for velocity
    public float getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }

    // Ball bouncing logic
    public void bounce(float screenX, float screenY) {
        // Check for collisions with left and right edges
        if (x - radius <= 0 || x + radius >= screenX) {
            velocityX = -velocityX;  // Reverse horizontal direction
        }

        // Check for collisions with top and bottom edges
        if (y - radius <= 0 || y + radius >= screenY) {
            velocityY = -velocityY;  // Reverse vertical direction
        }
    }

    // Move the ball based on its velocity
    public void updatePosition() {
        x += velocityX;
        y += velocityY;
    }
}