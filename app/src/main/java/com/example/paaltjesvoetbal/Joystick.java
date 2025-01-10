package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

public class Joystick {
    private final float baseCenterX;
    private final float baseCenterY;
    private float stickX, stickY;
    private int touchID = -1;
    private static final float JOYSTICK_RADIUS = 100; // Define joystick's maximum radius

    // Constructor to initialize the joystick position and player reference
    public Joystick(float baseCenterX, float baseCenterY) {
        this.baseCenterX = baseCenterX;
        this.baseCenterY = baseCenterY;
        this.stickX = baseCenterX;
        this.stickY = baseCenterY;
    }

    public void reset() {
        // Reset the stick to the base position when touch is released
        stickX = baseCenterX;
        stickY = baseCenterY;
        this.touchID = -1;
    }

    public void onTouch(float touchX, float touchY) {
        // Calculate joystick stick position relative to the base
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Constrain the stick movement within the joystick's base
        if (distance > JOYSTICK_RADIUS) {
            dx = dx / distance * JOYSTICK_RADIUS;
            dy = dy / distance * JOYSTICK_RADIUS;
        }

        stickX = baseCenterX + dx;
        stickY = baseCenterY + dy;
    }

    public void setPointerID(int pointerId) {
        this.touchID = pointerId;
    }

    public int getTouchID() {
        return this.touchID;
    }

    public boolean isTouchedBy(int pointerId) {
        return this.touchID == pointerId;
    }

    public float getDirection() {
        float dx = stickX - baseCenterX;
        float dy = stickY - baseCenterY;
        return (float) Math.atan2(dy, dx);  // Return direction in radians
    }

    public void draw(Canvas canvas) {
        // Draw the base of the joystick (circle)
        Paint paint = new Paint();
        paint.setColor(Color.GRAY);  // Joystick base color
        paint.setAlpha(100);  // Slight transparency for the base
        canvas.drawCircle(baseCenterX, baseCenterY, JOYSTICK_RADIUS, paint);

        // Draw the stick of the joystick (smaller circle)
        paint.setColor(Color.WHITE);  // Joystick stick color
        canvas.drawCircle(stickX, stickY, 30, paint);  // Stick size is smaller
    }

    public boolean isControlledBy(float touchX, float touchY) {
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance <= JOYSTICK_RADIUS; // Check if touch is inside the joystick area
    }
}