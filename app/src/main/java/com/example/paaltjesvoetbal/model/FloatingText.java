package com.example.paaltjesvoetbal.model;

public class FloatingText {
    int x;
    int y;
    int size;
    int rotation;

    public FloatingText(int x, int y, int size, int rotation) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.rotation = rotation;
    }

    public void increment(int sizeIncrement, int xIncrement, int yIncrement) {
        this.size += sizeIncrement;
        this.x += xIncrement;
        this.y += yIncrement;
    }

    public int getSize() {
        return size;
    }

    public void reset(int x, int y) {
        size = 0;
        this.x = x;
        this.y = y;
    }

    public int getRotation() {
        return rotation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}