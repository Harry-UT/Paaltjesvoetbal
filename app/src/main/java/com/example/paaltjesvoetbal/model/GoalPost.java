package com.example.paaltjesvoetbal.model;

public class GoalPost {
    private float x, y;      // center of the post
    private float radius;    // post radius

    public GoalPost(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}