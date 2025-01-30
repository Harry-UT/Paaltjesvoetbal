package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
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
    private Paint ballPaint;
    private Player shooter = null;

    public Ball(float x, float y, float radius, List<Vector> bounceEdges) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.velocityX = 0;
        this.velocityY = 0;
        this.bounceEdges = bounceEdges;
        initializePaint();
    }

    private void initializePaint() {
        // Create a paint object for the ball
        ballPaint = new Paint();
        ballPaint.setAntiAlias(true);

        // Create a shader to fill the ball with a gradient from black to white
        Shader shader = new LinearGradient(
                x - radius, y,           // Start at the left side of the circle
                x + radius, y,           // End at the right side of the circle
                Color.BLACK,             // Left side color (black)
                Color.WHITE,             // Right side color (white)
                Shader.TileMode.CLAMP    // The gradient will not repeat, it will clamp the color at the edges
        );

        // Apply the shader to the paint
        ballPaint.setShader(shader);
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

    public void drawNormalVectors(Canvas canvas) {
        Paint paint = new Paint();
        // Draw normal vectors of the edges from the middle of screen
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        paint = new Paint();
        for (Vector edge : bounceEdges) {
            // Draw the normal vector or the edge * 50 length for visibility
            Vector normalVector = getNormalVector(edge);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawLine((float) normalVector.getX1(), (float) normalVector.getY1(), (float) (normalVector.getX1() + (normalVector.getX2() - normalVector.getX1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), (float) (normalVector.getY1() + (normalVector.getY2() - normalVector.getY1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), paint);
        }
    }

    public void update(int screenX, int screenY) {
        if (this.player == null) {
            // Check for screen boundary collisions
            checkBounce(screenX, screenY);

            // Update ball position based on its velocity
            x += velocityX;
            y += velocityY;

            // reduce velocity
            setVelocityX(getVelocityX() * DAMPING_FACTOR);
            setVelocityY(getVelocityY() * DAMPING_FACTOR);
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

    public Player getShooter() {
        return this.shooter;
    }

    public void resetShooter() {
        this.shooter = null;
    }

    private void checkBounce(float screenX, float screenY) {
        // Check for collision with the left edge
        if (getX() - radius < 0) {
            if (getVelocityX() < 0) {
                setVelocityX(-getVelocityX());  // Reverse horizontal direction
            }
        }

        // Check for collision with the right edge
        if (getX() + radius > screenX) {
            if (getVelocityX() > 0) {
                setVelocityX(-getVelocityX());  // Reverse horizontal direction
            }
        }

        // Check for collisions with top and bottom edges
        if (getY() - radius < 0) {
            if (getVelocityY() < 0) {
                setVelocityY(-getVelocityY());  // Reverse vertical direction
            }
        }

        if (getY() + radius > screenY) {
            if (getVelocityY() > 0) {
                setVelocityY(-getVelocityY());  // Reverse vertical direction
            }
        }

        // Check for collisions with the specific polygon edges (4 edges only)
        for (int i = 0; i < bounceEdges.size(); i++) {
            checkEdgeCollision(i);
        }
    }

    // Check for collision with the ball and the passed polygon edge
    public void checkEdgeCollision(int edgeVectorIndex) {
        Vector edge = bounceEdges.get(edgeVectorIndex);

        // Get the edge's start and end points
        double x1 = edge.getX1();
        double y1 = edge.getY1();
        double x2 = edge.getX2();
        double y2 = edge.getY2();

        // Calculate the vector representing the edge
        double edgeDX = x2 - x1;
        double edgeDY = y2 - y1;

        // Calculate the vector from the ball's center to the start of the edge
        double ballDX = getX() - x1;
        double ballDY = getY() - y1;

        // Project the ball's position onto the edge line
        double dotProduct = ballDX * edgeDX + ballDY * edgeDY;
        double edgeLengthSquared = edgeDX * edgeDX + edgeDY * edgeDY;
        double projection = dotProduct / edgeLengthSquared;

        // If the projection falls outside the edge, use the closest endpoint
        double closestX, closestY;
        if (projection < 0) {
            // Closest point is the start of the segment
            closestX = x1;
            closestY = y1;
        } else if (projection > 1) {
            // Closest point is the end of the segment
            closestX = x2;
            closestY = y2;
        } else {
            // Closest point is somewhere on the segment
            closestX = x1 + projection * edgeDX;
            closestY = y1 + projection * edgeDY;
        }

        // Calculate the distance from the ball to the closest point on the edge
        double distanceToEdge = Math.sqrt(Math.pow(getX() - closestX, 2) + Math.pow(getY() - closestY, 2));

        // If the distance is less than or equal to the ball's radius, we have a collision
        if (distanceToEdge <= radius) {
            // Get the normal vector of the edge (perpendicular to the edge)
            Vector normalVector = getNormalVector(edge); // Get the normal vector
            float normalX = (float) (normalVector.getX2() - normalVector.getX1());  // X component of the normal
            float normalY = (float) (normalVector.getY2() - normalVector.getY1());  // Y component of the normal

            // Normalize the normal vector (though it should already be normalized)
            float normalLength = (float) Math.sqrt(normalX * normalX + normalY * normalY);
            normalX /= normalLength;
            normalY /= normalLength;

            // If the ball is moving towards the edge, reflect the ball's velocity off the edge
            if (isDeviationGreaterThan90(normalVector, getUnitDirectionVector(getVelocityX(), getVelocityY()))) {
                // Get current velocity components
                double velocityX = getVelocityX();
                double velocityY = getVelocityY();

                // Calculate the dot product of the ball's velocity and the line's normal
                double dotProductVelocity = velocityX * normalX + velocityY * normalY;

                // Reflect the ball's velocity using the normal vector
                setVelocityX((float) (velocityX - 2 * dotProductVelocity * normalX)); // Reflect along x-axis
                setVelocityY((float) (velocityY - 2 * dotProductVelocity * normalY)); // Reflect along y-axis

                // Optionally, adjust the ball's position to prevent overlap (correct position)
                double overlap = radius - distanceToEdge;
                setX((float) (getX() + normalX * overlap)); // Adjust X position
                setY((float) (getY() + normalY * overlap)); // Adjust Y position
            }
        }
    }

    public Vector getUnitDirectionVector(double dx, double dy) {
        return new Vector(0, 0, dx, dy);
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

    private Vector getNormalVector(Vector vector) {
        float edgeX = (float) (vector.getX2() - vector.getX1());
        float edgeY = (float) (vector.getY2() - vector.getY1());
        float edgeLength = (float) Math.sqrt(edgeX * edgeX + edgeY * edgeY);
        float normalX = -edgeY / edgeLength;
        float normalY = edgeX / edgeLength;

        // Draw normal vector from the middle of the edge
        float startX = (float) (vector.getX1() + edgeX / 2);
        float startY = (float) (vector.getY1() + edgeY / 2);
        float endX = startX + normalX;
        float endY = startY + normalY;
        return new Vector(startX, startY, endX, endY);
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

    public synchronized float getVelocityX() {
        return this.velocityX;
    }

    public synchronized float getVelocityY() {
        return this.velocityY;
    }

    public Player getPlayer() {
        return this.player;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
    public void shoot(int shootSpeed) {
        Log.d("Shoot", player == null ? "Player null for ball" : "Ball has a player");
        if (player != null) {
            shooter = player;
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
            this.velocityX = dx * shootSpeed;
            this.velocityY = dy * shootSpeed;

            // Once the ball is shot, release it from the player
            this.player.releaseBall();
            this.player = null;
            Log.d("Shoot", "Ball was shot");
        }
    }
}