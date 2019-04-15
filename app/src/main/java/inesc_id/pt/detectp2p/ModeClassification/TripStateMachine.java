package inesc_id.pt.detectp2p.ModeClassification;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import inesc_id.pt.detectp2p.BuildConfig;
import inesc_id.pt.detectp2p.ModeClassification.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.ModeClassification.DataModels.ActivityDataContainer;
import inesc_id.pt.detectp2p.ModeClassification.DataModels.ActivityDetected;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTrip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripPart;
import inesc_id.pt.detectp2p.ModeClassification.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.ModeClassification.dataML.OngoingTripWrapper;
import inesc_id.pt.detectp2p.ModeClassification.dataML.SpeedDistanceWrapper;
import inesc_id.pt.detectp2p.ModeClassification.dataML.Trip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.WaitingEvent;
import inesc_id.pt.detectp2p.Utils.DateHelper;
import inesc_id.pt.detectp2p.Utils.LocationUtils;
import inesc_id.pt.detectp2p.Utils.MiscUtils;
import inesc_id.pt.detectp2p.Utils.NumbersUtil;

import static org.joda.time.DateTimeZone.UTC;

/**
 * Created by admin on 1/2/18.
 */

public class TripStateMachine {

//    private static String LOG_TAG = LogsUtil.INIT_LOG_TAG + "TripStateMachine";

    private static final String TAG = "TripStateMachine";

    public state currentState;
    Context context;

    // Constants
    final static int tripDistanceLimit = 115;

    int tripTimeLimit = 5*60*1000;

    int fullTripTimeLimit = 25*60*1000;

    final int tripSnapshotFreshTime = 30*60*1000;

    boolean testing;

    public PersistentTripStorage persistentTripStorage;
    TSMSnapshotHelper tsmSnapshotHelper;

    private static TripStateMachine instance = null;

    final Object lockStateMachine = new Object();

    // if true: machine acts as if in real mode, if true: machine force starts a trip
    boolean fullDetectionMode;

    RawDataPreProcessing rawDataPreProcessing;

    boolean manualTripStart = false;
    boolean manualTripEnd = false;


//    /// TODO-SUSPECT 0.6.17.99? IF SUSPECT SPEED ADD TO SUSPECT LIST
//    /// AND BREAK CASE
//    /// IF THERE ARE SUSPECTS AND DISTANCE TO CTBC Is LOWER THEN FLUSH SSPECT LIST
//    /// WHEN SUSPECT LIST IS LONGER THAN MAX_SUSPECTS AND/OR TIMESPAN LONGER THAN 2 MINUTE
//
//    LinkedList<LocationDataContainer> suspectLocations = new LinkedList<>();
//    boolean suspectTripSubState = false;
//    /*final double maxStartingTripVelocity =  50*3.6; // 50 km/h*/
//    final int MAX_SUSPECTS = 10;
//    final double SUSPECT_RADIUS_FACTOR = 1.5; // this factor times min trip distance should be inline with most common outlier errors, e.g., burst of 75 meters or more over min trip distance
//    long lastLocationReceivedTimestamp = 0;
//    long MAX_TIME_TO_SUSPECT = 5 * 60 * 1000;
//
//    //// TODO-SUSPECT 0.6.17.99 -- END

    public synchronized  static TripStateMachine getInstance(Context context, boolean testing, boolean fullDetectionMode){

        if(instance == null){
            Log.d(TAG, "instantiating new trip state machine");
            instance = new TripStateMachine(context,testing, fullDetectionMode);
        }
        return instance;

    }

    public OngoingTripWrapper getCurrentOngoingTrip(){

        synchronized (lockStateMachine) {

            if(currentState ==state.still){
                return null;
            }else{
                return new OngoingTripWrapper(tripList, currentState.getStateInt(), currentListOfLocations);
            }

        }

    }


    public void initializeStateMachine(boolean testing){

        //checkIfItCanBeRestored();
        tsmSnapshotHelper.deleteAllSnapshotRecords();

        currentState = state.still;
        tsmSnapshotHelper.saveState(state.still.getStateInt());

        currentToBeCompared = null;

        currentListOfLocations = new ArrayList<>();
        currentListOfActivities = new ArrayList<>();
        currentListOfAccelerations = new ArrayList<>();

        temporaryListOfLocations = new LinkedList<>();

        lastInsertedLocationTS = 0;
        lastValidLocationTS = 0;

        tripList= new ArrayList<FullTripPart>();


        this.testing = testing;

        if(testing){
            tripTimeLimit = 10000;
            fullTripTimeLimit = 40000;
        }

        manualTripStart = false;
        manualTripEnd = false;

    }

    public void forceStartTrip(){
        synchronized (lockStateMachine) {
            currentState = state.trip;
            manualTripStart = true;
            tsmSnapshotHelper.saveState(currentState.getStateInt());
            Log.d(TAG, "Telling the ActivityRecognitionService to change to high accuracy mode to force start trip");
            Intent localIntentTripStarted = new Intent("FullTripStarted");
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntentTripStarted);
        }
    }

    public void forceFinishTrip(boolean testing){

        synchronized (lockStateMachine) {
            if (currentState == state.still) {
                initializeStateMachine(testing);
            }else{

//                for(Segment segment : rawDataPreProcessing.rawDataDetection.tripEvaluation()){
//                    Log.e("---segmentML", "mode"  + segment.getMode() + " first index " + segment.getMode());
//                }

                manualTripEnd = true;

                if(currentState == state.trip) {

                    try{
                        ArrayList<FullTripPart> currentTrips = rawDataPreProcessing.rawDataDetection.classifyTrip(currentListOfLocations, currentListOfAccelerations, isFirstLeg());
                        tripList.addAll(currentTrips);

                    }catch(Exception e){

                        tsmSnapshotHelper.deleteAllSnapshotRecords();
                        initializeStateMachine(false);
                        Log.d(TAG, "Exception while trying to force finish current trip");
                        Log.d(TAG, e.getMessage());

                        // Broadcast that the full trip has finished so that other app components/modules may act on it
                        Intent localIntent = new Intent("FullTripFinished");
                        localIntent.putExtra("result", "FullTripFinished");
                        localIntent.putExtra("isTrip", false);
                        Toast.makeText(context, "Trip discarded. No legs were identified!",Toast.LENGTH_LONG).show();

                        tsmSnapshotHelper.deleteAllSnapshotRecords();
                        initializeStateMachine(testing);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                        return;
                    }

                    //ArrayList<FullTripPart> currentTrips = tripAnalysis.classifyTrip(currentListOfLocations, currentListOfActivities, currentListOfAccelerations);
                }

                // Broadcast that the full trip has finished so that other app components/modules may act on it
                Intent localIntent = new Intent("FullTripFinished");
                localIntent.putExtra("result", "FullTripFinished");

                //check if any of the trip parts has 0 locations
                int ftpNumber = 0;
                for (Iterator<FullTripPart> iter = tripList.listIterator(); iter.hasNext(); ) {
                    FullTripPart a = iter.next();
                    if (a.getLocationDataContainers().size() == 0) {
                        ftpNumber++;
                        iter.remove();
                    }
                }

                //check if after filtering 0 locations trip parts if any trip parts are left
                if(tripList.size()>0){

                    try{
                        FullTrip fullTripToBeSaved = analyseListOfTrips(tripList, manualTripStart, manualTripEnd);
                        logFullTripInfo(fullTripToBeSaved);

                        //save the full trip persistently
                        persistentTripStorage.insertFullTripObject(fullTripToBeSaved);
                        String dateId = DateHelper.getDateFromTSString(fullTripToBeSaved.getTripList().get(0).getInitTimestamp());

                        localIntent.putExtra("date", dateId);
                        localIntent.putExtra("isTrip", true);
                    }catch(Exception e){

                        tsmSnapshotHelper.deleteAllSnapshotRecords();
                        initializeStateMachine(false);

                        // Broadcast that the full trip has finished so that other app components/modules may act on it
                        localIntent.putExtra("isTrip", false);
                        Toast.makeText(context, "Trip discarded. No legs were identified!",Toast.LENGTH_LONG).show();

                        tsmSnapshotHelper.deleteAllSnapshotRecords();
                        initializeStateMachine(testing);

                        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                        return;

                    }


                }else{
                    Log.d(TAG, "Trip force finished but discarded(no legs)");
                    localIntent.putExtra("isTrip", false);
                    Toast.makeText(context, "Trip discarded. No legs were identified!",Toast.LENGTH_LONG).show();
                }

                tsmSnapshotHelper.deleteAllSnapshotRecords();
                initializeStateMachine(testing);

                LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                // Broadcast that the full trip has finished so that other app components/modules may act on it
                //Intent localIntent = new Intent("FullTripFinished");
                //localIntent.putExtra("result", "FullTripFinished");
                //LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
            }
        }
    }

    // method called whenever the state machine acknowledges that a trip (former full trip) has ended, computes the stats necessary
    // receives a list of full trip parts (trips/legs or waiting events)
    // returns the FullTrip
    public FullTrip analyseListOfTrips(ArrayList<FullTripPart> fullTripParts, boolean manualTripStart, boolean manualTripEnd) throws Exception{

        long distanceTraveled = 0;
        float maxSpeed = 0;

        float avgSpeed;

        int count = 0;
        for(FullTripPart ftp : fullTripParts){

            if(ftp.isTrip()){

                Trip trip = (Trip) ftp;
                SpeedDistanceWrapper speedDistanceWrapper = RawDataDetection.computeSpeedsDistance(ftp.getLocationDataContainers());

                //COMMENTED RULE!
//                if(trip.getSugestedModeOfTransport() == ActivityDetected.keys.still){
//
//                    LocationDataContainer initLoc = trip.getLocationDataContainers().get(0);
//                    LocationDataContainer endLoc = trip.getLocationDataContainers().get(trip.getLocationDataContainers().size()-1);
//
//                    if(LocationUtils.meterDistanceBetweenTwoLocations(initLoc, endLoc) > (TripStateMachine.tripDistanceLimit * 2.5)){
//                        trip.setSugestedModeOfTransport(ActivityDetected.keys.train);
//                        Log.d(TAG,"Switched still for train - leg distance: " + LocationUtils.meterDistanceBetweenTwoLocations(initLoc, endLoc));
//                    }
//
//                }
                distanceTraveled+=trip.getDistanceTraveled();
                if(trip.getMaxSpeed()>maxSpeed){
                    maxSpeed = trip.getMaxSpeed();
                }
            }
            count++;
        }


        //DELETED CODE - IF-THEN RULES TO MERGE/DELETE SPLITS

        long initTimestamp = fullTripParts.get(0).getInitTimestamp();
        long endTimestamp = fullTripParts.get(fullTripParts.size()-1).getEndTimestamp();

        avgSpeed = NumbersUtil.getSegmentSpeedKm(distanceTraveled,endTimestamp-initTimestamp);

        String uid = "TEST_USER X";

        return new FullTrip(fullTripParts,initTimestamp,endTimestamp,distanceTraveled,endTimestamp-initTimestamp, avgSpeed,maxSpeed, MiscUtils.getDeviceName(), "Android AppVersion " + BuildConfig.VERSION_CODE , MiscUtils.getOSVersion(), uid);
    }

    private TripStateMachine(Context context, boolean testing, boolean fullDetectionMode){

        tsmSnapshotHelper = new TSMSnapshotHelper(context);
        this.context = context;
        persistentTripStorage = new PersistentTripStorage(context);

        this.fullDetectionMode = fullDetectionMode;

        rawDataPreProcessing = RawDataPreProcessing.getInstance(context);

        Log.d(TAG, "Exists saved state?:"+tsmSnapshotHelper.existsSavedState());

        //check if the key "tripstate" exists
        if(tsmSnapshotHelper.existsSavedState() && (tsmSnapshotHelper.getSavedCurrentToBeCompared(null) != null) ){

            currentToBeCompared = tsmSnapshotHelper.getSavedCurrentToBeCompared(null);

            Log.d(TAG,"Time of current to be Compared="+ DateHelper.getDateFromTSString(currentToBeCompared.getSysTimestamp()) +
                    " Time now:"+ DateHelper.getDateFromTSString(new DateTime(UTC).getMillis()));

            Log.d(TAG,"State saved:" + tsmSnapshotHelper.getSavedState(-1));

            //  check how much time has passed since the current to be compared location saved...if
            // it has more than half an hour, discard snapshot and initialize state machine
            if(currentToBeCompared.getSysTimestamp() > (new DateTime(UTC).getMillis() - tripSnapshotFreshTime)){


                Log.d(TAG,"Recovering state");
                currentState = getStateFromInt(tsmSnapshotHelper.getSavedState(4));
                Log.d(TAG, "Recovered state:"+currentState);

                currentListOfLocations = tsmSnapshotHelper.getSavedLocations();
                currentListOfActivities = tsmSnapshotHelper.getSavedActivities();
                currentListOfAccelerations = tsmSnapshotHelper.getSavedAccelerationValues();
                tripList = tsmSnapshotHelper.getSavedFullTripParts();

                if(currentState != state.still){
                    Log.d(TAG, "Trip state was recovered from memory. Telling the ActivityRecognitionService to change to high accuracy mode");
                    Intent localIntentTripStarted = new Intent("FullTripStartedRecovered");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(localIntentTripStarted);
                }

                //else initialize the state machine from scratch
            }else{
                Log.d(TAG, "Snapshot is not fresh. Initializing State Machine from scratch.");
                initializeStateMachine(testing);
            }


        }else{
            Log.d(TAG, "Either saved state is null, current to be compared is null. Initializing State Machine from scratch.");
            initializeStateMachine(testing);
        }
        this.testing = testing;
    }

    private LocationDataContainer currentToBeCompared;

    //global
    private ArrayList<LocationDataContainer> currentListOfLocations;
    private ArrayList<ActivityDataContainer> currentListOfActivities;
    private ArrayList<AccelerationData> currentListOfAccelerations;

    //todo
//    private LinkedList<LocationDataContainer> temporaryListOfLocations = new LinkedList<>();
    private LinkedList<LocationDataContainer> temporaryListOfLocations = new LinkedList<>();

    //trip list
    ArrayList<FullTripPart> tripList;

    public void insertActivityUpdate(ActivityDataContainer activityDataContainer){

        //synchronized (currentState) {
        //if (currentState != state.still) {
        currentListOfActivities.add(activityDataContainer);
        tsmSnapshotHelper.saveActivity(activityDataContainer);


        for(ActivityDetected activityDetected: activityDataContainer.getListOfDetectedActivities()) {
            Log.d(TAG,"getType " + activityDetected.getType() + " getConf " + activityDetected.getConfidenceLevel());
        }
        //}
        //}
    }

    //counter of saved acceleration values - will only take snapshot of those values from 30 to 30 seconds
    private ArrayList<AccelerationData> cacheAccelerationValues = new ArrayList<>();

    public synchronized void insertAccelerometerUpdate(AccelerationData accelerationData){

        //   added still state in order to try to get the first accelerations of the trip to be
        // considered - just like the locations

        if((currentState == state.trip) || (currentState == state.waitingEvent) || (currentState == state.still)) {

            currentListOfAccelerations.add(accelerationData);

            if(currentState == state.trip){
                rawDataPreProcessing.insertAcceleration(accelerationData);
            }

            if(currentState != state.still) {

                cacheAccelerationValues.add(accelerationData);

                if (cacheAccelerationValues.size() >= 30) {
                    tsmSnapshotHelper.saveAccelerationValues(cacheAccelerationValues);
                    cacheAccelerationValues = new ArrayList<>();
                }
            }
        }

    }

    long lastInsertedLocationTS = 0;
    long lastValidLocationTS = 0;


    public long getlastValidLocationTS(){

        synchronized (lockStateMachine){
            return lastValidLocationTS;
        }

    }


    public synchronized void insertLocationUpdate(LocationDataContainer locationDataContainer, boolean fromAlarm) {
        Log.d(TAG, "Receiving location in trip state machine");
        synchronized (lockStateMachine) {

            if(lastInsertedLocationTS == 0){
                lastInsertedLocationTS = locationDataContainer.getLocTimestamp();
            }else{

                if(lastInsertedLocationTS == locationDataContainer.getLocTimestamp()){
                    Log.d(TAG, "Duplicate location. Discarding.");
                    return;
                }else{
                    lastInsertedLocationTS = locationDataContainer.getLocTimestamp();
                }

            }

            if ((locationDataContainer.getAccuracy() > 100) && (currentState == state.still)) {
                Log.d(TAG, "Still - Low accuracy - location discarded");
                return;
            }

            if ((locationDataContainer.getAccuracy() > 150) && (currentState != state.still)) {

                Log.d(TAG, "In leg/waiting event - Low accuracy - location discarded");
                return;

            }

            // if it gets here it means that the location is accepted
            lastValidLocationTS = locationDataContainer.getSysTimestamp();

            long ts = locationDataContainer.getSysTimestamp();

            if (currentState != state.still) {

                if(currentState == state.trip) {

                    rawDataPreProcessing.insertLocation(locationDataContainer);
                }

                currentListOfLocations.add(locationDataContainer);
                tsmSnapshotHelper.saveLocation(locationDataContainer);
            }


            //synchronized (currentState) {
            switch (currentState) {

                case still:

                    //in case it's the first location received by the state machine
                    if (currentToBeCompared == null) {
                        if(!fullDetectionMode){
                            forceStartTrip();
                        }
                        else{
                            Log.d(TAG,  "First location - full detection mode");
                        }
                        currentToBeCompared = locationDataContainer;
                        tsmSnapshotHelper.saveCurrentToBeCompared(currentToBeCompared);

                        Log.d(TAG,"Current to be compared equals null - setting " + locationDataContainer.getLatLng().toString());

                        temporaryListOfLocations.addFirst(locationDataContainer);

                    } else {

                        Log.d(TAG,"Distance between current to be compared - " + currentToBeCompared.getLatLng().toString() + " at " + DateHelper.getDateFromTSString(currentToBeCompared.getSysTimestamp()));
                        Log.d(TAG,"And current location - " + locationDataContainer.getLatLng().toString() + " at " + DateHelper.getDateFromTSString(locationDataContainer.getSysTimestamp()));
                        Log.d(TAG,"Is " + (LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared)));

                        //check if the distance between the location received and the one to be compared is larger than the limit
                        if ((LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared) >= tripDistanceLimit)) {

                            LinkedList<LocationDataContainer> toBeAdded  = new LinkedList<>();

                            Log.d(TAG, "List of temporary location size" + temporaryListOfLocations.size());
                            Log.d(TAG, "Checking which locations from the temporary list of location should be added to the trip ");

                            int i = -1;
                            for (LocationDataContainer tempLCD : temporaryListOfLocations){
                                i++;

                                Log.d(TAG,"checking location i " + tempLCD.getLatLng().toString() + "at " + DateHelper.getDateFromTSString(tempLCD.getSysTimestamp()));
                                Log.d(TAG,"Distance between " + i + " and " + "current location is " + LocationUtils.meterDistanceBetweenTwoLocations(tempLCD, locationDataContainer));
                                Log.d(TAG,"Time between them " + (locationDataContainer.getSysTimestamp() - tempLCD.getSysTimestamp()));


                                //todo trip start 5 minutes ago regardless of vector distance of each point to the CTBC
//                                if((LocationUtils.meterDistanceBetweenTwoLocations(tempLCD, locationDataContainer) < tripDistanceLimit)
//                                        && ((locationDataContainer.getSysTimestamp() - tempLCD.getSysTimestamp()) < tripTimeLimit )){

                                boolean broke=false;

                                if((locationDataContainer.getSysTimestamp() - tempLCD.getSysTimestamp()) < tripTimeLimit){

                                    Log.d(TAG, "Location " + i + " should be added to the new trip: " + DateHelper.getDateFromTSString(tempLCD.getSysTimestamp()));

                                    toBeAdded.addFirst(tempLCD);

                                }else{

                                    Log.d(TAG, "Location " + i + " fails condition. Break!");
//                                    }

                                    break;
                                }
                            }

                            currentListOfLocations.addAll(toBeAdded);

                            //  if no location was appended to the trip that has just started,
                            // it means we have to add the last location
                            if (toBeAdded.size() == 0){

                                Log.d(TAG,"No locations to be added in a 100meter/5 min radius, trying to get the last location");
                                // if temporary list of locations has locations, add the last one inserted
                                if (temporaryListOfLocations.size() > 0){
                                    Log.d(TAG,"Adding location from " + DateHelper.getDateFromTSString(temporaryListOfLocations.getFirst().getSysTimestamp()));
                                    currentListOfLocations.add(temporaryListOfLocations.getFirst());
                                }else{
                                    //if not (I think I will never happen but just in case)
                                    Log.d(TAG,"No locations to be added, plus no pior location in temp location list. Just adding current to be compared from " + DateHelper.getDateFromTSString(currentToBeCompared.getSysTimestamp()));
                                    currentListOfLocations.add(currentToBeCompared);
                                }

                            }

                            currentListOfLocations.add(locationDataContainer);
                            tsmSnapshotHelper.saveLocation(locationDataContainer);

                            currentToBeCompared = locationDataContainer;
                            tsmSnapshotHelper.saveCurrentToBeCompared(locationDataContainer);


                            Log.d(TAG, "Looking for accelerations that happened after the location acknowledged as trip start " + DateHelper.getDateFromTSString(currentListOfLocations.get(0).getSysTimestamp()));


                            insertSortedLocsAccels(currentListOfLocations, currentListOfAccelerations);

//                            for(AccelerationData ad : currentListOfAccelerations){
//                                if(ad.getTimestamp() > currentListOfLocations.get(0).getSysTimestamp()){
//
//                                    rawDataPreProcessing.insertAcceleration(ad);
//
//                                    Log.d(TAG, "Inserting acceleration (appended from still state) in classifier with ts " + DateHelper.getDateFromTSString(ad.getTimestamp()));
//                                }
//                            }
//
//                            for (LocationDataContainer lcd : currentListOfLocations){
//                                rawDataPreProcessing.insertLocation(lcd);
//                                Log.d(TAG, "Inserting location "+ lcd.getLatLng().toString() + " in classifier with from " + DateHelper.getDateFromTSString(lcd.getSysTimestamp()));
//                            }

                            currentState = state.trip;
                            tsmSnapshotHelper.saveState(state.trip.getStateInt());
                            //currentListOfLocations.add(locationDataContainer);

                            Log.d(TAG, "From still to trip");

                            Log.d(TAG, "Telling the ActivityRecognitionService to change to high accuracy mode");
                            Intent localIntentTripStarted = new Intent("FullTripStarted");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntentTripStarted);

                        }else{
                            Log.d(TAG,"Trip has not started. Inserting in temporary list of locations.");
                            temporaryListOfLocations.addFirst(locationDataContainer);
                        }
                    }

                    break;

                case trip:
                    Log.d(TAG, "  Current state: Trip");
                    // added this to support force start trip
                    /*if (currentToBeCompared == null) {
                        currentToBeCompared = locationDataContainer;
                        tsmSnapshotHelper.saveCurrentToBeCompared(locationDataContainer);
                        tsmSnapshotHelper.saveState(state.trip.getStateInt());
                        Log.d(TAG, "First location on a force started trip");*/
                    //else

                    //todo workaround for forcestart trip
                    if(currentToBeCompared == null){
                        currentToBeCompared = locationDataContainer;
                    }

                    // todo finish trip is no fresh locations for 30 minutes
                    // todo uncomment to next version

                    long differenceFromLastValid;

                    long lastLocationTS;
                    if(currentListOfLocations.size() == 0){
                        lastLocationTS = 0;
                        differenceFromLastValid = 0;
                    }else if(currentListOfLocations.size() == 1){
                        differenceFromLastValid = 0;
                        lastLocationTS = locationDataContainer.getSysTimestamp();
                    }else{
                        lastLocationTS = locationDataContainer.getSysTimestamp();
                        differenceFromLastValid = lastLocationTS - currentListOfLocations.get(currentListOfLocations.size()-2).getSysTimestamp();

                        Log.d(TAG,"curr to size -2 " + LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentListOfLocations.get(currentListOfLocations.size()-2)));
                    }

                    Log.d(TAG,"difference from current to ctbc" + (locationDataContainer.getSysTimestamp() - currentToBeCompared.getSysTimestamp()) + " ms");
                    Log.d(TAG,"difference from last valid" + differenceFromLastValid + " ms");
                    Log.d(TAG,"curr to ctbc" + LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared));


                    //if last valid location older than 30 minutes and new location is within the trip distance radius
                    if((LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared) <= tripDistanceLimit * 2.5) && (lastLocationTS != 0) &&
                            ((differenceFromLastValid > 30*1000*60))){

                        Log.d(TAG,"Last valid location older than 30 minutes and new location is within the trip distance radius - close trip");

                        //remove this location - does not belong to this closed trip
                        currentListOfLocations.remove(currentListOfLocations.size()-1);

                        ArrayList<FullTripPart> currentTrips =
                                rawDataPreProcessing.rawDataDetection.classifyTrip(currentListOfLocations, currentListOfAccelerations, isFirstLeg());

                        tripList.addAll(currentTrips);

                        try {
                            FullTrip fullTripToBeSaved = analyseListOfTrips(tripList, manualTripStart, manualTripEnd);

                            logFullTripInfo(fullTripToBeSaved);

                            //check if any of the trip parts has 0 locations
                            boolean foundErrors = false;
                            int ftpNumber = 0;
                            for (Iterator<FullTripPart> iter = fullTripToBeSaved.getTripList().listIterator(); iter.hasNext(); ) {
                                FullTripPart a = iter.next();
                                if (a.getLocationDataContainers().size() == 0) {
                                    Log.e(TAG, "FullTripPart " + ftpNumber + " has 0 locations, discarding it!");
                                    foundErrors = true;
                                    ftpNumber++;
                                    iter.remove();
                                }
                            }

                            //save the full trip persistently
                            persistentTripStorage.insertFullTripObject(fullTripToBeSaved);

                            tsmSnapshotHelper.deleteAllSnapshotRecords();

//                        if (foundErrors){
//                            LOG.error("Crashing app!");
//                            Crashlytics.getInstance().crash();
//                        }

                            String dateId = DateHelper.getDateFromTSString(fullTripToBeSaved.getTripList().get(0).getInitTimestamp());

                            initializeStateMachine(testing);

                            // Broadcast that the full trip has finished so that other app components/modules may act on it
                            Intent localIntent = new Intent("FullTripFinished");
                            localIntent.putExtra("result", "FullTripFinished");
                            localIntent.putExtra("date", dateId);
                            localIntent.putExtra("isTrip", true);

                            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
                        }catch (Exception e){

                            tsmSnapshotHelper.deleteAllSnapshotRecords();
                            initializeStateMachine(false);
                            Log.d(TAG,"Exception while trying to force finish current trip");
                            Log.d(TAG,e.getMessage());

                            Log.d(TAG, "Trip force finished but discarded(no legs)");
                            // Broadcast that the full trip has finished so that other app components/modules may act on it
                            Intent localIntent = new Intent("FullTripFinished");
                            localIntent.putExtra("result", "FullTripFinished");
                            localIntent.putExtra("isTrip", false);
                            Toast.makeText(context, "Trip discarded. No legs were identified!",Toast.LENGTH_LONG).show();

                            Log.d(TAG,"Caught exception trying force finish trip",e);

                            tsmSnapshotHelper.deleteAllSnapshotRecords();
                            initializeStateMachine(testing);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                            return;

                        }

                    }
                    else
                    if ((LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared) <= tripDistanceLimit)
                            && (ts - currentToBeCompared.getSysTimestamp() >= tripTimeLimit)) {
                        currentState = state.waitingEvent;
                        tsmSnapshotHelper.saveState(state.waitingEvent.getStateInt());

                        Log.d(TAG, "From trip to waiting event");

                        currentToBeCompared = locationDataContainer;
                        tsmSnapshotHelper.saveCurrentToBeCompared(locationDataContainer);

                        //code to add the last five minutes of the leg to the following waiting event

                        ArrayList<LocationDataContainer> legsLocationsToInsert = new ArrayList<>();
                        ArrayList<AccelerationData> legsAccelsToInsert = new ArrayList<>();

                        for (Iterator<LocationDataContainer> iter = currentListOfLocations.listIterator(); iter.hasNext(); ) {
                            LocationDataContainer a = iter.next();
                            if (locationDataContainer.getSysTimestamp() - a.getSysTimestamp() > tripTimeLimit*0.8) {
                                legsLocationsToInsert.add(a);
                                iter.remove();
                            }
                        }

                        for (Iterator<AccelerationData> iter = currentListOfAccelerations.listIterator(); iter.hasNext(); ) {
                            AccelerationData a = iter.next();
                            if (locationDataContainer.getSysTimestamp() - a.getTimestamp() > tripTimeLimit*0.8) {
                                legsAccelsToInsert.add(a);
                                iter.remove();
                            }
                        }


                        if(currentListOfLocations.size()>0) {
                            legsLocationsToInsert.add(currentListOfLocations.get(0));
                        }

                        //todo duplicate the first and last points to the corresponding trip parts

//                        for(LocationDataContainer ldc : currentListOfLocations){
//                            if (locationDataContainer.getSysTimestamp() - ldc.getSysTimestamp() > tripTimeLimit){
//                                legsLocationsToInsert.add(ldc);
//                            }else{
//                                weLocations.add(ldc);
//                            }
//                        }
//
//                        for(AccelerationData ad : currentListOfAccelerations){
//                            if (locationDataContainer.getSysTimestamp() - ad.getTimestamp() > tripTimeLimit){
//                                legsAccelsToInsert.add(ad);
//                            }else{
//                                weAccels.add(ad);
//                            }
//                        }

                        //currentTrip = new Trip(currentListOfLocations,currentListOfActivities,0,0,0,0,0,0,0);

//                        ArrayList<FullTripPart> currentTrips = tripAnalysis.classifyTrip(currentListOfLocations, currentListOfActivities, currentListOfAccelerations);
//

                        //todo just changed
//                        ArrayList<FullTripPart> currentTrips = rawDataPreProcessing.rawDataDetection.classifyTrip(currentListOfLocations, currentListOfAccelerations);
                        ArrayList<FullTripPart> currentTrips =
                                rawDataPreProcessing.rawDataDetection.classifyTrip(legsLocationsToInsert, legsAccelsToInsert, isFirstLeg());

//                        for(Segment segment : rawDataPreProcessing.rawDataDetection.tripEvaluation()){
//
//                            Log.d("segmentML", "mode"  + segment.getMode() + " first index " + segment.getMode());

                        //rawDataPreProcessing.
//
//                        }

                        tripList.addAll(currentTrips);
                        tsmSnapshotHelper.saveFullTripParts(currentTrips);

                        //todo commented
//                        currentListOfLocations = new ArrayList<>();
//                        currentListOfLocations.add(locationDataContainer);


                        tsmSnapshotHelper.deleteSavedLocations();

                        for(LocationDataContainer ldc : legsLocationsToInsert){
                            Log.d(TAG,"Location kept for leg " + DateHelper.getDateFromTSString(ldc.getSysTimestamp()));
                        }

                        for(LocationDataContainer ldc : currentListOfLocations){
                            tsmSnapshotHelper.saveLocation(ldc);
                            Log.d(TAG,"Location pushed to the waiting event " + DateHelper.getDateFromTSString(ldc.getSysTimestamp()));
                        }

                        tsmSnapshotHelper.deleteSavedAccelerationValues();
                        tsmSnapshotHelper.saveAccelerationValues(currentListOfAccelerations);

//                        currentListOfAccelerations = new ArrayList<>();
//                        cacheAccelerationValues.clear();



                    } else if ((LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared) >= tripDistanceLimit)) {
                        currentToBeCompared = locationDataContainer;
                        tsmSnapshotHelper.saveCurrentToBeCompared(locationDataContainer);

                        Log.d(TAG, "In trip - refreshed currentToBeCompared");

                        //distance covered is larger than the limit so we set a new location to be compared
                    }
                    break;

                case waitingEvent:
                    Log.d(TAG, "  Current state: Waiting event");

                    if ((LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared) >= tripDistanceLimit)) {


                        Log.d(TAG, "1st case-" + LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared));

                        LinkedList<LocationDataContainer> toBeAddedToTheNextLeg  = new LinkedList<>();

                        ListIterator<LocationDataContainer> listIter = currentListOfLocations.listIterator(currentListOfLocations.size());
                        while (listIter.hasPrevious()) {
                            LocationDataContainer prev = listIter.previous();
                            // Do something with prev here

                            if ((LocationUtils.meterDistanceBetweenTwoLocations(prev, locationDataContainer) < tripDistanceLimit)
                                    && (locationDataContainer.getSysTimestamp() - prev.getSysTimestamp() < tripTimeLimit)) {

                                Log.d(TAG,"To Be Added loc with ts: " + DateHelper.getDateFromTSString(prev.getSysTimestamp()));

                                toBeAddedToTheNextLeg.addFirst(prev);
                                listIter.remove();

                            } else {

                                toBeAddedToTheNextLeg.addFirst(currentListOfLocations.get(currentListOfLocations.size()-1));
//                                currentListOfLocations.clear();
//                                currentListOfLocations.addAll(toBeAddedToTheNextLeg);

                                //no need for this right? If the locations and accelerations belong
                                // to a trip they were automatically inserted into raw detection
//                                for (LocationDataContainer lcd : toBeAdded){
//
//                                    Log.d(TAG, "Adding loc with ts: " + DateHelper.getDateFromTSString(tempLCD.getSysTimestamp()));
//                                    rawDataPreProcessing.insertLocation(lcd);
//                                }
//                                currentListOfLocations.add(0,);
                                break;

                            }
                        }

                        Log.d(TAG,"From waiting event to trip - Waiting event locations");

                        for (LocationDataContainer ldc : currentListOfLocations) {
                            Log.d(TAG,DateHelper.getDateFromTSString(ldc.getSysTimestamp()));
                        }

                        //currentTrip = new WaitingEvent(currentListOfLocations,0,0,null);
                        FullTripPart currentTrip = analyseTripPart(currentListOfLocations, null, false, -1, null, -1);

                        currentListOfLocations = new ArrayList<>();
                        currentListOfLocations.addAll(toBeAddedToTheNextLeg);

                        tripList.add(currentTrip);
                        tsmSnapshotHelper.saveFullTripPart(currentTrip);

                        //ActivityDataContainer lastAct = currentListOfActivities.get(currentListOfActivities.size() -1);

                        //currentListOfActivities = new ArrayList<>();
//                        currentListOfLocations = new ArrayList<>();
                        tsmSnapshotHelper.deleteSavedLocations();

                        Log.d(TAG,"From waiting event to trip");

                        //TODO add locations after accelerations
//                        for(LocationDataContainer ldc : currentListOfLocations){
//                            tsmSnapshotHelper.saveLocation(ldc);
//                            rawDataPreProcessing.insertLocation(ldc);
//                            Log.d(TAG,DateHelper.getDateFromTSString(ldc.getSysTimestamp()));
//                        }



                        if (currentListOfLocations.size() > 0){

                            long initTS = currentListOfLocations.get(0).getSysTimestamp();

                            for (Iterator<AccelerationData> iter = currentListOfAccelerations.listIterator(); iter.hasNext(); ) {
                                AccelerationData a = iter.next();
                                if (a.getTimestamp() < initTS) {
                                    iter.remove();
                                }else{
                                    break;
                                }
                            }

                        }

//                        rawDataPreProcessing.insertLocation(locationDataContainer);
                        //currentListOfActivities.add(lastAct);

                        tsmSnapshotHelper.deleteSavedAccelerationValues();
                        tsmSnapshotHelper.saveAccelerationValues(currentListOfAccelerations);

//                        //todo new code
//                        // after shifting accelerations from waiting event to next leg, insert them in constantin
//                        for(AccelerationData accelerationData : currentListOfAccelerations){
//                            rawDataPreProcessing.insertAcceleration(accelerationData);
//                        }
//
//                        Log.d(TAG,"Locations appended to the next leg - trying to insert in classifier");
//
//                        for(LocationDataContainer ldc : currentListOfLocations){
//                            tsmSnapshotHelper.saveLocation(ldc);
//                            rawDataPreProcessing.insertLocation(ldc);
//                            Log.d(TAG,DateHelper.getDateFromTSString(ldc.getSysTimestamp()));
//                        }

                        for(LocationDataContainer ldc : currentListOfLocations){
                            tsmSnapshotHelper.saveLocation(ldc);
                        }

                        insertSortedLocsAccels(currentListOfLocations, currentListOfAccelerations);

                        currentState = state.trip;
                        tsmSnapshotHelper.saveState(state.trip.getStateInt());
                        currentToBeCompared = locationDataContainer;
                        tsmSnapshotHelper.saveCurrentToBeCompared(locationDataContainer);

                        Log.d(TAG, "From waiting event to trip");

                    } else if (fullDetectionMode && (LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared) <= tripDistanceLimit)
                            && (ts - currentToBeCompared.getSysTimestamp() >= fullTripTimeLimit)) {

                        Log.d(TAG, "2nd case-" + LocationUtils.meterDistanceBetweenTwoLocations(locationDataContainer, currentToBeCompared));

                        Log.d(TAG, "FullTripEnded");
                        Log.d(TAG, "Telling the ActivityRecognitionService to change to power balanced mode");

                        try{

                            FullTrip fullTripToBeSaved = analyseListOfTrips(tripList, manualTripStart, manualTripEnd);

                            logFullTripInfo(fullTripToBeSaved);

                            //check if any of the trip parts has 0 locations
                            boolean foundErrors = false;
                            int ftpNumber = 0;
                            for (Iterator<FullTripPart> iter = fullTripToBeSaved.getTripList().listIterator(); iter.hasNext(); ) {
                                FullTripPart a = iter.next();
                                if (a.getLocationDataContainers().size() == 0) {
                                    Log.e(TAG, "FullTripPart " + ftpNumber + " has 0 locations, discarding it!");
                                    foundErrors = true;
                                    ftpNumber++;
                                    iter.remove();
                                }
                            }

                            //save the full trip persistently
                            persistentTripStorage.insertFullTripObject(fullTripToBeSaved);

                            tsmSnapshotHelper.deleteAllSnapshotRecords();

//                        if (foundErrors){
//                            LOG.error("Crashing app!");
//                            Crashlytics.getInstance().crash();
//                        }

                            String dateId = DateHelper.getDateFromTSString(fullTripToBeSaved.getTripList().get(0).getInitTimestamp());

                            initializeStateMachine(testing);

                            // Broadcast that the full trip has finished so that other app components/modules may act on it
                            Intent localIntent = new Intent("FullTripFinished");
                            localIntent.putExtra("result", "FullTripFinished");
                            localIntent.putExtra("date", dateId);
                            localIntent.putExtra("isTrip", true);

                            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                        }catch (Exception e){

                            tsmSnapshotHelper.deleteAllSnapshotRecords();
                            initializeStateMachine(false);
                            Log.d(TAG,"Exception while trying close current trip");
                            Log.d(TAG,e.getMessage());

                            Log.d(TAG, "Trip force finished but discarded(no legs)");
                            // Broadcast that the full trip has finished so that other app components/modules may act on it
                            Intent localIntent = new Intent("FullTripFinished");
                            localIntent.putExtra("result", "FullTripFinished");
                            localIntent.putExtra("isTrip", false);
                            Toast.makeText(context, "Trip discarded. No legs were identified!",Toast.LENGTH_LONG).show();

                            Log.d(TAG,"Caught exception trying force finish trip",e);

                            tsmSnapshotHelper.deleteAllSnapshotRecords();
                            initializeStateMachine(testing);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                            return;

                        }

                    }

                    break;

                //}
            }

        }
    }

    public FullTripPart analyseTripPart(ArrayList<LocationDataContainer> locationDataContainers, ArrayList<ActivityDataContainer> activityDataContainers, boolean isTrip, int
            mostProbableTripActivity, ArrayList<AccelerationData> accelerationData, int correctedModeOfTransport){

        if(locationDataContainers.size()<2 && isTrip){
            return null;
        }

        long initTimestamp = locationDataContainers.get(0).getSysTimestamp();
        long endTimestamp = locationDataContainers.get(locationDataContainers.size()-1).getSysTimestamp();

        if(isTrip){

            long timeTraveled = endTimestamp - initTimestamp;
            double distanceTraveled = 0;
            float maxSpeed = 0;

            //compute average speed of segments with speed above the threshold
            //beware - speed threshold in meters per second
            final double speedThreshold = 1.08;
            int countSpeedAboveThreshold=0;
            double sumSpeedAboveThreshold=0;

            //compute time in which speed is below average
            double sumTimeSpeedBelowThreshold=0;

            //compute maxSpeed, avgSpeed, distance traveled
            LocationDataContainer lastLocation = null;

            float sumAccuracy=0;

            for (LocationDataContainer lcd : locationDataContainers) {

                sumAccuracy += lcd.getAccuracy();

                if(lastLocation != null) {

                    double distanceBetweenLastTwo = LocationUtils.meterDistanceBetweenTwoLocations(lcd, lastLocation);
                    double timeBetweenLastTwo = (lcd.getSysTimestamp() - lastLocation.getSysTimestamp());
                    float segmentSpeed = NumbersUtil.getSegmentSpeedKm(distanceBetweenLastTwo,timeBetweenLastTwo);

                    if(segmentSpeed>maxSpeed) maxSpeed = segmentSpeed;

                    distanceTraveled = distanceTraveled + distanceBetweenLastTwo;

                    //compute speed above threshold
                    if(segmentSpeed>=speedThreshold){
                        sumSpeedAboveThreshold += segmentSpeed;
                        countSpeedAboveThreshold++;
                    }else{
                        //compute percentage of time below threshold
                        sumTimeSpeedBelowThreshold+=timeBetweenLastTwo;
                    }

                    /*if (loopIter <= locationDataContainers.size() - 2) {

                        distanceTraveled += LocationUtils.meterDistanceBetweenTwoLocations(lcd, locationDataContainers.get(loopIter + 1));
                    }*/
                }
                lastLocation = lcd;
            }

            float avgSpeed = NumbersUtil.getSegmentSpeedKm(distanceTraveled,timeTraveled);

            //compute acceleration average below threshold(0.2)
            double accelerationSumAboveThreshold=0;
            double countAccelerationAboveThreshold=0;
            final double thresholdAccelerationValue=0.2;

            //compute acceleration average
            double accelerationSum=0;

            //compute time percentage below threshold
            double sumTimeAccelerationBelowThreshold=0;
            AccelerationData lastAccelerationData = null;

            for(AccelerationData ad : accelerationData){
                Double accLength = Math.sqrt(ad.getxValue()*ad.getxValue()+ad.getyValue()*ad.getyValue()+ad.getzValue()*ad.getzValue());
                accelerationSum += accLength;

                if(accLength>=thresholdAccelerationValue){
                    accelerationSumAboveThreshold += accLength;
                    countAccelerationAboveThreshold++;
                }

                if(lastAccelerationData !=null){
                    Double accLength2 = Math.sqrt(lastAccelerationData.getxValue()*lastAccelerationData.getxValue()+ad.getyValue()*lastAccelerationData.getyValue()+ad.getzValue()*lastAccelerationData.getzValue());

                    if(accLength2<=thresholdAccelerationValue){
                        sumTimeAccelerationBelowThreshold += (ad.getTimestamp()-lastAccelerationData.getTimestamp());
                    }

                }

                lastAccelerationData = ad;

            }

            //acceleration avg
            accelerationSum = accelerationSum/accelerationData.size();

            //filtered acceleration
            if(Double.isNaN(accelerationSumAboveThreshold) || countAccelerationAboveThreshold == 0){
                accelerationSumAboveThreshold = 0;
            }else{
                accelerationSumAboveThreshold = accelerationSumAboveThreshold/countAccelerationAboveThreshold;
            }

            //percentage of time acceleration values are below the threshold
            Double percentageOfTimeAccelerationBelowThreshold = sumTimeAccelerationBelowThreshold/(endTimestamp-initTimestamp);

            //filtered speed
            sumSpeedAboveThreshold = sumSpeedAboveThreshold/countSpeedAboveThreshold;

            if (Double.isNaN(sumSpeedAboveThreshold)){
                sumSpeedAboveThreshold = 0;
            }

            if(Double.isNaN(accelerationSum)){
                accelerationSum = 0;
            }

            //percentage of time speed values are below the threshold
            Double percentageOfTimeSpeedBelowThreshold = sumTimeSpeedBelowThreshold/(endTimestamp-initTimestamp);

            //accuracy avg (m/s)
            sumAccuracy = sumAccuracy/locationDataContainers.size();

            return new Trip(locationDataContainers,activityDataContainers,mostProbableTripActivity,initTimestamp,endTimestamp,(long) distanceTraveled,timeTraveled/1000,avgSpeed,maxSpeed,
                    accelerationSum, accelerationData, accelerationSumAboveThreshold, percentageOfTimeAccelerationBelowThreshold, sumSpeedAboveThreshold, percentageOfTimeSpeedBelowThreshold, sumAccuracy, mostProbableTripActivity, correctedModeOfTransport);



        }else{

            double sumLat=0;
            double sumLng=0;

            for(LocationDataContainer ldc : locationDataContainers){
                sumLat+=ldc.getLatitude();
                sumLng+=ldc.getLongitude();
            }

            ArrayList<LocationDataContainer> aux = new ArrayList<>();
            aux.addAll(locationDataContainers);

            double avgLat = sumLat/locationDataContainers.size();
            double avgLng = sumLng/locationDataContainers.size();

            return new WaitingEvent(aux,initTimestamp,endTimestamp,avgLat,avgLng);
        }
    }

    public void insertSortedLocsAccels(ArrayList<LocationDataContainer> locationsToInsert, ArrayList<AccelerationData> accelerationsToInsert){

        int numAccels = accelerationsToInsert.size();

        long firstLocTS = 0;
        if (locationsToInsert.size() > 0){
            firstLocTS = locationsToInsert.get(0).getSysTimestamp();
        }

        int currAccel = 0;

        if(numAccels == 0){
            for(LocationDataContainer location : locationsToInsert) {
                rawDataPreProcessing.insertLocation(location);
            }
            return;
        }

        for(LocationDataContainer location : locationsToInsert){

            while((accelerationsToInsert.get(currAccel).getTimestamp() <= location.getSysTimestamp())){

                if(currAccel == (numAccels - 1)) break;

                if(accelerationsToInsert.get(currAccel).getTimestamp() > firstLocTS){

                    rawDataPreProcessing.insertAcceleration(accelerationsToInsert.get(currAccel));

                    Log.d(TAG, "Inserting acceleration (appended from still state) in classifier with ts " + DateHelper.getDateFromTSString(accelerationsToInsert.get(currAccel).getTimestamp()));
                }

                currAccel++;

            }

            Log.d(TAG, "Inserting Location (appended from still state) in classifier with ts " + DateHelper.getDateFromTSString(location.getSysTimestamp()));

            rawDataPreProcessing.insertLocation(location);
        }

        while(currAccel < numAccels){

            if(accelerationsToInsert.get(currAccel).getTimestamp() > firstLocTS){

                rawDataPreProcessing.insertAcceleration(accelerationsToInsert.get(currAccel));

                Log.d(TAG, "Inserting acceleration (After last location) (appended from still state) in classifier with ts " + DateHelper.getDateFromTSString(accelerationsToInsert.get(currAccel).getTimestamp()));
            }

            currAccel++;
        }

    }


    private void logFullTripInfo(FullTrip fullTripToBeSaved) {

        Log.d(TAG,"---------------------------------------------------------------------");
        Log.d(TAG,"---------------------------------------------------------------------");

        Log.d(TAG,DateHelper.getDateFromTSString(fullTripToBeSaved.getInitTimestamp()));
        Log.d(TAG,DateHelper.getDateFromTSString(fullTripToBeSaved.getEndTimestamp()));


        int j = 0;
        for (FullTripPart ftp : fullTripToBeSaved.getTripList()) {

            Log.d(TAG,"---------------------------------------------------------------------");


            Log.d(TAG,"!!! init time ftp " + DateHelper.getDateFromTSString(ftp.getInitTimestamp()));
            Log.d(TAG,"!!! end time ftp " + DateHelper.getDateFromTSString(ftp.getEndTimestamp()));


            Log.d(TAG,"!!! is trip" + ftp.isTrip() + "");
            if (ftp.isTrip()) {

                int size = ((Trip) ftp).getAccelerationData().size();

                if (((Trip) ftp).getAccelerationData().size() > 0) {

                    Log.d(TAG,"!!! accel " + DateHelper.getDateFromTSString(((Trip) ftp).getAccelerationData().get(0).getTimestamp()));
                    Log.d(TAG,"!!! accel " + DateHelper.getDateFromTSString(((Trip) ftp).getAccelerationData().get(size - 1).getTimestamp()));
                } else {

                    Log.d(TAG,"!!! accel , No acceleration values in this leg");
                }



//                for(AccelerationData accelerationData : ((Trip) ftp).getAccelerationData()){
//
//
//                    if(((Trip) ftp).getAccelerationData().size()>0){
//                        Log.e("!!! accel", DateHelper.getDateFromTSString(accelerationData.getTimestamp()));
//                    }else{
//                        Log.e("!!! accel", "No acceleration values in this leg");
//                    }
//
//                }
            }

            for (LocationDataContainer locationDataContainer : ftp.getLocationDataContainers()) {

                Log.d(TAG, "!!! j=" + j +
                        DateHelper.getDateFromTSString(locationDataContainer.getSysTimestamp()) + " " + "lat:" + locationDataContainer.getLatitude() + " lng:" + locationDataContainer.getLongitude() + " + acc: " + locationDataContainer.getAccuracy());
                j++;
            }

        }
    }

    public boolean isFirstLeg() {
        return  (tripList.size()==0);
    }

    public state getStateFromInt(int savedState){

        switch(savedState){
            case 0:
                return state.still;
            case 1:
                return state.trip;
            case 2:
                return state.waitingEvent;
        }
        return null;
    }

    public enum state{
        still(0),
        trip(1),
        waitingEvent(2)
        ;

        private final int stateInt;

        state(int stateInt) {
            this.stateInt = stateInt;
        }

        public int getStateInt() {
            return this.stateInt;
        }


    }

}