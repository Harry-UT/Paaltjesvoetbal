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
    private final int velocity;
    private int dx;
    private int dy;
    private static final int SIZE = 20;
    private int rotation = 0;
    private static final int rotationSpeed = 8;
    private boolean started = false;
    private final Paint paint = new Paint();

    public Star() {
        Random random = new Random();
        velocity = random.nextInt(2) + 1; // Ensuring a positive velocity
        dx = random.nextInt(21) - 10; // Range from -10 to 10 inclusive
        dy = random.nextInt(21) - 10; // Range from -10 to 10 inclusive
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    public void rotate() {
        rotation += rotationSpeed;
        if (rotation >= 360) {
            rotation = 0;
        }
    }

    public void update(Canvas canvas, int ballX, int ballY, boolean fade) {
        if (!started && !fade) {
            x = ballX;
            y = ballY;
            started = true;
        }
        x += dx * velocity;
        y += dy * velocity;
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
        if (x <= 0) {
            if (dx < 0) {
                dx = -dx;
            }
        }
        if (x >= screenWidth) {
            if (dx > 0) {
                dx = -dx;
            }
        }
        if (y <= 0) {
            if (dy < 0) {
                dy = -dy;
            }
        }
        if (y >= screenHeight) {
            if (dy > 0) {
                dy = -dy;
            }
        }
    }

    /**
     * Draw a star shape.
     */
    private void draw(Canvas canvas) {
        Path path = new Path();
        double angle = Math.PI / 2 + Math.toRadians(rotation); // Start from the top with rotation
        rotate();
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