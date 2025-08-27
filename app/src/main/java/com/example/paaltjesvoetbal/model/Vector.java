package com.example.paaltjesvoetbal.model;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Vector {
    private double x1, y1, x2, y2; // Start and end points of the line
    private final Paint paint = new Paint();

    public Vector(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
    }

    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    public Vector getCenteredAndScaledVector(double scaleFactor) {
        // Calculate the midpoint of the original vector
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        // Calculate the direction vector (difference in x and y)
        double dx = x2 - x1;
        double dy = y2 - y1;

        // Calculate the scaled distance for each half
        double scaledDX = dx * scaleFactor / 2;
        double scaledDY = dy * scaleFactor / 2;

        // New start point is the midpoint minus half the scaled distance
        double newX1 = midX - scaledDX;
        double newY1 = midY - scaledDY;

        // New end point is the midpoint plus half the scaled distance
        double newX2 = midX + scaledDX;
        double newY2 = midY + scaledDY;

        return new Vector(newX1, newY1, newX2, newY2);
    }

    public Vector[] split(double scaleFactor) {
        Vector vectorToCutWith = this.getCenteredAndScaledVector(scaleFactor);
        Vector vector1 = new Vector(this.getX1(), this.getY1(), vectorToCutWith.getX1(), vectorToCutWith.getY1());
        Vector vector2 = new Vector(vectorToCutWith.getX2(), vectorToCutWith.getY2(), this.getX2(), this.getY2());
        return new Vector[]{vector1, vector2};
    }

    public void draw(Canvas canvas) {
        // Draw the line on the canvas
        Paint paint = new Paint();
        paint.setColor(Color.MAGENTA);
        paint.setStrokeWidth(5);
        canvas.drawLine((float) x1, (float) y1, (float) x2, (float) y2, paint);
    }

    // Get distance from point (x, y) to the line segment
    public double distanceToPoint(double x, double y) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            // Edge is a single point
            return Math.hypot(x - x1, y - y1);
        }

        // Projection factor t of point onto line
        double t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;

        // Clamp t to [0, 1] to stay within segment
        t = Math.max(0, Math.min(1, t));

        // Closest point on the segment
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(x - projX, y - projY);
    }

    public float getMidX() {
        return (float) ((x1 + x2) / 2);
    }

    /** Calculate the length of the vector
     * @return Length of the vector
     */
    public float getMidY() {
        return (float) ((y1 + y2) / 2);
    }
}