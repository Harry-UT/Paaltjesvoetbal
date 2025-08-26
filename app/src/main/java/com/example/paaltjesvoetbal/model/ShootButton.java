package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class ShootButton {
    private final Paint paint;
    private Ball ball;
    private final float x;
    private final float y;
    private final float radius;
    private boolean isPressed;
    private int color;
    private int touchID = -1; // Store the pointer ID for the current touch

    public ShootButton(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        isPressed = false;
        paint = new Paint();
    }

    /** Draw the shoot button on the provided canvas
     * @param canvas Canvas to draw the button on
     */
    public void draw(Canvas canvas) {
        // Draw the outer circle with color based on pressed state
        paint.setColor(isPressed ? Color.DKGRAY : Color.LTGRAY);
        canvas.drawCircle(x, y, radius, paint);

        // Draw the inner circle using the 'color' field
        paint.setColor(color); // Use the 'color' field for the inner circle
        float innerRadius = radius * 0.6f; // Smaller radius for the inner circle
        canvas.drawCircle(x, y, innerRadius, paint);
    }

    /**
     * Check if the button was touched
     * @param touchX touch x-coordinate
     * @param touchY touch y-coordinate
     * @return true if the button was touched, false otherwise
     */
    public boolean isTouched(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        float distanceSquared = dx * dx + dy * dy;
        return distanceSquared <= (radius * 4.5f) * (radius * 4.5f);
    }

    public Ball getBall() {
        return ball;
    }

    // Set the ball object for the shoot button
    public void setBall(Ball ball) {
        this.ball = ball;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    // Update pressed state
    public void setPressed(boolean pressed) {
        isPressed = pressed;
    }

    // Set the pointer ID for the current touch
    public void setTouchID(int pointerId) {
        this.touchID = pointerId;
    }

    // Check if the button was touched by a specific pointer ID
    public boolean wasTouchedBy(int pointer) {
        return this.touchID == pointer;
    }

    // Handle pointer release
    public void resetTouchID() {
        this.touchID = -1;
    }

    public void resetBall() {
        this.ball = null;
    }
}