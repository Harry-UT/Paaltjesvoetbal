package com.example.paaltjesvoetbal;  // Adjust the package name to your actual package

import android.graphics.Canvas;
import android.graphics.Paint;

public class Ball {
    private float x, y, radius;
    private int color;
    private float velocityX, velocityY;
    private static final float DAMPING_FACTOR = 0.98F;

    public Ball(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);
    }

    // Update the ball's position and apply velocity decay
    public void update(float screenX, float screenY) {
        // Apply damping factor to simulate velocity decay
        velocityX *= DAMPING_FACTOR;
        velocityY *= DAMPING_FACTOR;

        // Update position based on velocity
        x += velocityX;
        y += velocityY;

        // Constrain the ball within the screen boundaries and make it bounce
        bounce(screenX, screenY);
    }

    private void bounce(float screenX, float screenY) {
        // Check for collisions with left and right edges
        if (x - radius <= 0 || x + radius >= screenX) {
            velocityX = -velocityX;  // Reverse horizontal direction
        }

        // Check for collisions with top and bottom edges
        if (y - radius <= 0 || y + radius >= screenY) {
            velocityY = -velocityY;  // Reverse vertical direction
        }
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

    public float getRadius() {
        return radius;
    }

    public void setVelocity(float velocityX, float velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }
}