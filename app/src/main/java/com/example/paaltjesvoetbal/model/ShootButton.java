package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class ShootButton {
    private final Paint paint;
    private final float x;
    private final float y;
    private final float radius;
    private int color;
    private int pointerID = -1; // Store the pointer ID for the current touch

    /** Constructor to initialize the shoot button
     * @param x X-coordinate of the button center
     * @param y Y-coordinate of the button center
     * @param radius Radius of the button
     * @param color Color of the inner circle of the button
     */
    public ShootButton(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        paint = new Paint();
    }

    /** Draw the shoot button on the provided canvas
     * @param canvas Canvas to draw the button on
     */
    public void draw(Canvas canvas) {
        // Draw the outer circle with color based on pressed state
        paint.setColor(pointerID != -1 ? Color.DKGRAY : Color.LTGRAY);
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    // Set the pointer ID that's currently touching the button
    public void setPointerID(int pointerId) {
        this.pointerID = pointerId;
    }

    // Check if the button is hold by a specific pointer ID
    public boolean wasTouchedBy(int pointer) {
        return this.pointerID == pointer;
    }

    // Handle pointer release
    public void resetTouchID() {
        this.pointerID = -1;
    }
}