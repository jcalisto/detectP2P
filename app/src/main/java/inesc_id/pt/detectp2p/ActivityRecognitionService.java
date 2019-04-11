package inesc_id.pt.detectp2p;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.LinkedList;

import android.os.Handler;
import android.util.Log;

import inesc_id.pt.detectp2p.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.DataModels.ActivityDataContainer;
import inesc_id.pt.detectp2p.DataModels.ActivityDetected;
import inesc_id.pt.detectp2p.TripStateMachine.KeepAwakeReceiver;
import inesc_id.pt.detectp2p.TripStateMachine.TSMSnapshotHelper;
import inesc_id.pt.detectp2p.TripStateMachine.TripStateMachine;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.Utils.DateHelper;

/**
 * Created by admin on 7/11/17.
 */


public class ActivityRecognitionService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    public static final String TAG = "Recognition Service";
    public static final String PRIMARY_NOTIF_CHANNEL = "default";
    public static final int PRIMARY_FOREGROUND_NOTIF_SERVICE_ID = 1001;

    private final boolean fullDetectionMode = true;

    ArrayList<ActivityDataContainer> ActivitiesDetected;

    TripStateMachine tripStateMachine;

    Context context;

    int testActivity=7;

    FusedLocationProviderClient fusedLocationProviderClient;

    public static boolean serviceRunning=false;

    //////// Acceleration detection

    SensorManager mSensorManager;
    Sensor mSensor;

    int startId = 0;

    public ActivityRecognitionService() {
        //super("ActivityRecognitionService");

        ActivitiesDetected = new ArrayList<ActivityDataContainer>();
        accelerationDataLinkedList = new LinkedList<>();

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Binding
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private final IBinder mBinder = new ActivityRecognitionBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
        //return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Accelerometer callbacks
    ////////////////////////////////////////////////////////////////////////////////////////////////

    double gravity[] = {0,0,0};
    double linear_acceleration[] = {0,0,0};
    int counter = 0;
    final double alpha = 0.8;

    boolean isCalibrated = false;
    long lastAccelerationSampleTS=0;

    LinkedList<AccelerationData> accelerationDataLinkedList;
    int i = 0;

    long lastActivatedGPSTS = 0l;

    long timeToTestGps = 5 * 1000 * 60; //3 minutes in milliseconds

    boolean currentlyRequestingLocations = false;

    long referenceTimestamp = 0l;
    long referenceEventTimestamp = 0l;

    @SuppressWarnings("MissingPermission")
    @Override
    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        if(referenceTimestamp == 0l){
            //set reference timestamp
            referenceTimestamp = System.currentTimeMillis();
            referenceEventTimestamp = event.timestamp;
        }

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        if(isCalibrated) {

            //timestamp is in nanoseconds = 1s
            if(event.timestamp >= (lastAccelerationSampleTS+1000000000)) {


                if(tripStateMachine.currentState == TripStateMachine.state.still){

                    //gps was activated, but 6 minutes have passed and the state is still "still" -> remove location updates
                    if((System.currentTimeMillis() - lastActivatedGPSTS > timeToTestGps) && (lastActivatedGPSTS != 0l)){
                        Log.d(TAG, "GPS was activated, but X minutes have passed and the state is still still -> remove location updates and reinitialize state machine");
                        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                        currentlyRequestingLocations = false;
                        lastActivatedGPSTS = 0l;

                    }else{

                        if(currentlyRequestingLocations){
                            Log.d(TAG, "We are inside the GPS test period");
                        }else{
                            Log.d(TAG, "GPS is not activated and the time has not passed");
                        }

                        accelerationDataLinkedList.addLast(new AccelerationData(linear_acceleration[0],linear_acceleration[1],linear_acceleration[2],System.currentTimeMillis()));

                        i++;



                        if(i >= 10){

                            Log.d(TAG, "Processing last 10 seconds");


                            double meanAccel = computeAccelMean(accelerationDataLinkedList);

                            if(meanAccel > keys.MIN_ACCEL){

                                Log.d(TAG,"Mean acceleration in the last 10 seconds > 3.0 -> ACTIVATING GPS");

                                LocationRequest a = buildHighAccuracyAfterMovementRequest();

                                lastActivatedGPSTS = System.currentTimeMillis();

                                if(!currentlyRequestingLocations){
//                                    tripStateMachine.initializeStateMachine(false);
                                    Log.d(TAG, "Mean acceleration in the last 10 seconds > 3.0 -> ACTIVATING GPS");
                                }else{
                                    Log.d(TAG, "Extending GPS test period");
                                }

                                //if already in gps test period -> just increase the timestamp
                                if(!currentlyRequestingLocations) {
                                    fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                                    fusedLocationProviderClient.requestLocationUpdates(a,mLocationCallback,null);
                                    currentlyRequestingLocations = true;
                                }

                            }else{

                                Log.d(TAG, "Mean acceleration in the last 10 seconds < 3.0 -> doing nothing");

                            }

                            accelerationDataLinkedList.clear();
                            i=0;


                        }

                    }

                }

                // divide by 1000000 to get the delta in ms
                // add to the reference timestamp to get current timestamp
                long computedTimestamp = (event.timestamp - referenceEventTimestamp)/1000000 + referenceTimestamp;

                tripStateMachine.insertAccelerometerUpdate(new AccelerationData(linear_acceleration[0],linear_acceleration[1],linear_acceleration[2],computedTimestamp));

                lastAccelerationSampleTS = event.timestamp;




                Log.d(TAG, linear_acceleration[1] + "  " + linear_acceleration[1] + "  " + linear_acceleration[2] + " at " + System.currentTimeMillis() + " startid="+startId + "event ts: " + DateHelper.getDateFromTSString(computedTimestamp));
                Log.d(TAG, ""+Math.sqrt(linear_acceleration[0]*linear_acceleration[0]+linear_acceleration[1]*linear_acceleration[1]+linear_acceleration[2]*linear_acceleration[2]));


            }

        }else {
            Log.d(TAG, "Still calibrating accelerometer");
            counter++;
            //wait until the low pass filter

            if (counter > 10) {
                isCalibrated = true;
                lastAccelerationSampleTS = event.timestamp;

            }
        }

    }

    private double computeAccelMean(LinkedList<AccelerationData> accelerationDataLinkedList) {

        double result = 0d;

        for(AccelerationData accelerationData : accelerationDataLinkedList){

            result += Math.sqrt(accelerationData.getxValue()*accelerationData.getxValue()+accelerationData.getyValue()*accelerationData.getyValue()+accelerationData.getzValue()*accelerationData.getzValue());

        }

        return result/accelerationDataLinkedList.size();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class ActivityRecognitionBinder extends Binder {
        public ActivityRecognitionService getService() {
            return ActivityRecognitionService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void onTaskRemoved(Intent rootIntent)
    {

//        stopRecognition();
    }


    public void stopRecognition() {

        Log.d(TAG, "Stopself stop recog");

        //unregister listeners
        //do any other cleanup if required
        activitySimulationHandler.removeCallbacks(runnableCode);

        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        currentlyRequestingLocations = false;

        LocalBroadcastManager.getInstance(context).unregisterReceiver(mFullTripReceiver);

        LocalBroadcastManager.getInstance(context).unregisterReceiver(tripStartedReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(tripStartedRecoveredReceiver);

        stopAccelerometer();

        serviceRunning = false;

        LocalBroadcastManager.getInstance(context).unregisterReceiver(
                mFullTripReceiver);

        TSMSnapshotHelper tsmSnapshotHelper = new TSMSnapshotHelper(context);
        tsmSnapshotHelper.deleteAllSnapshotRecords();

        //stop service
        stopForeground(true);
        stopSelf(startId);

    }

    public void forceFinishTrip(){

        tripStateMachine.forceFinishTrip(false);

    }

    public void forceStopAccel(){
        stopAccelerometer();
    }

    public void forceStopLoc(){
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        currentlyRequestingLocations = false;
    }

    //testing alarm
    AlarmManager alarmMgr;




    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        context = getApplicationContext();

        buildGoogleApiClient();

        //register the broadcast receiver to handle receiving ActivityRecognitionResult
        //from the ActivityRecognitionService
//        LocalBroadcastManager.getInstance(context).registerReceiver(
//                mMessageReceiver, new IntentFilter("ActivityDetected"));

        LocalBroadcastManager.getInstance(context).registerReceiver(
                mFullTripReceiver, new IntentFilter("FullTripFinished"));

        LocalBroadcastManager.getInstance(context).registerReceiver(
                tripStartedReceiver, new IntentFilter("FullTripStarted"));

        LocalBroadcastManager.getInstance(context).registerReceiver(
                tripStartedRecoveredReceiver, new IntentFilter("FullTripStartedRecovered"));

//        intentActRecog = new Intent(context, ActivityRecogService.class);
//        pendingIntent = PendingIntent.getService(context, 0, intentActRecog, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        activityRecognitionClient = ActivityRecognition.getClient(context);
//
//
//        ActivityRecogService.setShouldStop(false);


        mGoogleApiClient.connect();

        activitySimulationHandler = new Handler();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //alarm
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Service onStartCommand " + startId);
        this.startId = startId;

        serviceRunning = true;

        //todo started here to test

        StartStateMachine startStateMachine = new StartStateMachine();
        startStateMachine.execute();

        return START_STICKY;
    }


    protected GoogleApiClient mGoogleApiClient;


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Google API methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionSuspended(int i) {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mInsideShopNotification);
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mInsideShopNotification);
        mGoogleApiClient.connect();

    }


    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.d(TAG, "Start onLocationResult");
            for (Location location : locationResult.getLocations()) {

                LocationDataContainer received = new LocationDataContainer(
                        System.currentTimeMillis(),
                        location.getAccuracy(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getSpeed(),
                        location.getTime());

//                kalmanFilterTest.filterAndAddLocation(location);

               Log.d(TAG, "Time of fix" + DateHelper.getDateFromTSString(location.getTime()));

                Log.d(TAG,"lat:"+location.getLatitude()+" lng:"+location.getLongitude()
                        + " ac:"+location.getAccuracy() + " " +location.getProvider() + " speed:" + location.getSpeed() + " loc time : " + location.getTime());

                Log.d(TAG,"CSV: " + location.getLatitude() + "," + location.getLongitude() + "," + "ACC " + location.getAccuracy() + " " +
                        DateHelper.getDateFromTSString(System.currentTimeMillis()));

                tripStateMachine = TripStateMachine.getInstance(context, false, fullDetectionMode);
                tripStateMachine.insertLocationUpdate(received, false);

            }
        }
    };

    private class StartStateMachine extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... urls) {

            Log.d(TAG,"Instantiating TripStateMachine");
            tripStateMachine = TripStateMachine.getInstance(context, false, fullDetectionMode);
            startAccelerometer();

            return null;
        }

        protected void onPostExecute(Void... urls) {


        }
    }


    Handler activitySimulationHandler;


    private LocationRequest buildHighAccuracyLocRequest(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(0);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Log.d(TAG,"Building high accuracy location request");

        return mLocationRequest;

    }

    private LocationRequest buildHighAccuracyAfterMovementRequest(){

        LocationRequest mLocationRequest = new LocationRequest();

        //testing
//        mLocationRequest.setInterval(20000);
        mLocationRequest.setInterval(10000);

        mLocationRequest.setFastestInterval(0);

        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Log.d(TAG,"Building high accuracy location request");

        return mLocationRequest;

    }

    private LocationRequest buildPowerBalancedLocRequest(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(0);

        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        Log.d(TAG,"Building power balanced location request");

        return mLocationRequest;

    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"Simulating an event with modality: ");
            ArrayList<ActivityDetected> test = new ArrayList<>();
            test.add(new ActivityDetected(testActivity,100));
            ActivityDataContainer testContainer = new ActivityDataContainer(System.currentTimeMillis(),test);
            tripStateMachine.insertActivityUpdate(testContainer);

            activitySimulationHandler.postDelayed(runnableCode, 100);
        }
    };

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "ActivityRecognitionResult" is broadcasted.
//

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "ActivityRecognitionResult" is broadcasted.
    @SuppressWarnings("MissingPermission")
    private BroadcastReceiver mFullTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback); // todo be careful this has been changed
            lastActivatedGPSTS = 0l;
            currentlyRequestingLocations = false;

            Log.d(TAG,"Fulltrip finished --- remove location updates / lastActivatedGPSTS = 0l");


        }
    };

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "ActivityRecognitionResult" is broadcasted.
    @SuppressWarnings("MissingPermission")
    private BroadcastReceiver tripStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //startAccelerometer(); todo be careful this has been changed
                Log.d(TAG, "Full trip has started - broacast received");

            //Request Location data to be gathered
            LocationRequest mLocationRequest = buildHighAccuracyLocRequest();

            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,mLocationCallback,null);
            currentlyRequestingLocations = true;

        }
    };

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "ActivityRecognitionResult" is broadcasted.
    @SuppressWarnings("MissingPermission")
    private BroadcastReceiver tripStartedRecoveredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //startAccelerometer(); todo be careful this has been changed

            Log.d(TAG,"Full trip has been recovered - broadcast message received");

            //Request Location data to be gathered
            LocationRequest mLocationRequest = buildHighAccuracyLocRequest();

            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,mLocationCallback,null);
            currentlyRequestingLocations = true;

        }
    };

    private void stopAccelerometer(){
        Log.d(TAG, "stopping accelerometer startid" + startId);
        mSensorManager.unregisterListener(this);


    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        Intent intent = new Intent(getApplicationContext(), KeepAwakeReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

        alarmMgr.cancel(alarmIntent);

    }



    private void startAccelerometer(){
//        mSensorManager.registerListener(this,mSensor,1000000);
        mSensorManager.registerListener(this,mSensor,1000000);
        Log.d(TAG, mSensor.toString());
        Log.d(TAG,"starting accelerometer startid" + startId);
//

        Intent intent = new Intent(getApplicationContext(), KeepAwakeReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,5000,
//                        2* 60 * 1000, alarmIntent);
                3 * 60 * 1000, alarmIntent);

//        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(), 60*1000, alarmIntent);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to Google API");
    }

    public interface keys{

        String highAccuracyMode = "highAccuracy";
        String powerBalanceMode = "powerBalance";

        double MIN_ACCEL = 2.5;


    }

}
