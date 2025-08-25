package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Joystick {
    private final float baseCenterX;
    private final float baseCenterY;
    private float stickX, stickY;
    private int touchID = -1;
    private final float radius;
    private final Paint outerPaint = new Paint();
    private final Paint innerPaint = new Paint();

    // Constructor to initialize the joystick position and player reference
    public Joystick(float baseCenterX, float baseCenterY, float radius) {
        this.baseCenterX = baseCenterX;
        this.baseCenterY = baseCenterY;
        this.stickX = baseCenterX;
        this.stickY = baseCenterY;
        this.radius = radius;
        this.outerPaint.setColor(Color.GRAY);  // Joystick base color
        this.outerPaint.setAlpha(100);  // Slight transparency for the base
        innerPaint.setAntiAlias(true);
        innerPaint.setColor(Color.WHITE);  // Joystick stick color
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
        if (distance > radius) {
            dx = dx / distance * radius;
            dy = dy / distance * radius;
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
        // Draw the base of the joystick (gray donut)
        canvas.drawCircle(baseCenterX, baseCenterY, radius, outerPaint);

        // Draw the stick of the joystick (smaller coloured circle)
        canvas.drawCircle(stickX, stickY, 30, innerPaint);
    }

    public boolean isTouched(float touchX, float touchY) {
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance <= radius * 1.5; // Check if touch is inside the joystick area
    }
}