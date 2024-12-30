package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

public class Joystick {
    private int joystickBaseRadius = 150;
    private int joystickStickRadius = 50;
    private PointF joystickBaseCenter;
    private PointF joystickStickCenter;
    private int joystickDragRadius = 300; // Area larger than the base for dragging
    private Paint paint;
    private Player player; // Player reference to control

    public Joystick(int screenX, int screenY, Player player) {
        joystickBaseCenter = new PointF(screenX - joystickBaseRadius - 50, screenY - joystickBaseRadius - 50);
        joystickStickCenter = new PointF(joystickBaseCenter.x, joystickBaseCenter.y); // Initially at the center
        paint = new Paint();
        this.player = player;  // Set the player that the joystick will control
    }

    public void draw(Canvas canvas) {
        // Draw the joystick base (the large circle)
        paint.setColor(Color.GRAY);
        canvas.drawCircle(joystickBaseCenter.x, joystickBaseCenter.y, joystickBaseRadius, paint);

        // Draw the joystick stick (the small circle)
        paint.setColor(Color.BLACK);
        canvas.drawCircle(joystickStickCenter.x, joystickStickCenter.y, joystickStickRadius, paint);
    }

    public void onTouchEvent(float touchX, float touchY) {
        // Check if the touch is within the extended drag area
        float distanceFromBaseCenter = (float) Math.sqrt(Math.pow(touchX - joystickBaseCenter.x, 2) + Math.pow(touchY - joystickBaseCenter.y, 2));
        if (distanceFromBaseCenter <= joystickDragRadius) {
            // Update the joystick stick position based on the touch
            joystickStickCenter.set(touchX, touchY);

            // Constrain the joystick stick movement within the base circle
            float distance = (float) Math.sqrt(Math.pow(joystickStickCenter.x - joystickBaseCenter.x, 2) + Math.pow(joystickStickCenter.y - joystickBaseCenter.y, 2));
            if (distance > joystickBaseRadius - joystickStickRadius) {
                // Calculate the stick's position based on the base radius
                float scale = (joystickBaseRadius - joystickStickRadius) / distance;
                joystickStickCenter.x = joystickBaseCenter.x + (joystickStickCenter.x - joystickBaseCenter.x) * scale;
                joystickStickCenter.y = joystickBaseCenter.y + (joystickStickCenter.y - joystickBaseCenter.y) * scale;
            }
        }
        // Update player position based on joystick input
        movePlayer();
    }

    private void movePlayer() {
        // Calculate movement direction from joystick stick position
        float deltaX = joystickStickCenter.x - joystickBaseCenter.x;
        float deltaY = joystickStickCenter.y - joystickBaseCenter.y;

        // Normalize the direction vector
        float magnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (magnitude > 0) {
            deltaX /= magnitude;
            deltaY /= magnitude;
        }

        // Calculate the distance from the center to determine how fast to move
        float distanceFromCenter = (float) Math.sqrt(Math.pow(joystickStickCenter.x - joystickBaseCenter.x, 2) + Math.pow(joystickStickCenter.y - joystickBaseCenter.y, 2));

        // Scale the movement speed based on distance from center (larger distance = faster movement)
        // For faster movement the further away the joystick is pushed
        float speedFactor = Math.min(1.5f, distanceFromCenter / joystickBaseRadius);  // The speed factor increases with distance

        // Move the controlled player based on joystick direction and speed factor
        player.setX(player.getX() + deltaX * 10 * speedFactor);  // Speed of player movement, scaled by the distance
        player.setY(player.getY() + deltaY * 10 * speedFactor);

        // Constrain the player to screen bounds
        player.setX(Math.max(player.getRadius(), Math.min(player.getX(), joystickBaseCenter.x)));
        player.setY(Math.max(player.getRadius(), Math.min(player.getY(), joystickBaseCenter.y)));
    }

    public void reset() {
        // Reset joystick stick position when touch is released
        joystickStickCenter.set(joystickBaseCenter.x, joystickBaseCenter.y);
    }

    public PointF getStickPosition() {
        return joystickStickCenter;
    }

    public PointF getBaseCenter() {
        return joystickBaseCenter;
    }
}