package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class ShootButton {
    private final Paint paint;
    private Ball ball;
    private float x;
    private float y;
    private final float radius;
    private boolean isPressed; // Track press state

    public ShootButton(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        isPressed = false;
        paint = new Paint();
    }

    public void draw(Canvas canvas) {
        // Change color based on the button's pressed state
        paint.setColor(isPressed ? Color.DKGRAY : Color.LTGRAY);
        canvas.drawCircle(x, y, radius, paint);

        // Draw text in the center of the button
        paint.setColor(Color.BLACK);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        float textX = x;
        float textY = y - ((paint.descent() + paint.ascent()) / 2);
        canvas.drawText("SHOOT", textX, textY, paint);
    }

    public boolean isTouched(float touchX, float touchY) {
        float distance = (float) Math.sqrt(Math.pow(touchX - x, 2) + Math.pow(touchY - y, 2));
        return distance <= radius;
    }

    public void shoot() {
        if (ball != null) {
            ball.shoot();  // Laat de bal schieten
        }
    }
    public void setBall(Ball ball) {
        this.ball = ball;
    }
    public Ball getBall() {
        return ball;
    }

    public void setPressed(boolean pressed) {
        isPressed = pressed;
    }
}