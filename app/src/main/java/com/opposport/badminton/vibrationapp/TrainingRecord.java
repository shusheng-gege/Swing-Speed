package com.opposport.badminton.vibrationapp;

public class TrainingRecord {
    private long id;
    private long timestamp;
    private int count;
    private float avgSpeed;
    private float maxSpeed;

    public TrainingRecord(long id, long timestamp, int count, float avgSpeed, float maxSpeed) {
        this.id = id;
        this.timestamp = timestamp;
        this.count = count;
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCount() {
        return count;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }
}