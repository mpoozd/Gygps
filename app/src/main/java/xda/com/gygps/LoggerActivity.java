package xda.com.gygps;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Handler;


public class LoggerActivity extends Activity implements SensorEventListener {
    // Constants for the low-pass filters
    private float timeConstant = 0.18f;
    private float alpha = 0.9f;
    private float dt = 0;

    // Timestamps for the low-pass filters
    private float timestamp1 = System.nanoTime();
    private float timestampOld = System.nanoTime();

    // Gravity and linear accelerations components for the
// Wikipedia low-pass filter
    private float[] gravity = new float[]
            { 0, 0, 0 };

    private float[] linearAcceleration = new float[]
            { 0, 0, 0 };

    // Raw accelerometer data
    private float[] input = new float[]
            { 0, 0, 0 };

    private int count = 0;
/////
    private static final String TAG = "LocationActivity";
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;
    private DynamicLinePlot dynamicPlot;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    TextView x1,y1,z1,xchg,ychg,zchg;
    Button startBut,stopBut;
    SensorManager mSensorManager;
    // Plot keys for the acceleration plot
    private final static int PLOT_ACCEL_X_AXIS_KEY = 0;
    private final static int PLOT_ACCEL_Y_AXIS_KEY = 1;
    private final static int PLOT_ACCEL_Z_AXIS_KEY = 2;
    Runnable runable;
    Handler handler;
    WindowManager mWindowManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);
        Button button = (Button)findViewById(R.id.button11);
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
       initPlots();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });



    }
    public LoggerActivity() {
    }

    public void start() {

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME );
    }
    public void stop() {
        mSensorManager.unregisterListener(this);
    }


    public void onSensorChanged(SensorEvent event) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            if (omegaMagnitude > 0.000000000008854187817) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
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
initPlots();
    }

    private void initPlots()
    {
        // Create the graph plot
        XYPlot plot = (XYPlot) findViewById(R.id.plot_sensor);

        plot.setTitle("Acceleration");
        dynamicPlot = new DynamicLinePlot(plot, this);
        dynamicPlot.setMaxRange(20);
        dynamicPlot.setMinRange(-20);

    }
    private void plotData()
    {

            dynamicPlot.setData(linearAcceleration[0], PLOT_ACCEL_X_AXIS_KEY);
            dynamicPlot.setData(linearAcceleration[1], PLOT_ACCEL_Y_AXIS_KEY);
            dynamicPlot.setData(linearAcceleration[2], PLOT_ACCEL_Z_AXIS_KEY);


        dynamicPlot.draw();
    }



    /**
     * Add a sample.
     *
     * @param acceleration
     *            The acceleration data.
     * @return Returns the output of the filter.
     */
    public float[] addSamples(float[] acceleration)
    {
// Get a local copy of the sensor values
        System.arraycopy(acceleration, 0, this.input, 0, acceleration.length);

        timestamp1 = System.nanoTime();

// Find the sample period (between updates).
// Convert from nanoseconds to seconds
        dt = 1 / (count / ((timestamp1 - timestampOld) / 1000000000.0f));

        count++;

        alpha = timeConstant / (timeConstant + dt);

        gravity[0] = alpha * gravity[0] + (1 - alpha) * input[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * input[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * input[2];

        linearAcceleration[0] = input[0] - gravity[0];
        linearAcceleration[1] = input[1] - gravity[1];
        linearAcceleration[2] = input[2] - gravity[2];

        return linearAcceleration;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
