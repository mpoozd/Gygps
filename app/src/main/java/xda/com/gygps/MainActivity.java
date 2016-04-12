package xda.com.gygps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements SensorEventListener, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    TextView x1,y1,z1,xchg,ychg,zchg;
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    private float[] mAccelGravityData = new float[3];
    private float[] mGeomagneticData = new float[3];
    private float[] mRotationMatrix = new float[16];
    private float[] bufferedAccelGData = new float[3];
    private float[] bufferedMagnetData = new float[3];
    Button startBut,stopBut,butto;

    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        x1 = (TextView)findViewById(R.id.x);
        y1 = (TextView)findViewById(R.id.y);
        z1 = (TextView)findViewById(R.id.z);
        xchg = (TextView)findViewById(R.id.xvchange);
        ychg = (TextView)findViewById(R.id.yvchage);
        zchg = (TextView)findViewById(R.id.zvchange);
        butto = (Button)findViewById(R.id.buttonto);
        startBut = (Button)findViewById(R.id.start);
        stopBut = (Button)findViewById(R.id.stop);
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        startBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
                updateUI();


            }
        });
        stopBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xchg.setText("X value");
                ychg.setText("Y value");
                zchg.setText("Z value");
                stop();
                stopLocationUpdates();

            }
        });
        butto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication() ,Test.class);
                startActivity(intent);
            }
        });
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


    }



    public MainActivity() {
    }

    public void start() {

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME );
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    private void loadNewSensorData(SensorEvent event) {
        final int type = event.sensor.getType();
        if (type == Sensor.TYPE_GYROSCOPE) {
            //Smoothing the sensor data a bit
            mAccelGravityData[0]=(mAccelGravityData[0]*2+event.values[0])*0.33334f;
            mAccelGravityData[1]=(mAccelGravityData[1]*2+event.values[1])*0.33334f;
            mAccelGravityData[2]=(mAccelGravityData[2]*2+event.values[2])*0.33334f;
        }
        if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            //Smoothing the sensor data a bit
            mGeomagneticData[0]=(mGeomagneticData[0]*1+event.values[0])*0.5f;
            mGeomagneticData[1]=(mGeomagneticData[1]*1+event.values[1])*0.5f;
            mGeomagneticData[2]=(mGeomagneticData[2]*1+event.values[2])*0.5f;

            float x = mGeomagneticData[0];
            float y = mGeomagneticData[1];
            float z = mGeomagneticData[2];
            double field = Math.sqrt(x*x+y*y+z*z);
            if (field>25 && field<65){
                Log.e(TAG, "loadNewSensorData : wrong magnetic data, need a recalibration field = " + field);
            }
        }


    }


    private void rootMeanSquareBuffer(float[] target, float[] values) {

        final float amplification = 200.0f;
        float buffer = 20.0f;

        target[0] += amplification;
        target[1] += amplification;
        target[2] += amplification;
        values[0] += amplification;
        values[1] += amplification;
        values[2] += amplification;

        target[0] = (float) (Math
                .sqrt((target[0] * target[0] * buffer + values[0] * values[0])
                        / (1 + buffer)));
        target[1] = (float) (Math
                .sqrt((target[1] * target[1] * buffer + values[1] * values[1])
                        / (1 + buffer)));
        target[2] = (float) (Math
                .sqrt((target[2] * target[2] * buffer + values[2] * values[2])
                        / (1 + buffer)));

        target[0] -= amplification;
        target[1] -= amplification;
        target[2] -= amplification;
        values[0] -= amplification;
        values[1] -= amplification;
        values[2] -= amplification;
    }


    /*
     * Tablets have LANDSCAPE as default orientation, so screen rotation is 0 or 180 when the orientation is LANDSCAPE, and smartphones have PORTRAIT.
     * I use the next code to difference between tablets and smartphones:
     */
    public static int getScreenOrientation(Display display){
        int orientation;

        if(display.getWidth()==display.getHeight()){
            orientation = Configuration.ORIENTATION_SQUARE;
        }else{ //if width is less than height than it is portrait
            if(display.getWidth() < display.getHeight()){
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }else{ // if it is not any of the above it will definitly be landscape
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    private void debugSensorData(SensorEvent event) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < event.values.length; i++) {
            // ...
            if (event.values[0] > 1) {
                builder.append("--- SENSOR ---");
                builder.append("\nName: ");
                Sensor sensor = event.sensor;
                builder.append(sensor.getName());
                builder.append("\nType: ");
                builder.append(sensor.getType());
                builder.append("\nVendor: ");
                builder.append(sensor.getVendor());
                builder.append("\nVersion: ");
                builder.append(sensor.getVersion());
                builder.append("\nMaximum Range: ");
                builder.append(sensor.getMaximumRange());
                builder.append("\nPower: ");
                builder.append(sensor.getPower());
                builder.append("\nResolution: ");
                builder.append(sensor.getResolution());

                builder.append("\n\n--- EVENT ---");
                builder.append("\nAccuracy: ");
                builder.append(event.accuracy);
                builder.append("\nTime: ");
                builder.append(event.timestamp);
                builder.append("\nLocation:\n");
                if (mCurrentLocation !=null){
                    builder.append(mCurrentLocation.getLatitude()+ "\n" + mCurrentLocation.getLongitude());
                }
                builder.append("\nValues:\n");
            builder.append("   [");
            builder.append(i);
            builder.append("] = ");
            builder.append(event.values[i]);
            builder.append("\n");}
        }

        Log.d(TAG, builder.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    /* Sensor Processing/Rotation Matrix
     * Each time a sensor update happens the onSensorChanged method is called.
     * This is where we receive the raw sensor data.
     * First of all we want to take the sensor data from the accelerometer and magnetometer and smooth it out to reduce jitters.
     * From there we can call the getRotationMatrix function with our smoothed accelerometer and magnetometer data.
     * The rotation matrix that this outputs is mapped to have the y axis pointing out the top of the phone, so when the phone is flat on a table facing north, it will read {0,0,0}.
     * We need it to read {0,0,0} when pointing north, but sitting vertical. To achieve this we simply remap the co-ordinates system so the X axis is negative.
     * The following code example shows how this is acheived.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        loadNewSensorData(event);
        int type=event.sensor.getType();

        if (mAccelGravityData != null && mGeomagneticData != null) {

            if ((type==Sensor.TYPE_MAGNETIC_FIELD) || (type==Sensor.TYPE_GYROSCOPE)) {
                rootMeanSquareBuffer(bufferedAccelGData, mAccelGravityData);
                rootMeanSquareBuffer(bufferedMagnetData, mGeomagneticData);
                if (SensorManager.getRotationMatrix(mRotationMatrix, null, bufferedAccelGData, bufferedMagnetData)){

                    Display display = mWindowManager.getDefaultDisplay();
                    int orientation = getScreenOrientation(display);
                    int rotation = display.getRotation();

                    boolean dontRemapCoordinates  = (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0) ||
                            (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180) ||
                            (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90) ||
                            (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);

                    if( !dontRemapCoordinates){
                        SensorManager.remapCoordinateSystem(
                                mRotationMatrix,
                                SensorManager.AXIS_Y,
                                SensorManager.AXIS_MINUS_X,
                                mRotationMatrix);
                    }
                    debugSensorData(event);
                }
            }
            x1.setText(Float.toString(event.values[0]));
            y1.setText(Float.toString(event.values[1]));
            z1.setText(Float.toString(event.values[2]));

            for (int i = 0; i < event.values.length; i++) {
                String aa;
                long tt ;
                tt = Long.valueOf(event.timestamp);
                aa = DateFormat.getTimeInstance().format(new Date(tt));

                ychg.setText(mLastUpdateTime + "\n:" + aa);
                if (event.values[0] > 1) {
                    xchg.setText("X Value changed " + "\n" + mCurrentLocation.getLatitude() + "\n"+ mCurrentLocation.getLongitude());

                }

            }


        }


    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart fired ..............");
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop fired ..............");
        mGoogleApiClient.disconnect();
        Log.d(TAG, "isConnected ...............: " + mGoogleApiClient.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "Location update started ..............: ");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Firing onLocationChanged..............................................");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        Log.d(TAG, "UI update initiated .............");
        if (null != mCurrentLocation) {
            String lat = String.valueOf(mCurrentLocation.getLatitude());
            String lng = String.valueOf(mCurrentLocation.getLongitude());
            zchg.setText("At Time: " + mLastUpdateTime + "\n" +
                    "Latitude: " + lat + "\n" +
                    "Longitude: " + lng + "\n");
        } else {
            Log.d(TAG, "location is null ...............");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        Log.d(TAG, "Location update stopped .......................");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
            Log.d(TAG, "Location update resumed .....................");
        }
    }

}
