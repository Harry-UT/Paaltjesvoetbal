package com.example.paaltjesvoetbal;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Random;

/**
 * Ball for splash animation upon scoring a goal.
 */
public class SplashBall {
    private int x;
    private int y;
    private int xSpeed;
    private int ySpeed;
    private final int ballSize = 20;
    private int color;
    private boolean started = false;

    public SplashBall() {
        Random random = new Random();
        xSpeed = random.nextInt(10) - 5;
        if (xSpeed == 0) {
            xSpeed = 1;
        }
        if (xSpeed > 3) {
            xSpeed = 3;
        }
        ySpeed = random.nextInt(10) - 5;
        if (ySpeed == 0) {
            ySpeed = 1;
        }
        if (ySpeed > 3) {
            ySpeed = 3;
        }
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
        x += xSpeed;
        y += ySpeed;
        draw(canvas);
    }

    public void bounce(int screenWidth, int screenHeight) {
        if (x < 0 || x > screenWidth) {
            xSpeed = -xSpeed;
        }
        if (y < 0 || y > screenHeight) {
            ySpeed = -ySpeed;
        }
    }

    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawCircle(x, y, ballSize, paint);
    }

    public void reset() {
        started = false;
    }
}