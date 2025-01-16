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
    private List<Vector> edges;

//    private long lastBounceTime = 0;
//    private final long BOUNCE_TIMEOUT = 100; // 100 milliseconds timeout

    public Ball(float x, float y, float radius, List<Vector> edges) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.velocityX = 0;
        this.velocityY = 0;
        this.edges = edges;
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

        paint = new Paint();
        // Draw normal vectors of the edges from the middle of screen
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        for (Vector edge : edges) {
            // Calculate normal vector of edge
            float edgeX = (float) (edge.getX2() - edge.getX1());
            float edgeY = (float) (edge.getY2() - edge.getY1());
            float edgeLength = (float) Math.sqrt(edgeX * edgeX + edgeY * edgeY);
            float normalX = -edgeY / edgeLength;
            float normalY = edgeX / edgeLength;

            // Draw normal vector from the middle of the edge
            float startX = (float) (edge.getX1() + edgeX / 2);
            float startY = (float) (edge.getY1() + edgeY / 2);

            float endX = startX + normalX;
            float endY = startY + normalY;

            canvas.drawLine(startX, startY, endX, endY, paint);
        }
        paint = new Paint();
        for (Vector edge : edges) {
            // Draw the normal vector or the edge * 50 length for visibility
            Vector normalVector = getNormalVector(edge);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawLine((float) normalVector.getX1(), (float) normalVector.getY1(), (float) (normalVector.getX1() + (normalVector.getX2() - normalVector.getX1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), (float) (normalVector.getY1() + (normalVector.getY2() - normalVector.getY1()) * 50 / Math.sqrt(Math.pow(normalVector.getX2() - normalVector.getX1(), 2) + Math.pow(normalVector.getY2() - normalVector.getY1(), 2))), paint);
        }
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
            if (getVelocityX() != 0) {
                setVelocityX(-getVelocityX());  // Reverse horizontal direction
            }
        }

        // Check for collisions with top and bottom edges
        if (y - radius <= 0 || y + radius >= screenY) {
            setVelocityY(-getVelocityY());  // Reverse vertical direction
        }

        // Check for collisions with the specific polygon edges (4 edges only)
        for (int i = 0; i < 4; i++) {
            checkEdgeCollision(i);
        }
    }

    // Check for collision with the ball and the passed polygon edge
    public void checkEdgeCollision(int edgeIndex) {
        Vector edge = edges.get(edgeIndex);

        // Get the edge's start and end points
        double x1 = edge.getX1();
        double y1 = edge.getY1();
        double x2 = edge.getX2();
        double y2 = edge.getY2();

        Vector normalVector = getNormalVector(edge);

        // Calculate the vector from the ball's center to the line
        double dx = getX() - x1;
        double dy = getY() - y1;

        // Calculate the perpendicular distance from the ball to the line
        double lineLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        double distance = Math.abs(dy * (x2 - x1) - dx * (y2 - y1)) / lineLength;

        // If the distance is less than or equal to the ball's radius, we have a collision
        if (distance <= radius) {
            if (isDeviationGreaterThan90(normalVector, getUnitDirectionVector(getVelocityX(), getVelocityY()))) {
                // Reflect the ball's velocity off the line
                double normalX = (y2 - y1) / lineLength;
                double normalY = (x1 - x2) / lineLength;

                // Get current velocity components
                double velocityX = getVelocityX();
                double velocityY = getVelocityY();

                // Calculate the dot product of the ball's velocity and the line's normal
                double dotProduct = velocityX * normalX + velocityY * normalY;

                // Reflect velocity along x-axis and y-axis using set methods
                setVelocityX((float) (velocityX - 2 * dotProduct * normalX)); // Reflect along x-axis
                setVelocityY((float) (velocityY - 2 * dotProduct * normalY)); // Reflect along y-axis

                // Optionally, adjust the ball's position to prevent overlap (correct position)
                double overlap = radius - distance;
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

    // Normalize a vector
    private void normalize(double[] vector) {
        double length = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
        vector[0] /= length;
        vector[1] /= length;
    }

    // Reflect the ball's velocity based on the normal vector
    private void reflectVelocity(double[] normal) {
        double dotProduct = velocityX * normal[0] + velocityY * normal[1];
        velocityX -= (float) (2 * dotProduct * normal[0]);
        velocityY -= (float) (2 * dotProduct * normal[1]);
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