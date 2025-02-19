package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import java.util.Random;

/**
 * Ball for splash animation upon scoring a goal.
 */
public class Star {
    private int x, y;
    private int velocity, dx, dy;
    private static final int SIZE = 20;
    private int color;
    private boolean started = false;
    private final Paint paint = new Paint();

    public Star() {
        Random random = new Random();
        velocity = random.nextInt(2) + 1; // Ensuring a positive velocity
        dx = random.nextInt(21) - 10; // Range from -10 to 10 inclusive
        dy = random.nextInt(21) - 10; // Range from -10 to 10 inclusive
    }

    public static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public void setColor(int color) {
        this.color = color;
        paint.setColor(color);
    }

    public void update(Canvas canvas, int ballX, int ballY, boolean fade) {
        if (!started && !fade) {
            x = ballX;
            y = ballY;
            started = true;
        }
        // Map final velocity to range 0-10
        x += map(dx * velocity, -10, 10, 0, 10);
//        x += dx * velocity;
//        y += dy * velocity;
        y += map(dy * velocity, -10, 10, 0, 10);
        if (fade) {
            // Check if alpha of color is not yet fully opacity
            if (paint.getAlpha() == 0) {
                return;
            }
            int alpha = paint.getAlpha() - 9; // Gradually decrease opacity
            paint.setAlpha(Math.max(alpha, 0)); // Prevent negative alpha
            Log.d("Star", "Alpha: " + paint.getAlpha());
            draw(canvas);
            return;
        }
        draw(canvas);
    }

    public void bounce(int screenWidth, int screenHeight) {
        if (x <= 0 || x >= screenWidth) dx = -dx;
        if (y <= 0 || y >= screenHeight) dy = -dy;

        // Keep within bounds
        x = Math.max(0, Math.min(x, screenWidth));
        y = Math.max(0, Math.min(y, screenHeight));
    }

    /**
     * Draw a star shape.
     */
    private void draw(Canvas canvas) {
        Path path = new Path();
        double angle = Math.PI / 2; // Start from the top

        for (int i = 0; i < 5; i++) {
            float px = (float) (x + Math.cos(angle) * SIZE);
            float py = (float) (y - Math.sin(angle) * SIZE);
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
            angle += Math.PI * 2 / 5 * 2; // Skip one point for a star shape
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    public void resetStartTime() {
        started = false;
    }
}