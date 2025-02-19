package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

/**
 * Ball for splash animation upon scoring a goal.
 */
public class Star {
    private int x;
    private int y;
    private int velocity;
    private int dx;
    private int dy;
    private final int size = 20;
    private int color;
    private boolean started = false;

    public Star() {
        Random random = new Random();
        velocity = random.nextInt(5) + 1; // Ensuring a positive velocity
        dx = random.nextInt(11) - 5; // Range from -5 to 5 inclusive
        dy = random.nextInt(11) - 5; // Range from -5 to 5 inclusive
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void update(Canvas canvas, int ballX, int ballY) {
        if (!started) {
            x = ballX;
            y = ballY;
            started = true;
        }
        x += dx * velocity;
        y += dy * velocity;
        bounce(canvas.getWidth(), canvas.getHeight());
        draw(canvas);
    }

    public void bounce(int screenWidth, int screenHeight) {
        if (x < 0) {
            if (dx < 0) {
                dx = -dx;
            }
        }
        if (x > screenWidth) {
            if (dx > 0) {
                dx = -dx;
            }
        }

        if (y < 0) {
            if (dy < 0) {
                dy = -dy;
            }
        }
        if (y > screenHeight) {
            if (dy > 0) {
                dy = -dy;
            }
        }
    }

    /**
     * Draw a star shape.
     */
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);

        Path path = new Path();
        double angle = Math.PI / 2; // Start from the top

        for (int i = 0; i < 5; i++) {
            float x = (float) (this.x + Math.cos(angle) * size);
            float y = (float) (this.y - Math.sin(angle) * size);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
            angle += Math.PI * 2 / 5 * 2; // Skip one point for a star shape
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    public void reset() {
        started = false;
    }
}
