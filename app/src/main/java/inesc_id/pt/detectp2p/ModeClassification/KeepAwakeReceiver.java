package inesc_id.pt.detectp2p.ModeClassification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.joda.time.DateTime;

import inesc_id.pt.detectp2p.ModeClassification.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.Utils.DateHelper;

import static org.joda.time.DateTimeZone.UTC;

public class KeepAwakeReceiver extends BroadcastReceiver{

    private final static String TAG = "KeepAwakeReceiver";

    TripStateMachine tripStateMachine;
    FusedLocationProviderClient fusedLocationProviderClient;


    @SuppressWarnings("MissingPermission")
    //per https://stackoverflow.com/questions/25207548/android-how-to-execute-a-method-every-x-hours-or-minutes/25208088
    @Override
    public void onReceive(Context context, Intent intent)
    {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        Log.d(TAG, "Alarm fired at " + DateHelper.getDateFromTSString(new DateTime(UTC).getMillis()));
        tripStateMachine = TripStateMachine.getInstance(context, false, true);
        long lastValidLocationTS = tripStateMachine.getlastValidLocationTS();

        //3 minutes
        if((new DateTime(UTC).getMillis() - lastValidLocationTS) > 3 * 1000 * 60){
            Log.d(TAG,"Last valid location - more than 3 minutes ago - asking for gps");
//            LocationManager mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//            mLocationManager.requestSingleUpdate("gps", locationListener, null);

            fusedLocationProviderClient.requestLocationUpdates(buildHighAccuracyLocRequest(),mLocationCallback,null);


        }else{
            Log.d(TAG,"Last valid location - less than 3 minutes ago - dont ask for gps");
        }

    }
//
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {

                LocationDataContainer received = new LocationDataContainer(
                        new DateTime(UTC).getMillis(),
                        location.getAccuracy(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getSpeed(),
                        location.getTime());

                Log.d(TAG,"CSV: " + location.getLatitude() + "," + location.getLongitude() + "," + "ACC " + location.getAccuracy() + " " +
                        DateHelper.getDateFromTSString(new DateTime(UTC).getMillis()) + " from KeepAwakeReceiver");

                tripStateMachine.insertLocationUpdate(received, false);
            }
        };
    };

    private LocationRequest buildHighAccuracyLocRequest(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Log.d(TAG,"Building high accuracy location request");

        return mLocationRequest;

    }
}