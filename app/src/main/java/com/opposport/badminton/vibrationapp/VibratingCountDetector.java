package com.opposport.badminton.vibrationapp;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibratingCountDetector {
    // --- physical constants -------------------------------------------------
    private static final float GRAVITY = 9.81f;

    // --- swing-detection thresholds ----------------------------------------
    // A real badminton swing produces a sharp acceleration spike well above
    // noise floor. 30 m/s^2 (~3g) ignores daily motion while capturing
    // smashes and clears. Tune DOWN if you want to count gentle net shots.
    private static final float ACCELERATION_THRESHOLD = 30.0f;

    // The "arm-braking" phase after a smash can dip below the main threshold
    // and bounce back up quickly. Require the signal to stay below
    // END_RATIO * threshold for at least END_HYSTERESIS_MS before we commit
    // to ending the swing, so a single swing with a multi-peak profile is
    // not split into two.
    private static final float END_RATIO = 0.25f;
    private static final long END_HYSTERESIS_MS = 60;

    // A swing must be at least this long (acceleration build-up + release).
    // Below this it is just a jolt / wrist tap, not a real swing.
    private static final long MIN_SWING_DURATION = 150;

    // Above this the "swing" is likely two swings stuck together or sensor
    // drift; cap the duration rather than trusting the numbers.
    private static final long MAX_SWING_DURATION = 3000;

    // Consecutive samples above threshold required before we accept that a
    // swing has truly started. This suppresses single-sample noise spikes.
    private static final int MIN_START_SAMPLES = 3;

    // Minimum number of samples we must capture during the active phase to
    // trust the peak-acceleration reading.
    private static final int MIN_SWING_SAMPLES = 3;

    // Two real distinct swings are rarely closer than this on a badminton
    // court. (Even a fast drive rally is ~400-500ms between shots.)
    private static final long MIN_SWING_GAP = 400;

    // --- speed estimate bounds --------------------------------------------
    private static final float MIN_SPEED = 5.0f;
    private static final float MAX_SPEED = 350.0f;

    // Racket-head path radius estimate in metres. A badminton racket is ~0.65m
    // long, and the effective radius during a swing (wrist + forearm + some
    // upper-arm contribution) is closer to 0.5m than the 0.35m used before,
    // which was producing undervalued speeds on smashes.
    private static final float SWING_RADIUS = 0.5f;

    // --- vibration ----------------------------------------------------------
    private static final int VIBRATION_DURATION = 30;
    private static final int VIBRATION_AMPLITUDE = 50;

    // --- low-pass filter ----------------------------------------------------
    // Small moving-average on the net acceleration. Smooths the sensor signal
    // so that a single noisy sample cannot start or end a swing on its own.
    private static final int FILTER_SIZE = 5;
    private final float[] accFilter = new float[FILTER_SIZE];
    private int filterIndex = 0;

    // --- swing-phase state machine -----------------------------------------
    // IDLE  -> acceleration has been low. Waiting for a sustained spike.
    // ACTIVE-> swing committed; we track peak acceleration and sample count
    //         until the signal stays low long enough to end the swing.
    private enum Phase { IDLE, ACTIVE }
    private Phase phase = Phase.IDLE;

    private long swingStartTime = 0;
    private long firstAboveThresholdTime = 0;
    private long firstBelowEndTime = 0;
    private int aboveThresholdRun = 0;
    private float peakAcceleration = 0.0f;
    private int swingSamples = 0;

    // Debounce: a swing will not be accepted until this time has elapsed since
    // the last committed swing.
    private long lastSwingTime = 0;

    // --- accumulators for current session ----------------------------------
    private float currentSpeed = 0.0f;
    private float averageSpeed = 0.0f;
    private float totalSpeed = 0.0f;
    private int count = 0;

    private final Vibrator vibrator;

    public VibratingCountDetector(Vibrator vibrator) {
        for (int i = 0; i < accFilter.length; i++) {
            accFilter[i] = GRAVITY;
        }
        this.vibrator = vibrator;
    }

    public void onAccelerometerChanged(float x, float y, float z) {
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
        float netAcceleration = Math.abs(magnitude - GRAVITY);
        float filteredAcc = applyLowPassFilter(netAcceleration);
        long now = System.currentTimeMillis();

        switch (phase) {
            case IDLE:
                if (filteredAcc > ACCELERATION_THRESHOLD) {
                    aboveThresholdRun++;
                    if (aboveThresholdRun == 1) {
                        firstAboveThresholdTime = now;
                    }
                    if (aboveThresholdRun >= MIN_START_SAMPLES) {
                        phase = Phase.ACTIVE;
                        swingStartTime = firstAboveThresholdTime;
                        peakAcceleration = filteredAcc;
                        swingSamples = aboveThresholdRun;
                        firstBelowEndTime = 0;
                    }
                } else {
                    aboveThresholdRun = 0;
                }
                break;

            case ACTIVE:
                swingSamples++;
                if (filteredAcc > peakAcceleration) {
                    peakAcceleration = filteredAcc;
                }
                if (filteredAcc < ACCELERATION_THRESHOLD * END_RATIO) {
                    if (firstBelowEndTime == 0) {
                        firstBelowEndTime = now;
                    }
                    if (now - firstBelowEndTime >= END_HYSTERESIS_MS) {
                        endSwing(now);
                    }
                } else {
                    firstBelowEndTime = 0;
                }
                break;
        }
    }

    /**
     * Called once per swing candidate at the moment the active phase ends.
     * Validates the candidate (duration, minimum samples, debounce gap) and,
     * if it passes, commits it as a real swing: updates counters, average
     * speed and fires haptic feedback.
     */
    private void endSwing(long now) {
        phase = Phase.IDLE;
        aboveThresholdRun = 0;
        firstBelowEndTime = 0;

        long duration = now - swingStartTime;
        if (duration < MIN_SWING_DURATION || duration > MAX_SWING_DURATION) {
            return;
        }
        if (swingSamples < MIN_SWING_SAMPLES) {
            return;
        }
        if (now - lastSwingTime < MIN_SWING_GAP) {
            return;
        }

        // Translational speed estimate derived from centripetal relationship:
        // v ~= a_peak * r / t  -> convert m/s to km/h by * 3.6.
        float speedMs = peakAcceleration * SWING_RADIUS / (duration / 1000.0f) * 3.6f;
        currentSpeed = Math.min(Math.max(speedMs, MIN_SPEED), MAX_SPEED);

        if (currentSpeed >= MIN_SPEED) {
            count++;
            totalSpeed += currentSpeed;
            averageSpeed = totalSpeed / count;
            lastSwingTime = now;
            vibrateOnce();
        }
    }

    private void vibrateOnce() {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect effect = VibrationEffect.createOneShot(
                        VIBRATION_DURATION,
                        VIBRATION_AMPLITUDE
                    );
                    vibrator.vibrate(effect);
                } else {
                    vibrator.vibrate(VIBRATION_DURATION);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private float applyLowPassFilter(float input) {
        accFilter[filterIndex] = input;
        filterIndex = (filterIndex + 1) % FILTER_SIZE;

        float sum = 0;
        for (float val : accFilter) {
            sum += val;
        }
        return sum / FILTER_SIZE;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }

    public int getCount() {
        return count;
    }

    public void reset() {
        count = 0;
        totalSpeed = 0.0f;
        averageSpeed = 0.0f;
        currentSpeed = 0.0f;
        peakAcceleration = 0.0f;
        phase = Phase.IDLE;
        aboveThresholdRun = 0;
        firstBelowEndTime = 0;
        lastSwingTime = 0;
        swingSamples = 0;
        for (int i = 0; i < accFilter.length; i++) {
            accFilter[i] = GRAVITY;
        }
        filterIndex = 0;
    }
}
