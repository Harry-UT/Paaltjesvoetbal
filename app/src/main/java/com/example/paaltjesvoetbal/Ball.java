package com.example.paaltjesvoetbal;  // Adjust the package name to your actual package

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.Log;
import android.graphics.Color;

public class Ball {
    private float x;
    private float y;
    private final float radius;
    private float velocityX, velocityY;
    private static final float DAMPING_FACTOR = 0.98F;
    private Player player;

    public Ball(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    public void draw(Canvas canvas) {
        // Create a paint object for the ball
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Create a shader to fill the ball with two colors (black and white)
        // Adjust the gradient's positions to favor more white area
        Shader shader = new LinearGradient(
                x - radius, y, // Start at the left side of the circle
                x + radius * 0.5f, y, // End at the mid-point of the circle (more white)
                Color.BLACK, // Color on the left half
                Color.WHITE, // Color on the right half
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);

        // Draw the ball with the gradient effect
        canvas.drawCircle(x, y, radius, paint);
    }

    public void update(int screenX, int screenY) {
        if (this.player == null) {
            // Update ball position based on its velocity
            x += velocityX;
            y += velocityY;

            // reduce velocity
            setVelocityX(getVelocityX() * DAMPING_FACTOR);
            setVelocityY(getVelocityY() * DAMPING_FACTOR);

            // Check for screen boundary collisions
            checkBounce(screenX, screenY);
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
    }

    private void checkBounce(float screenX, float screenY) {
        // Check for collisions with left and right edges
        if (x - radius <= 0 || x + radius >= screenX) {
            setVelocityX(-getVelocityX());  // Reverse horizontal direction
        }

        // Check for collisions with top and bottom edges
        if (y - radius <= 0 || y + radius >= screenY) {
            setVelocityY(-getVelocityY());  // Reverse vertical direction
        }
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

    public void shoot() {
        Log.d("Shoot", player == null ? "Player null for ball" : "Ball has a player");
        if (player != null) {
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
            float shootSpeed = 30;  // Adjust this value to control how fast the ball shoots
            this.velocityX = dx * shootSpeed;
            this.velocityY = dy * shootSpeed;

            // Once the ball is shot, release it from the player
            this.player.releaseBall();
            this.player = null;
            Log.d("Shoot", "Ball was shot");
        }
    }
}