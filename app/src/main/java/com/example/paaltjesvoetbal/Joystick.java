package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

public class Joystick {
    private float baseCenterX, baseCenterY;
    private float stickX, stickY;
    private Player player;  // Reference to the player controlled by this joystick
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
    }

    // Method to handle touch events and update joystick position
    public void onTouchEvent(float touchX, float touchY) {
        // Calculate the stick's position relative to the joystick base
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

    // Getter for the stick position
    public PointF getStickPosition() {
        return new PointF(stickX, stickY);
    }

    // Getter for the joystick's base center
    public PointF getBaseCenter() {
        return new PointF(baseCenterX, baseCenterY);
    }

    public float getDirection() {
        float dx = stickX - baseCenterX;
        float dy = stickY - baseCenterY;
        return (float) Math.atan2(dy, dx);  // Angle in radians
    }

    // Set the player this joystick controls
    public void setPlayer(Player player) {
        this.player = player;
    }

    // Add the draw method to render the joystick
    public void draw(Canvas canvas, Paint paint) {
        // Draw the base of the joystick (circle)
        paint.setColor(Color.GRAY);  // Joystick base color
        paint.setAlpha(100);  // Slight transparency for the base
        canvas.drawCircle(baseCenterX, baseCenterY, JOYSTICK_RADIUS, paint);

        // Draw the stick of the joystick (smaller circle)
        paint.setColor(Color.WHITE);  // Joystick stick color
        canvas.drawCircle(stickX, stickY, 30, paint);  // Stick size is smaller
    }
}