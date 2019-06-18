package com.alienonwork.compass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;

public class CompassActivity extends AppCompatActivity {

    private CompassView mCompassView;
    private SensorManager mSensorManager;
    private int mScreenRotation;
    private float[] mNewestValues;

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mNewestValues = calculateOrientation(event.values);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        mCompassView = findViewById(R.id.compassView);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        Display display = wm.getDefaultDisplay();
        mScreenRotation = display.getRotation();

        mNewestValues = new float[] {0, 0, 0};

        Timer updateTimer = new Timer("compassUpdate");
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateGUI();
            }
        }, 0, 1000/60);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor rotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorManager.registerListener(mSensorEventListener, rotationVector, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    private void updateOrientation(float[] values) {
        if (mCompassView != null) {
            mCompassView.setBearing(values[0]);
            mCompassView.setPitch(values[1]);
            mCompassView.setRoll(-values[2]);
            mCompassView.invalidate();
        }
    }

    private float[] calculateOrientation(float[] values) {
        float[] rotationMatrix = new float[9];
        float[] remappedMatrix = new float[9];
        float[] orientation = new float[3];

        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);

        int x_axis = SensorManager.AXIS_X;
        int y_axis = SensorManager.AXIS_Y;

        switch (mScreenRotation) {
            case (Surface.ROTATION_90):
                x_axis = SensorManager.AXIS_Y;
                y_axis = SensorManager.AXIS_MINUS_X;
                break;
            case (Surface.ROTATION_180):
                y_axis = SensorManager.AXIS_MINUS_Y;
                break;
            case (Surface.ROTATION_270):
                x_axis = SensorManager.AXIS_MINUS_Y;
                y_axis = SensorManager.AXIS_X;
                break;
            default: break;
        }

        SensorManager.remapCoordinateSystem(rotationMatrix, x_axis, y_axis, remappedMatrix);

        SensorManager.getOrientation(remappedMatrix, orientation);

        values[0] = (float) Math.toDegrees(orientation[0]);
        values[1] = (float) Math.toDegrees(orientation[1]);
        values[2] = (float) Math.toDegrees(orientation[2]);

        return values;
    }

    private void updateGUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateOrientation(mNewestValues);
            }
        });
    }
}
