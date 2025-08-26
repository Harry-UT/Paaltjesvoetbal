package com.example.paaltjesvoetbal.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Joystick {
    private final float baseCenterX;
    private final float baseCenterY;
    private float stickX, stickY;
    private int touchID = -1;
    private final float outerRadius;
    private final float innerRadius;
    private final Paint innerPaint = new Paint();
    private final Paint outerPaint = new Paint();

    // Constructor to initialize the joystick position and player reference
    public Joystick(float baseCenterX, float baseCenterY, float innerRadius, float outerRadius) {
        this.baseCenterX = baseCenterX;
        this.baseCenterY = baseCenterY;
        this.stickX = baseCenterX;
        this.stickY = baseCenterY;
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.outerPaint.setColor(Color.GRAY);  // Joystick base color
        this.outerPaint.setAlpha(100);  // Slight transparency for the base
        innerPaint.setAntiAlias(true);
        innerPaint.setColor(Color.WHITE);  // Joystick stick color
    }

    /** Reset the joystick to its base position
     */
    public void reset() {
        // Reset the stick to the base position when touch is released
        stickX = baseCenterX;
        stickY = baseCenterY;
        this.touchID = -1;
    }

    /** Update the joystick position based on touch input
     * @param touchX X coordinate of the touch
     * @param touchY Y coordinate of the touch
     */
    public void onTouch(float touchX, float touchY) {
        // Calculate joystick stick position relative to the base
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Constrain the stick movement within the joystick's base
        if (distance > outerRadius) {
            dx = dx / distance * outerRadius;
            dy = dy / distance * outerRadius;
        }

        stickX = baseCenterX + dx;
        stickY = baseCenterY + dy;
    }

    /** Set the pointer ID for the current touch
     * @param pointerId ID of the touch pointer
     */
    public void setPointerID(int pointerId) {
        this.touchID = pointerId;
    }

    /** Get the pointer ID for the current touch
     * @return ID of the touch pointer
     */
    public int getTouchID() {
        return this.touchID;
    }

    /** Check if the joystick is being pressed atm by a specific pointer ID
     * @param pointerId ID of the touch pointer to check
     * @return true if the joystick is being touched by the specified pointer ID, false otherwise
     */
    public boolean isPressedBy(int pointerId) {
        return this.touchID == pointerId;
    }

    public float getDirection() {
        float dx = stickX - baseCenterX;
        float dy = stickY - baseCenterY;
        return (float) Math.atan2(dy, dx);  // Return direction in radians
    }

    /** Draw the joystick on the canvas
     * @param canvas the canvas to draw on
     */
    public void draw(Canvas canvas) {
        // Draw the base of the joystick (gray donut)
        canvas.drawCircle(baseCenterX, baseCenterY, outerRadius, outerPaint);
        // Draw the stick of the joystick (smaller coloured circle)
        canvas.drawCircle(stickX, stickY, innerRadius, innerPaint);
    }

    /** Check if a touch is within the joystick area
     * @param touchX X coordinate of the touch
     * @param touchY Y coordinate of the touch
     * @return true if the touch is within the joystick area, false otherwise
     */
    public boolean isTouched(float touchX, float touchY) {
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance <= outerRadius * 1.5; // Check if touch is inside the joystick area
    }
}