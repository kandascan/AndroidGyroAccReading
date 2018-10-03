package com.example.bogus.myapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private TextView accelerometerXVal, accelerometerYVal, accelerometerZVal, sensorName, sensorName2, gyroscopeXVal, gyroscopeYVal, gyroscopeZVal;
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometerXVal = (TextView) findViewById(R.id.accelerometerXValue);
        accelerometerYVal = (TextView) findViewById(R.id.accelerometerYValue);
        accelerometerZVal = (TextView) findViewById(R.id.accelerometerZValue);
        gyroscopeXVal = (TextView) findViewById(R.id.gyroXValue);
        gyroscopeYVal = (TextView) findViewById(R.id.gyroYValue);
        gyroscopeZVal = (TextView) findViewById(R.id.gyroZValue);
        sensorName = (TextView) findViewById(R.id.sensorName);
        sensorName2 = (TextView) findViewById(R.id.sensorName2);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        if(accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.e("onCreate", "No Accelerometer found!");
            finish();
        }

        if(gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.e("onCreate", "No Gyroscope found!");
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if(mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            processAccelerometer(event);
            sensorName.setText(event.sensor.getName());
        }
        else if(mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            processGyroscope(event);
            sensorName2.setText(event.sensor.getName());
        }
        /*
        sensorName.setText("Sensor: " + event.sensor.getName());
        accelerometerXVal.setText("x: " + event.values[0]);
        accelerometerYVal.setText("y: " + event.values[1]);
        accelerometerZVal.setText("z: " + event.values[2]);
        */
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    void processGyroscope(SensorEvent event) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > 0.01) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            /*
            gyroscopeXVal.setText(String.format("%8.5f\n", axisX));
            gyroscopeYVal.append(String.format("%8.5f\n", axisY));
            gyroscopeZVal.append(String.format("%8.5f\n", axisZ));
            */
            gyroscopeXVal.setText(String.format("%8.5f\n", axisX));
            gyroscopeXVal.append(String.format("%8.5f\n", axisY));
            gyroscopeXVal.append(String.format("%8.5f\n", axisZ));
            gyroscopeYVal.setText("Original X: "+event.values[0] +"\n");
            gyroscopeYVal.append("Original Y: "+event.values[1]+"\n");
            gyroscopeYVal.append("Original Z: "+event.values[2]);


            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
        }
        timestamp = event.timestamp;
        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
    }

    void processAccelerometer(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        final float alpha = 0.8f;

        float[] gravity = {0, 0, 0};
        float[] linear_acceleration = {0, 0, 0};
        String[] acceleration = {"", "", ""};

        for(int i = 0; i < 3; i++) {
            // Isolate the force of gravity with the low-pass filter.
            gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[i] = event.values[i] - gravity[i];

            acceleration[i] = String.format("%8.5f\n", linear_acceleration[i]);
        }

        //mAccelTextView.setText(acceleration[0] + acceleration[1] + acceleration[2]);
        accelerometerXVal.setText("x: " + acceleration[0] + "Original X: " + event.values[0]);
        accelerometerYVal.setText("y: " + acceleration[1] + "Original Y: " + event.values[1]);
        accelerometerZVal.setText("z: " + acceleration[2] + "Original Z: " + event.values[2]);
    }
}
