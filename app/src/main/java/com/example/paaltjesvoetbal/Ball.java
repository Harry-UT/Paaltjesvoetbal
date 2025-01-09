package com.example.paaltjesvoetbal;  // Adjust the package name to your actual package

import android.graphics.Canvas;
import android.graphics.Paint;

public class Ball {
    private float x;
    private float y;
    private final float radius;
    private final int color;
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

    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
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
        if (player != null) {
            // Calculate the direction from ball to player (ballX - playerX, ballY - playerY)
            float dx = this.x - player.getX();  // Ball's position minus Player's position (shoot away from player)
            float dy = this.y - player.getY();  // Ball's position minus Player's position (shoot away from player)

            // Normalize the direction vector (unit vector)
            float magnitude = (float) Math.sqrt(dx * dx + dy * dy);
            if (magnitude > 0) {
                dx /= magnitude;
                dy /= magnitude;
            }

            // Set the ball's velocity to move away from the player
            float shootSpeed = 30;  // Adjust this value to control how fast the ball shoots
            this.velocityX = dx * shootSpeed;
            this.velocityY = dy * shootSpeed;

            // Once the ball is shot, release it from the player
            this.player.releaseBall();
            this.player = null;
        }
    }
}