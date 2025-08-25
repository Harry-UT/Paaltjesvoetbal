package com.example.paaltjesvoetbal.model;

public class GoalPost {
    private float x, y;      // center of the post
    private float radius;    // post radius

    public GoalPost(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void bounceBall(Ball ball) {
        // Vector from post to ball
        float dx = ball.getX() - x;
        float dy = ball.getY() - y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);

        if (dist <= ball.getRadius() + radius) {  // collision
            // Normal vector
            float nx = dx / dist;
            float ny = dy / dist;

            // Reflect velocity
            float dot = ball.getVelocityX() * nx + ball.getVelocityY() * ny;
            ball.setVelocityX(ball.getVelocityX() - 2 * dot * nx);
            ball.setVelocityY(ball.getVelocityY() - 2 * dot * ny);

            // Move ball out of post
            ball.setX(x + nx * (ball.getRadius() + radius));
            ball.setY(y + ny * (ball.getRadius() + radius));
        }
    }
}