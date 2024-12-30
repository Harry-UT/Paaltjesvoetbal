package com.example.paaltjesvoetbal;  // Adjust the package name to your actual package

import android.graphics.Canvas;
import android.graphics.Paint;

public class Ball {
    private float x, y, radius;
    private int color;
    private float velocityX, velocityY;
    private static final float DAMPING_FACTOR = 0.98F;
    private Player player;

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

    public void update(int screenX, int screenY) {
        if (this.player == null) {
            // Update ball position based on its velocity
            x += velocityX;
            y += velocityY;

            // Check for screen boundary collisions
            if (x - radius < 0 || x + radius > screenX) {
                velocityX = -velocityX;  // Reverse horizontal direction
            }
            if (y - radius < 0 || y + radius > screenY) {
                velocityY = -velocityY;  // Reverse vertical direction
            }
        } else {
            // Update the ball's position based on the player's direction
            float direction = this.player.getDirection();  // Get the player's direction (angle in radians)

            // Define a distance to move the ball from the player (e.g., just in front of the player)
            float combinedRadius = this.player.getRadius() + this.radius; // Combine player and ball radii

            // Calculate new position based on direction, adjusted by combined radius
            x = this.player.getX() + (float) Math.cos(direction) * combinedRadius;
            y = this.player.getY() + (float) Math.sin(direction) * combinedRadius;
        }
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

    public Player getPlayer() {
        return this.player;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
}