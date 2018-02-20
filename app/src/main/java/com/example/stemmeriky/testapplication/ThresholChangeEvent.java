package com.example.stemmeriky.testapplication;

public class ThresholChangeEvent {
    private final int threshold;

    public ThresholChangeEvent(int threshold) {
        this.threshold = threshold;
    }

    public int getMessage() {
        return threshold;
    }
}