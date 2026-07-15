package com.opposport.badminton.vibrationapp;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private PowerManager.WakeLock wakeLock;

    private TextView speedTextView;
    private TextView averageSpeedTextView;
    private TextView countTextView;

    private VibratingCountDetector detector;
    private DatabaseHelper dbHelper;
    private long currentRecordId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // keep screen on while training
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // init database
        dbHelper = new DatabaseHelper(this);

        // init sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // init UI views
        speedTextView = findViewById(R.id.speedTextView);
        averageSpeedTextView = findViewById(R.id.averageSpeedTextView);
        countTextView = findViewById(R.id.countTextView);

        // init swing detector with vibrator
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        detector = new VibratingCountDetector(vibrator);

        // reset button
        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentTraining();
                detector.reset();
                speedTextView.setText(R.string.speed_zero);
                averageSpeedTextView.setText(R.string.avg_zero);
                countTextView.setText(R.string.count_zero);
                currentRecordId = -1;
                Toast.makeText(MainActivity.this, R.string.toast_reset, Toast.LENGTH_SHORT).show();
            }
        });

        // history button
        Button historyButton = findViewById(R.id.historyButton);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentTraining();
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // register sensor listeners
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(this, R.string.toast_no_accelerometer, Toast.LENGTH_LONG).show();
        }
    }

    private void saveCurrentTraining() {
        if (detector.getCount() > 0 && currentRecordId < 0) {
            currentRecordId = dbHelper.insertTraining(
                detector.getCount(),
                detector.getAverageSpeed()
            );
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;
            detector.onAccelerometerChanged(values[0], values[1], values[2]);

            float speed = detector.getCurrentSpeed();
            float avg = detector.getAverageSpeed();
            int count = detector.getCount();

            speedTextView.setText(String.format(getString(R.string.speed_format), speed));
            averageSpeedTextView.setText(String.format(getString(R.string.speed_format), avg));
            countTextView.setText(String.valueOf(count));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // training scenario does not need accuracy callbacks
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}