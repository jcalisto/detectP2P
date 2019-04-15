package inesc_id.pt.detectp2p.ModeClassification;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import inesc_id.pt.detectp2p.ModeClassification.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.ModeClassification.DataModels.ActivityDetected;
import inesc_id.pt.detectp2p.ModeClassification.dataML.CheckMergesResult;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripPart;
import inesc_id.pt.detectp2p.ModeClassification.dataML.KeyValueWrapper;
import inesc_id.pt.detectp2p.ModeClassification.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.ModeClassification.dataML.MLInputMetadata;
import inesc_id.pt.detectp2p.ModeClassification.dataML.Segment;
import inesc_id.pt.detectp2p.ModeClassification.dataML.SpeedDistanceWrapper;
import inesc_id.pt.detectp2p.ModeClassification.dataML.Trip;
import inesc_id.pt.detectp2p.Utils.LocationUtils;
import inesc_id.pt.detectp2p.Utils.NumbersUtil;

public class RawDataDetection {

    private final static String TAG = "RawDataDetection";

    ArrayList<MLInputMetadata> outputsMetadata;

    private static RawDataDetection instance;

    PersistentTripStorage persistentTripStorage;

    Context context;

    private RawDataDetection(Context context) {
        outputsMetadata = new ArrayList<>();

        this.context = context;
        persistentTripStorage = new PersistentTripStorage(context);

        outputsMetadata = persistentTripStorage.getAllMLInputObjects(); //todo just commented
        Log.d(TAG, "retrived OM size:" +outputsMetadata.size());

    }

    public static RawDataDetection getInstance(Context context){

        if (instance == null){
            instance = new RawDataDetection(context);
        }

        return instance;
    }

    public ArrayList<MLInputMetadata> getOutputsMetadata() {
        return outputsMetadata;
    }


    public void insertMLMetadata(MLInputMetadata mlInputMetadata){
        //TODO GET SEGMENT INFO HERE
        outputsMetadata.add(mlInputMetadata);
        persistentTripStorage.insertMLInputObject(mlInputMetadata);
    }

    private ArrayList<Segment> legSeparation() {

        ArrayList<Segment> strongSegments = segmentIdentification(keys.WALKING_STRONG_MIN_PROB, ActivityDetected.keys.walking, -1, -1);

        Log.d(TAG, "All segments:");
        for (Segment s : strongSegments){
            Log.d(TAG, "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }

        ArrayList<Segment> tempSegmentsArray = new ArrayList<>();

        for (Segment segment : strongSegments) {

            if (segment.getLength() >= keys.WALKING_LENGTH_FILTER) {

                tempSegmentsArray.add(segment);

            }

        }

        strongSegments = tempSegmentsArray;

        Log.d(TAG, "Strong segments:");
        for (int j = 0; j < strongSegments.size(); j++) {
            Log.d(TAG, "{'mode': " + strongSegments.get(j).getMode() + ", 'probSum': " + strongSegments.get(j).getProbSum() + ", 'length': " + strongSegments.get(j).getLength() + ", 'firstIdx': " + strongSegments.get(j).getFirstIndex() + "}");

            int segmentFirstIdx = strongSegments.get(j).getFirstIndex();

            int segmentLastIdx = strongSegments.get(j).getFirstIndex() + strongSegments.get(j).getLength() - 1;

            int leftLim;

            if (j == 0) {
                leftLim = 0;
            } else {
                leftLim = strongSegments.get(j - 1).getFirstIndex() + strongSegments.get(j - 1).getLength();
            }

            int currIdx = segmentFirstIdx - 1;

            while (currIdx >= leftLim) {

                double walkingProb = outputsMetadata.get(currIdx).getProbabilityByMode(ActivityDetected.keys.walking);

                if (walkingProb >= keys.WALKING_STRONG_JOIN_PROB) {
                    int newFirstIdx = currIdx;
                    int newLength = strongSegments.get(j).getLength() + 1;
                    double newProbSum = strongSegments.get(j).getProbSum() + walkingProb;

                    Segment s = strongSegments.get(j);

                    s.setFirstIndex(newFirstIdx);
                    s.setLength(newLength);
                    s.setProbSum(newProbSum);

                    strongSegments.set(j, s);

                } else {
                    break;
                }

                currIdx -= 1;

            }

            int rightLim;

            if (j == strongSegments.size() - 1) {
                rightLim = outputsMetadata.size() - 1;
            } else {
                rightLim = strongSegments.get(j + 1).getFirstIndex() - 1;
            }

            currIdx = segmentLastIdx + 1;

            while (currIdx <= rightLim) {

                double walkingProb = outputsMetadata.get(currIdx).getProbabilityByMode(ActivityDetected.keys.walking);

                if (walkingProb >= keys.WALKING_STRONG_JOIN_PROB) {

                    int newLength = strongSegments.get(j).getLength() + 1;
                    double newProbSum = strongSegments.get(j).getProbSum() + walkingProb;


                    Segment s = strongSegments.get(j);
                    s.setLength(newLength);
                    s.setProbSum(newProbSum);
                    strongSegments.set(j, s);

                }else{
                    break;
                }

                currIdx +=1;

            }
        }//end for

        Log.d(TAG,  "Strong segments after point 1):");
        for (Segment s : strongSegments){
            Log.d(TAG, "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }

        HashMap<Integer, ArrayList<Segment>> candidatesPerStrong = new HashMap<>();
        //-1 = last //todo remember!
        candidatesPerStrong.put(-1, new ArrayList<Segment>());

        for(Segment segment : strongSegments){
            candidatesPerStrong.put(segment.getFirstIndex(), new ArrayList<Segment>());
        }

        for(int j = 0; j < strongSegments.size(); j++){
            int first;

            if (j == 0){
                first=0;
            }else{
                first = strongSegments.get(j-1).getFirstIndex() + strongSegments.get(j-1).getLength();
            }

            int last = strongSegments.get(j).getFirstIndex();
            ArrayList<Segment> candidateSegments = segmentIdentification(keys.WALKING_MIN_PROB_FOR_CANDIDATES, ActivityDetected.keys.walking, first, last);

            candidatesPerStrong.put(strongSegments.get(j).getFirstIndex(), candidateSegments);

            if(j == strongSegments.size()-1){
                first = strongSegments.get(j).getFirstIndex() + strongSegments.get(j).getLength();
                last = outputsMetadata.size();

                candidateSegments = segmentIdentification(keys.WALKING_MIN_PROB_FOR_CANDIDATES, ActivityDetected.keys.walking, first, last);

                candidatesPerStrong.put(-1, candidateSegments);
            }

        }

        strongSegments = mergeCandidates(strongSegments, candidatesPerStrong);

        return strongSegments;
    }


    public ArrayList<Segment> tripEvaluation() {

        Log.d(TAG,  "Probas (tripEval):");

        RawDataPreProcessing.getInstance(context).evaluateBuffer();

        int outputNum = 0;
        for (MLInputMetadata o : outputsMetadata){
            List<KeyValueWrapper> probasOrdered = o.getProbasOrdered();

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(outputNum);
            stringBuilder.append(" [");
            for (KeyValueWrapper currProba : probasOrdered){
                stringBuilder.append("(").append(currProba.getKey()).append(", ").append(currProba.getValue()).append("), ");
            }
            stringBuilder.append("]");
            Log.d(TAG, stringBuilder.toString());

            outputNum ++;
        }

        ArrayList<Segment> strongWalkingSegments = legSeparation();

        Log.d(TAG,  "Walking segments after point 2.1):");
        for (Segment s : strongWalkingSegments){
            Log.d(TAG, "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }

        ArrayList<Segment> potentiaLegs = generatePotentialLegs(strongWalkingSegments, -1, -1);

        Log.d(TAG, "Potencial legs:");
        for (Segment s : potentiaLegs){
            Log.d(TAG, "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }

        if (potentiaLegs.size() >= 2) {
            potentiaLegs = mergeConsecutiveLegs(potentiaLegs);
        }

        Log.d(TAG, "Potencial legs after merging consecutive legs:");
        for (Segment s : potentiaLegs){
            Log.d(TAG, "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }

        if (potentiaLegs.size() >= 2) {
            potentiaLegs = mergePotentialLegs(potentiaLegs, true);
        }

        Log.d(TAG, "Potencial legs after merging Potential legs (walking level):");
        for (Segment s : potentiaLegs){
            Log.d(TAG,  "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }


        LinkedHashMap<Integer, ArrayList<Segment>> segmentsToAdd = identifyStillSegments(potentiaLegs);
        List<Map.Entry<Integer, ArrayList<Segment>>> indexedList = new ArrayList<>(segmentsToAdd.entrySet());


        for (int i = indexedList.size() - 1; i >= 0; i--) {

            Map.Entry<Integer, ArrayList<Segment>> currentEntry = indexedList.get(i);
            Integer indexToInsert = currentEntry.getKey();
            potentiaLegs.remove(indexToInsert.intValue());

            ArrayList<Segment> newSegmentsToAdd = currentEntry.getValue();
            for (int j = newSegmentsToAdd.size() - 1; j >= 0; j--) {
                potentiaLegs.add(indexToInsert, newSegmentsToAdd.get(j));
            }

        }

        if (potentiaLegs.size() >= 2) {
            potentiaLegs = mergeConsecutiveLegs(potentiaLegs);
        }

        if(potentiaLegs.size() >= 2){
            int left = 0;
            int right = 1;

            int mergeWeakFilter = 3;

            while(right < potentiaLegs.size()){

                boolean wasMerged = false;


                if((potentiaLegs.get(left).getLength() <= mergeWeakFilter)
                        && (potentiaLegs.get(left).getProbSum() < ((double) potentiaLegs.get(left).getLength())/2.0)
                            && (potentiaLegs.get(right).getLength() > mergeWeakFilter)
                        ){

                    Segment rLeg = potentiaLegs.remove(right);

                    potentiaLegs.get(left).setMode(rLeg.getMode());

                    HashMap<Integer, Double> mProbas = calcModeInInterval(potentiaLegs.get(left).getFirstIndex(),
                            potentiaLegs.get(left).getFirstIndex()+potentiaLegs.get(left).getLength());

                    potentiaLegs.get(left).setProbSum(mProbas.get(rLeg.getMode()) + rLeg.getProbSum());

                    int currLength = potentiaLegs.get(left).getLength();
                    potentiaLegs.get(left).setLength(currLength + rLeg.getLength());

                    wasMerged = true;
                }

                if((!wasMerged) && (potentiaLegs.get(right).getLength()<= mergeWeakFilter)
                        && (potentiaLegs.get(right).getProbSum() < ((double) potentiaLegs.get(right).getLength())/2.0)
                            && (potentiaLegs.get(left).getLength() > mergeWeakFilter)){

                    Segment rLeg = potentiaLegs.remove(right);

                    int currLength = potentiaLegs.get(left).getLength();
                    potentiaLegs.get(left).setLength(currLength + rLeg.getLength());

                    HashMap<Integer, Double> mProbas = calcModeInInterval(rLeg.getFirstIndex(),
                            rLeg.getFirstIndex() + rLeg.getLength());

                    double currProbSum = potentiaLegs.get(left).getProbSum();
                    potentiaLegs.get(left).setProbSum(currProbSum + mProbas.get(potentiaLegs.get(left).getMode()));

                    wasMerged = true;

                }

                if(!wasMerged){
                    left+= 1;
                    right+= 1;
                }

            }

        }

        Log.d(TAG,  "Potencial legs after merging weak segments:");
        for (Segment s : potentiaLegs){
            Log.d(TAG,  "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + "}");
        }

        Log.d(TAG,  "Final legs res:");
        for (Segment s : potentiaLegs){
            int lastIdx = s.getFirstIndex() + s.getLength() - 1;
            double firstTsLeg = outputsMetadata.get(s.getFirstIndex()).getMlAlgorithmInput().getStartDate();
            double minutesFromTripStart = (firstTsLeg - (double) outputsMetadata.get(0).getMlAlgorithmInput().getStartDate()) / (1000.0 * 60.0);
            Log.d(TAG,  "{'mode': " + s.getMode() + ", 'probSum': " + s.getProbSum() + ", 'length': " + s.getLength() + ", 'firstIdx': " + s.getFirstIndex() + ", 'lastIdx': " + lastIdx + ", 'firstTimestamp': " + firstTsLeg + ", 'minutesFromTripStart': " + minutesFromTripStart + "}");
        }

        return potentiaLegs;

    }

    private LocationDataContainer computeIntermediateLocation(LocationDataContainer prevLocation, LocationDataContainer nextLocation, long intermediateTS){

        double prevLat, prevLong, nextLat, nextLong;
        double prevTime, nextTime;
        double newLat, newLong;

        prevLat = prevLocation.getLatitude();
        prevLong = prevLocation.getLongitude();
        prevTime = prevLocation.getSysTimestamp();

        nextLat = nextLocation.getLatitude();
        nextLong = nextLocation.getLongitude();
        nextTime = nextLocation.getSysTimestamp();

        newLat = prevLat + (nextLat - prevLat) * ((intermediateTS - prevTime) / (nextTime - prevTime));
        newLong = prevLong + (nextLong - prevLong) * ((intermediateTS - prevTime) / (nextTime - prevTime));

        float avgAcc = (prevLocation.getAccuracy() + nextLocation.getAccuracy())/2;

        return new LocationDataContainer(intermediateTS, avgAcc, newLat, newLong, 0, intermediateTS);

    }

    private void filterLocationsTriangulation(ArrayList<LocationDataContainer> locationsToEvaluate) {

        // TODO: FILTERING

        Log.d(TAG, "filteringLocationsTriangulation");
        Log.d(TAG, "num locs to evaluate: " + locationsToEvaluate.size());


        if (locationsToEvaluate.size () >= 3){
            for (int i = 1; i< locationsToEvaluate.size() -1 ; i++){

                double alpha=2;

                double prevLat, prevLong, currLat, currLong, nextLat, nextLong;
                double prevTime, currTime, nextTime;
                double prevCurr, prevNext, currNext;
                double newLat, newLong;

                prevLat = locationsToEvaluate.get(i-1).getLatitude();
                prevLong = locationsToEvaluate.get(i-1).getLongitude();
                prevTime = locationsToEvaluate.get(i-1).getSysTimestamp();

                currLat = locationsToEvaluate.get(i).getLatitude();
                currLong = locationsToEvaluate.get(i).getLongitude();
                currTime = locationsToEvaluate.get(i).getSysTimestamp();

                nextLat = locationsToEvaluate.get(i+1).getLatitude();
                nextLong = locationsToEvaluate.get(i+1).getLongitude();
                nextTime = locationsToEvaluate.get(i+1).getSysTimestamp();

                // TODO
                // FILTER: still take into account relative speeds, errors are just outliers very close in time, not trips back and forth
                prevNext= RawDataPreProcessing.simpleDistance( prevLat, prevLong, nextLat, nextLong);
                prevCurr =RawDataPreProcessing.simpleDistance( prevLat, prevLong, currLat, currLong);
                currNext = RawDataPreProcessing.simpleDistance( currLat, currLong, nextLat, nextLong);


                Log.d(TAG, "FILTER: prev: " + locationsToEvaluate.get(i-1).getLatLng().toString() + ", curr: " + locationsToEvaluate.get(i).getLatLng().toString() + " ,Next: " + locationsToEvaluate.get(i+1).getLatLng().toString());
                Log.d(TAG, "FILTER: distance: prevNext: " + prevNext + ", prevCurr: " + prevCurr + " ,currNext: " + currNext);


                if (prevNext * alpha < prevCurr + currNext)
                {

                    Log.d(TAG, "FILTER: CHECK distance: prevNext: " + prevNext + ", prevCurr: " + prevCurr + " ,currNext: " + currNext);


                    if ((nextTime-prevTime)==0){
                        Log.d(TAG, "FILTER: skip filtering prevTime too close to nexTime");
                    } else {

                        //TODO FILTER: media aritmetica nao tinha emconta o tempo de intervalo entre as samples e podia estar a linearizar velocidades
                        //locationsToEvaluate.get(i).setLatitude((prevLat + nextLat)/2);
                        //locationsToEvaluate.get(i).setLongitude((prevLong + nextLong)/2);

                        newLat = prevLat + (nextLat - prevLat) * ((currTime - prevTime) / (nextTime - prevTime));
                        newLong = prevLong + (nextLong - prevLong) * ((currTime - prevTime) / (nextTime - prevTime));

                        Log.d(TAG, "FILTER: CORRECTED: sample " + i + "/" + locationsToEvaluate.size() +" currLat: " + currLat +", currLong: " + currLong + ", CORRLat:" + newLat + ", CORRLong: " + newLong);

                        locationsToEvaluate.get(i).setLatitude(newLat);
                        locationsToEvaluate.get(i).setLongitude(newLong);

                    }

                }
            }
        }

    }


    public ArrayList<FullTripPart> classifyTrip(ArrayList<LocationDataContainer> locations, ArrayList<AccelerationData> accelerations,
                                                boolean addAdditionalLocations){

        ArrayList<FullTripPart> result = new ArrayList<>();

        if(outputsMetadata.size() == 0){
            cleanUp();
            return result;
        }

        //todo added
        filterLocationsTriangulation(locations);

        ArrayList<Segment> identifiedLegs = tripEvaluation();

        if(identifiedLegs.size() == 1){

            long lastTs = NumbersUtil.getLastTimestamp(accelerations, locations);

            SpeedDistanceWrapper speedDistanceWrapper = computeSpeedsDistance(locations);

            long firstTsLeg;

            if (addAdditionalLocations) {
                firstTsLeg = locations.get(0).getSysTimestamp();
            }else{
                firstTsLeg = outputsMetadata.get(identifiedLegs.get(0).getFirstIndex()).getMlAlgorithmInput().getStartDate();
            }

            Trip trip = new Trip(locations, accelerations, firstTsLeg, lastTs, identifiedLegs.get(0).getMode(),
                    speedDistanceWrapper.getAvgSpeed(), speedDistanceWrapper.getMaxSpeed(), speedDistanceWrapper.getDistance());

            result.add(trip);

            cleanUp();

            return result;

        }

        LocationDataContainer lastLocationOfLastLeg = null;

        for (int i = 1; i < identifiedLegs.size(); i++){

            Segment current = identifiedLegs.get(i-1);

            Segment next = identifiedLegs.get(i);

            long firstTsLeg;

            if((i == 1) && (addAdditionalLocations)) {
                firstTsLeg = locations.get(0).getSysTimestamp();
            }else{
                firstTsLeg = outputsMetadata.get(current.getFirstIndex()).getMlAlgorithmInput().getStartDate();
            }

            long lastTs = outputsMetadata.get(next.getFirstIndex()).getMlAlgorithmInput().getStartDate();

            ArrayList<LocationDataContainer> filteredLocations = new ArrayList<>();

            if(lastLocationOfLastLeg != null){
                filteredLocations.add(lastLocationOfLastLeg);
            }

            filteredLocations.addAll(getLocationsInsideInterval(locations, firstTsLeg, lastTs, false)); //todo pensar melhor
            ArrayList<AccelerationData> filteredAccelerations = getAccelerationsInsideInterval(accelerations, firstTsLeg, lastTs);

            //todo - check this
            if(filteredLocations.size() == 0 ){
                lastLocationOfLastLeg = null;
                continue;
            }

            LocationDataContainer nextLocationAfterInterval = getFirstLocationAfterTS(locations, lastTs);

            if(nextLocationAfterInterval != null){

                if(nextLocationAfterInterval.getSysTimestamp() == lastTs){
                    lastLocationOfLastLeg = nextLocationAfterInterval;
                }else{
                    lastLocationOfLastLeg = computeIntermediateLocation(filteredLocations.get(filteredLocations.size()-1), nextLocationAfterInterval, lastTs);
                    filteredLocations.add(lastLocationOfLastLeg);
                }

            }
//            else{
//                lastLocationOfLastLeg = filteredLocations.get(filteredLocations.size()-1);
//            }

            SpeedDistanceWrapper speedDistanceWrapper = computeSpeedsDistance(filteredLocations);


            Trip trip = new Trip(filteredLocations, filteredAccelerations, firstTsLeg, lastTs, current.getMode(), speedDistanceWrapper.getAvgSpeed(), speedDistanceWrapper.getMaxSpeed(), speedDistanceWrapper.getDistance());
            result.add(trip);
        }

        Segment current = identifiedLegs.get(identifiedLegs.size()-1);
        long firstTsLeg = outputsMetadata.get(current.getFirstIndex()).getMlAlgorithmInput().getStartDate();
        long lastTs = NumbersUtil.getLastTimestamp(accelerations, locations);

        ArrayList<LocationDataContainer> filteredLocations = new ArrayList<>();

        if(lastLocationOfLastLeg != null){
            filteredLocations.add(lastLocationOfLastLeg);
        }

        filteredLocations.addAll(getLocationsInsideInterval(locations, firstTsLeg, lastTs, true));
        ArrayList<AccelerationData> filteredAccelerations = getAccelerationsInsideInterval(accelerations, firstTsLeg, lastTs);

        if(filteredLocations.size()>0) {

            SpeedDistanceWrapper speedDistanceWrapper = computeSpeedsDistance(filteredLocations);

            Trip trip = new Trip(filteredLocations, filteredAccelerations, firstTsLeg, lastTs, current.getMode(), speedDistanceWrapper.getAvgSpeed(), speedDistanceWrapper.getMaxSpeed(), speedDistanceWrapper.getDistance());
            result.add(trip);
        }

        cleanUp();

        return result;
    }

    private LocationDataContainer getFirstLocationAfterTS(ArrayList<LocationDataContainer> locationDataContainers, long ts){

        for(LocationDataContainer ldc : locationDataContainers){

            if (ldc.getSysTimestamp() >= ts){
                return ldc;
            }
        }

        return null;
    }

    // receives a list of acceleration data, interval initial timestamp and interval end timestamp
    // returns the list of acceleration data points within the interval defined by the init timestamp and end timestamp
    private ArrayList<AccelerationData> getAccelerationsInsideInterval(ArrayList<AccelerationData> locationDataContainers, long initIntervalTS, long endIntervalTS){

        ArrayList<AccelerationData> accelerationInsideInterval = new ArrayList<>();

        for(AccelerationData adc : locationDataContainers){
            if(checkIfInInterval(initIntervalTS,endIntervalTS,adc.getTimestamp())){
                accelerationInsideInterval.add(adc);
            }
        }

        return accelerationInsideInterval;
    }

    public ArrayList<LocationDataContainer> getLocationsInsideInterval(ArrayList<LocationDataContainer> locationDataContainers, long initIntervalTS, long endIntervalTS, boolean isLastLeg){

        ArrayList<LocationDataContainer> locationsInsideInterval = new ArrayList<>();

        LocationDataContainer lastAdded = null;

        for(LocationDataContainer ldc : locationDataContainers){
            if(checkIfInInterval(initIntervalTS,endIntervalTS,ldc.getSysTimestamp())) {
                locationsInsideInterval.add(ldc);
                lastAdded = ldc;
            }
        }

        if(lastAdded != null && isLastLeg && locationDataContainers.size() > locationDataContainers.indexOf(lastAdded) + 1){
            locationsInsideInterval.add(locationDataContainers.get(locationDataContainers.indexOf(lastAdded)+1));
        }

        return locationsInsideInterval;
    }

    // checks if TS is within the interval defined by initTS and endTS
    private boolean checkIfInInterval(long initTS, long endTS, long TS){
        return TS >= initTS && TS < endTS;
    }


    private void cleanUp(){
        persistentTripStorage.dropAllMLInputObjects();
        RawDataPreProcessing.getInstance(context).clearRawDataPreProcessing();
        outputsMetadata = new ArrayList<>();
    }

    public static SpeedDistanceWrapper computeSpeedsDistance(ArrayList<LocationDataContainer> locationDataContainers){

        double distanceTraveled = 0;
        float maxSpeed = 0;

        //compute maxSpeed, avgSpeed, distance traveled
        LocationDataContainer lastLocation = null;

        long initTimestamp = locationDataContainers.get(0).getSysTimestamp();
        long endTimestamp = locationDataContainers.get(locationDataContainers.size()-1).getSysTimestamp();

        long timeTraveled = endTimestamp - initTimestamp;

        for (LocationDataContainer lcd : locationDataContainers) {

            if(lastLocation != null) {

                double distanceBetweenLastTwo = LocationUtils.meterDistanceBetweenTwoLocations(lcd, lastLocation);
                double timeBetweenLastTwo = (lcd.getSysTimestamp() - lastLocation.getSysTimestamp());
                float segmentSpeed = NumbersUtil.getSegmentSpeedKm(distanceBetweenLastTwo,timeBetweenLastTwo);

                if(segmentSpeed>maxSpeed) maxSpeed = segmentSpeed;

                distanceTraveled = distanceTraveled + distanceBetweenLastTwo;

            }
            lastLocation = lcd;
        }

        float avgSpeed = NumbersUtil.getSegmentSpeedKm(distanceTraveled,timeTraveled);

        long distanceTraveledLong = (long) distanceTraveled;

        return new SpeedDistanceWrapper(avgSpeed, maxSpeed, distanceTraveledLong);
    }

    private LinkedHashMap<Integer,ArrayList<Segment>> identifyStillSegments(ArrayList<Segment> potentiaLegs) {

        LinkedHashMap<Integer,ArrayList<Segment>> segmentsToAdd = new LinkedHashMap<>();

        for(int j  = 0; j < potentiaLegs.size(); j++){

            Segment leg = potentiaLegs.get(j);

            if (leg.getMode() != ActivityDetected.keys.walking){

                int first = leg.getFirstIndex();
                int last = leg.getFirstIndex() + leg.getLength();

                ArrayList<Segment> stillSegments = segmentIdentification(keys.STILL_STRONG_MIN_PROB, ActivityDetected.keys.still, first, last);

                ArrayList<Segment> stillStrongSegments = new ArrayList<>();
                ArrayList<Segment> candidateSegments = new ArrayList<>();

                for (Segment segment : stillSegments) {

                    if (segment.getLength() >= keys.STILL_LENGTH_FILTER) {

                        stillStrongSegments.add(segment);
                    }else{
                        candidateSegments.add(segment);
                    }

                }

                if(stillStrongSegments.size() == 0){continue;}

                if(candidateSegments.size() > 0){

                    HashMap<Integer, ArrayList<Segment>> candidatesPerStrong = new HashMap<>();
                    candidatesPerStrong.put(-1, new ArrayList<Segment>());

                    for(Segment segment : stillStrongSegments){
                        candidatesPerStrong.put(segment.getFirstIndex(), new ArrayList<Segment>());
                    }

                    for(Segment segment : stillStrongSegments){

                        while((candidateSegments.size() > 0) && (candidateSegments.get(0).getFirstIndex() < segment.getFirstIndex())){

                            Segment cand = candidateSegments.remove(0);
                            ArrayList<Segment> currCandidates = candidatesPerStrong.get(segment.getFirstIndex());

                            currCandidates.add(cand);
                            candidatesPerStrong.put(segment.getFirstIndex(), currCandidates);
                        }
                    }

                    while(candidateSegments.size() > 0){

                        Segment cand = candidateSegments.remove(0);
                        ArrayList<Segment> currCandidates = candidatesPerStrong.get(-1);

                        currCandidates.add(cand);
                        candidatesPerStrong.put(-1, currCandidates);
                    }

                    stillStrongSegments = mergeCandidates(stillStrongSegments, candidatesPerStrong);
                }

                ArrayList<Segment> potentialSegmentsInInterval = generatePotentialLegs(stillStrongSegments, first, last);

                if(potentialSegmentsInInterval.size() >= 2){
                    potentialSegmentsInInterval = mergePotentialLegs(potentialSegmentsInInterval, false);
                }

                if(potentialSegmentsInInterval.size() >= 2){
                    potentialSegmentsInInterval = mergeConsecutiveLegs(potentialSegmentsInInterval);
                }

                if(potentialSegmentsInInterval.size() > 1){
                    segmentsToAdd.put(j, potentialSegmentsInInterval);
                }

            }

        }

        return segmentsToAdd;
    }

    private ArrayList<Segment> mergePotentialLegs(ArrayList<Segment> potentiaLegs, boolean isWalkingLevel) {

        boolean runNext = true;

        while (runNext){

            List<Map.Entry<Integer, Integer>> orderedLegs = orderPotentialLegs(potentiaLegs);

            for(int i = 0; i < orderedLegs.size(); i++){

                if (orderedLegs.size() < 2){
                    runNext = false;
                    break;
                }

                int indexToCheck = orderedLegs.get(i).getKey();
                CheckMergesResult result;
                if(indexToCheck == 0){
                    result = checkMergesRight(indexToCheck, potentiaLegs, isWalkingLevel);
                } else if(indexToCheck == potentiaLegs.size()-1){

                    result = checkMergesLeft(indexToCheck, potentiaLegs, isWalkingLevel);

                }else{
                    result = checkMergesLeft(indexToCheck, potentiaLegs, isWalkingLevel);

                    if(!result.isWasMerged()){

                        result = checkMergesRight(indexToCheck, potentiaLegs, isWalkingLevel);

                    }
                }

                if (result.isWasMerged()){
                    break;
                }

                if ((i == orderedLegs.size()-1) && (!result.isWasMerged())){
                    runNext = false;
                }

            }


        }

        return potentiaLegs;

    }

    private CheckMergesResult checkMergesLeft(int indexToCheck, ArrayList<Segment> potentiaLegs, boolean isWalkingLevel) {

        int targetMode;
        int targetFilter;

        if (isWalkingLevel){
            targetMode = ActivityDetected.keys.walking;
            targetFilter = mergeFilters.WALK_EDGE;
        }else{
            targetMode = ActivityDetected.keys.still;
            targetFilter = mergeFilters.STILL_EDGE;
        }

        if(indexToCheck == 1){

            if((potentiaLegs.get(0).getMode() == targetMode) &&
                    (potentiaLegs.get(1).getMode() != targetMode) &&
                        (potentiaLegs.get(0).getLength() < targetFilter) &&
                            (potentiaLegs.get(1).getLength() >= mergeFilters.OTHERS_EDGE)){

                return mergeLeftTwo(0, potentiaLegs);

            }

            if ((potentiaLegs.get(0).getMode() != targetMode) &&
                    (potentiaLegs.get(1).getMode() == targetMode) &&
                        (potentiaLegs.get(0).getLength() < mergeFilters.OTHERS_EDGE)){

                if((potentiaLegs.get(1).getLength() >= targetFilter) ||
                        ((potentiaLegs.get(1).getLength() < targetFilter) && (potentiaLegs.get(1).getLength() >= potentiaLegs.get(0).getLength()) )
                        ){

                    return mergeLeftTwo(0, potentiaLegs);

                }

            }

            return new CheckMergesResult(potentiaLegs, false);
        }else{
            int left = indexToCheck - 2;
            int mid = indexToCheck - 1;
            int right = indexToCheck;

            return checkMergesBetween(right, mid, left, potentiaLegs, isWalkingLevel);
        }

    }


    private CheckMergesResult checkMergesRight(int indexToCheck, ArrayList<Segment> potentiaLegs, boolean isWalkingLevel) {

        int targetMode;
        int targetFilter;

        if (isWalkingLevel){
            targetMode = ActivityDetected.keys.walking;
            targetFilter = mergeFilters.WALK_EDGE;
        }else{
            targetMode = ActivityDetected.keys.still;
            targetFilter = mergeFilters.STILL_EDGE;
        }

        int last = potentiaLegs.size() - 1;
        int beforeLast = potentiaLegs.size() - 2;

        if(indexToCheck == beforeLast){

            if( (potentiaLegs.get(last).getMode() == targetMode)
                    && (potentiaLegs.get(beforeLast).getMode() != targetMode)
                    && (potentiaLegs.get(last).getLength() < targetFilter )
                    && (potentiaLegs.get(beforeLast).getLength() >= mergeFilters.OTHERS_EDGE))
            {
                       return mergeRightTwo(last, potentiaLegs);
            }

            if((potentiaLegs.get(last).getMode() != targetMode)
                    && (potentiaLegs.get(beforeLast).getMode() == targetMode)
                    && (potentiaLegs.get(last).getLength() < mergeFilters.OTHERS_EDGE )){

               if((potentiaLegs.get(beforeLast).getLength() >= targetFilter) ||
                       ((potentiaLegs.get(beforeLast).getLength() < targetFilter) && (potentiaLegs.get(beforeLast).getLength() >= potentiaLegs.get(last).getLength())) ){

                   return mergeRightTwo(last, potentiaLegs);

               }

            }

            return new CheckMergesResult(potentiaLegs, false);

        }else{
            int left = indexToCheck;
            int mid = indexToCheck + 1;
            int right = indexToCheck + 2;

            return checkMergesBetween(right, mid, left, potentiaLegs, isWalkingLevel);
        }

    }

    private CheckMergesResult checkMergesBetween(int right, int mid, int left, ArrayList<Segment> potentiaLegs, boolean isWalkingLevel) {

        CheckMergesResult result;
        int targetMode;
        int targetBetween;
        int othersBetweenTarget;

        if(isWalkingLevel){
            targetMode = ActivityDetected.keys.walking;
            targetBetween = mergeFilters.WALK_BETWEEN_OTHERS;
            othersBetweenTarget = mergeFilters.OTHERS_BETWEEN_WALK;
        }else{
            targetMode = ActivityDetected.keys.still;
            targetBetween = mergeFilters.STILL_BETWEEN_OTHERS;
            othersBetweenTarget = mergeFilters.OTHERS_BETWEEN_STILL;
        }

        if(potentiaLegs.get(mid).getMode() == targetMode){
            if ((potentiaLegs.get(left).getMode() == potentiaLegs.get(right).getMode()) && (potentiaLegs.get(mid).getLength() <= targetBetween)){
                return mergeThree(right, mid, left, potentiaLegs);
            }
        }

        if(potentiaLegs.get(mid).getMode() != targetMode){
            if((potentiaLegs.get(left).getMode() == potentiaLegs.get(right).getMode()) && (potentiaLegs.get(mid).getLength() <= othersBetweenTarget)){
                return mergeThree(right, mid, left, potentiaLegs);
            }
        }

        return new CheckMergesResult(potentiaLegs, false);

    }

    private CheckMergesResult mergeThree(int right, int mid, int left, ArrayList<Segment> potentiaLegs) {

        Segment rLeg = potentiaLegs.remove(right);
        Segment mLeg = potentiaLegs.remove(mid);

        int currLength = potentiaLegs.get(left).getLength();

        HashMap<Integer, Double> mProbas = calcModeInInterval(mLeg.getFirstIndex(), mLeg.getFirstIndex() + mLeg.getLength());

        double currProbSum = potentiaLegs.get(left).getProbSum();

        potentiaLegs.get(left).setLength(currLength + mLeg.getLength() + rLeg.getLength());
        potentiaLegs.get(left).setProbSum(currProbSum + mProbas.get(potentiaLegs.get(left).getMode()) + rLeg.getProbSum());

        return new CheckMergesResult(potentiaLegs, true);
    }

    private CheckMergesResult mergeRightTwo(int indexToMerge, ArrayList<Segment> potentiaLegs) {

        Segment lastSegment = potentiaLegs.remove(indexToMerge);

        HashMap<Integer, Double> mProbas = calcModeInInterval(lastSegment.getFirstIndex(), lastSegment.getFirstIndex()+lastSegment.getLength());

        double currProbSum = potentiaLegs.get(indexToMerge - 1).getProbSum();
        int currLength = potentiaLegs.get(indexToMerge -1).getLength();

        potentiaLegs.get(indexToMerge -1).setProbSum(currProbSum + mProbas.get(potentiaLegs.get(indexToMerge-1).getMode()));
        potentiaLegs.get(indexToMerge -1).setLength(currLength + lastSegment.getLength());

        return new CheckMergesResult(potentiaLegs, true);
    }

    private CheckMergesResult mergeLeftTwo(int indexToMerge, ArrayList<Segment> potentiaLegs) {

        Segment firstSegment = potentiaLegs.remove(indexToMerge);

        HashMap<Integer, Double> mProbas = calcModeInInterval(firstSegment.getFirstIndex(), firstSegment.getFirstIndex()+firstSegment.getLength());

        double currProbSum = potentiaLegs.get(indexToMerge).getProbSum();
        int currLength = potentiaLegs.get(indexToMerge).getLength();


        potentiaLegs.get(indexToMerge).setProbSum(currProbSum + mProbas.get(potentiaLegs.get(indexToMerge).getMode()));
        potentiaLegs.get(indexToMerge).setLength(currLength + firstSegment.getLength());
        potentiaLegs.get(indexToMerge).setFirstIndex(firstSegment.getFirstIndex());

        return new CheckMergesResult(potentiaLegs, true);
    }

    private List<Map.Entry<Integer, Integer>> orderPotentialLegs(ArrayList<Segment> potentiaLegs) {

       // LinkedHashMap<Integer, Double> orderedRes = (LinkedHashMap<Integer, Double>) SortMapByValue.sortByValueDesc(probasDicts);

        LinkedHashMap<Integer, Integer> allSize = new LinkedHashMap<>();

        for(int i = 0; i < potentiaLegs.size(); i++){

            allSize.put(i, potentiaLegs.get(i).getLength());

        }

        LinkedHashMap<Integer, Integer> orderedRes = (LinkedHashMap<Integer, Integer>) SortMapByValue.sortByValueDesc(allSize);

        List<Map.Entry<Integer, Integer>> orderedPotentialLegs = new ArrayList<>(orderedRes.entrySet());;

        return orderedPotentialLegs;
    }

    public ArrayList<Segment> mergeConsecutiveLegs(ArrayList<Segment> potentialLegs) {

        int left = 0;
        int right = 1;

        while(right < potentialLegs.size()){

            boolean wasMerged = false;

            if(potentialLegs.get(left).getMode() == potentialLegs.get(right).getMode()){

                Segment s = potentialLegs.remove(right);

                int currLength = potentialLegs.get(left).getLength();
                double currProbSum = potentialLegs.get(left).getProbSum();

                potentialLegs.get(left).setLength(currLength + s.getLength());
                potentialLegs.get(left).setProbSum(currProbSum + s.getProbSum());

                wasMerged = true;
            }

            if(!wasMerged){
                left += 1;
                right +=1;
            }

        }

        return potentialLegs;

    }


    public ArrayList<Segment> segmentIdentification(double strongMinProb, int mode, int firstIdx, int lastIdx){

        if((firstIdx == -1) && (lastIdx == -1)){
            firstIdx = 0;
            lastIdx = outputsMetadata.size();
        }

        ArrayList<Segment> segmentArrayList = new ArrayList<>();
        boolean prevIsStrong = false;

        for(int j= firstIdx; j<lastIdx; j++){
            KeyValueWrapper best = outputsMetadata.get(j).getBestMode();

            if(best.getValue() >= strongMinProb && best.getKey() == mode){

                if(j==0){
                    Segment newSegment = new Segment(best.getKey(), 1, best.getValue(), j);

                    segmentArrayList.add(newSegment);
                    prevIsStrong = true;
                    continue;
                }

                if(prevIsStrong){
                    int lastEl = segmentArrayList.size()-1;

                    int currLength = segmentArrayList.get(lastEl).getLength();
                    double currProbSum = segmentArrayList.get(lastEl).getProbSum();

                    segmentArrayList.get(lastEl).setLength(currLength+1);
                    segmentArrayList.get(lastEl).setProbSum(currProbSum + best.getValue());
                }else{
                    Segment newSegment = new Segment(best.getKey(), 1, best.getValue(), j);
                    segmentArrayList.add(newSegment);
                }
                prevIsStrong = true;
            }else{
                prevIsStrong = false;
            }

        }
        return segmentArrayList;
    }

    public ArrayList<Segment> mergeCandidates(ArrayList<Segment> strongSegments, HashMap<Integer, ArrayList<Segment>> candidatesPerStrong){

        for(int j =0; j<strongSegments.size(); j++){

            ArrayList<Segment> candidateSegments = candidatesPerStrong.get(strongSegments.get(j).getFirstIndex());

            if(j == 0){

                for(int k = candidateSegments.size()-1; k>-1; k--){
                    int lenSepSegment = strongSegments.get(j).getFirstIndex() - (candidateSegments.get(k).getFirstIndex() + candidateSegments.get(k).getLength());

                    if(lenSepSegment <= (candidateSegments.get(k).getLength() + 2)/2){

                        double currProbSum = strongSegments.get(j).getProbSum();
                        strongSegments.get(j).setProbSum(currProbSum + candidateSegments.get(k).getProbSum());

                        int m = candidateSegments.get(k).getFirstIndex() + candidateSegments.get(k).getLength();

                        for(; m < strongSegments.get(j).getFirstIndex(); m++){

                            currProbSum = strongSegments.get(j).getProbSum();

                            double probToAdd = outputsMetadata.get(m).getProbabilityByMode(strongSegments.get(j).getMode());
                            strongSegments.get(j).setProbSum(currProbSum + probToAdd);
                        }

                        strongSegments.get(j).setFirstIndex(candidateSegments.get(k).getFirstIndex());

                        int currLength = strongSegments.get(j).getLength();
                        strongSegments.get(j).setLength(currLength + candidateSegments.get(k).getLength() + lenSepSegment);

                    }else{
                        break;
                    }

                }

            }else{
                // # from right to left
                int lastMergedRight = candidateSegments.size();

                for(int k = candidateSegments.size()-1; k>-1; k--) {

                    int lenSepSegment = strongSegments.get(j).getFirstIndex() - (candidateSegments.get(k).getFirstIndex() + candidateSegments.get(k).getLength());

                    if(lenSepSegment <= (candidateSegments.get(k).getLength() + 2)/2) {
                        lastMergedRight = k;

                        double currProbSum = strongSegments.get(j).getProbSum();
                        strongSegments.get(j).setProbSum(currProbSum + candidateSegments.get(k).getProbSum());

                        int m = candidateSegments.get(k).getFirstIndex() + candidateSegments.get(k).getLength();

                        for(; m < strongSegments.get(j).getFirstIndex(); m++){

                            currProbSum = strongSegments.get(j).getProbSum();

                            double probToAdd = outputsMetadata.get(m).getProbabilityByMode(strongSegments.get(j).getMode());
                            strongSegments.get(j).setProbSum(currProbSum + probToAdd);
                        }

                        strongSegments.get(j).setFirstIndex(candidateSegments.get(k).getFirstIndex());

                        int currLength = strongSegments.get(j).getLength();
                        strongSegments.get(j).setLength(currLength + candidateSegments.get(k).getLength() + lenSepSegment);

                    } else {
                        break;
                    }

                    }

                    // # from left to right
                    for(int k = 0; k < lastMergedRight; k++){

                        int lenSepSegment = candidateSegments.get(k).getFirstIndex() - (strongSegments.get(j -1).getFirstIndex() + strongSegments.get(j-1).getLength());

                        if(lenSepSegment <= (candidateSegments.get(k).getLength() + 2)/2) {

                            double currProbSum = strongSegments.get(j-1).getProbSum();
                            strongSegments.get(j-1).setProbSum(currProbSum + candidateSegments.get(k).getProbSum());

                            int m = strongSegments.get(j-1).getFirstIndex() + strongSegments.get(j-1).getLength();

                            for(; m<candidateSegments.get(k).getFirstIndex(); m++){

                                currProbSum = strongSegments.get(j - 1).getProbSum();

                                double probToAdd = outputsMetadata.get(m).getProbabilityByMode(strongSegments.get(j).getMode());

                                strongSegments.get(j-1).setProbSum(currProbSum + probToAdd);
                            }

                            int currLength = strongSegments.get(j-1).getLength();
                            strongSegments.get(j-1).setLength(currLength + candidateSegments.get(k).getLength() + lenSepSegment);


                        }else{
                            break;
                        }

                        }

                }

                if(j==strongSegments.size()-1){

                    candidateSegments = candidatesPerStrong.get(-1);

                    // # from left to right
                    for(int k = 0; k<candidateSegments.size(); k++) {

                        int lenSepSegment = candidateSegments.get(k).getFirstIndex()-(strongSegments.get(j).getFirstIndex() + strongSegments.get(j).getLength());

                        if(lenSepSegment <= (candidateSegments.get(k).getLength() + 2)/2){

                            double currProbSum = strongSegments.get(j).getProbSum();
                            strongSegments.get(j).setProbSum(currProbSum + candidateSegments.get(k).getProbSum());

                            int m = strongSegments.get(j).getFirstIndex() + strongSegments.get(j).getLength();

                            for(; m<candidateSegments.get(k).getFirstIndex(); m++){

                                currProbSum = strongSegments.get(j).getProbSum();

                                double probToAdd = outputsMetadata.get(m).getProbabilityByMode(strongSegments.get(j).getMode());

                                strongSegments.get(j).setProbSum(currProbSum + probToAdd);

                            }
                            // strongSegments[j]["length"] += candidateSegments[k]["length"] + lenSepSegment
                            int currLength = strongSegments.get(j).getLength();
                            strongSegments.get(j).setLength(currLength + candidateSegments.get(k).getLength() + lenSepSegment);

                        }else{
                            break;
                        }

                    }
                }


        }

        return strongSegments;
    }

    public ArrayList<Segment> generatePotentialLegs(ArrayList<Segment> strongSegments, int intervalInit, int intervalEnd){

        if((intervalInit == -1) && (intervalEnd == -1)){
            intervalInit = 0;
            intervalEnd = outputsMetadata.size();
        }

        ArrayList<Segment> potentialLegs = new ArrayList<>();

        int first;
        int length;
        int bestModeSegment;
        double bestConfSegment;

        if(strongSegments.size() == 0){
            first = intervalInit;
            length = intervalEnd - intervalInit;

            Map.Entry<Integer, Double> best =  bestModeInInterval(-1, -1);

            Segment newSegment = new Segment(best.getKey(), length, best.getValue(), first);
            potentialLegs.add(newSegment);

        }else{

            for(int j = 0; j <  strongSegments.size(); j++){

                if((j==0) && (strongSegments.get(j).getFirstIndex() > intervalInit)){

                    first = intervalInit;
                    length = strongSegments.get(j).getFirstIndex() - first;
                    Map.Entry<Integer, Double> best =  bestModeInInterval(first, length);

                    Segment newSegment = new Segment(best.getKey(), length, best.getValue(), first);
                    potentialLegs.add(newSegment);
                }

                if(j > 0){

                    first = strongSegments.get(j-1).getFirstIndex() + strongSegments.get(j-1).getLength();
                    length = strongSegments.get(j).getFirstIndex() - first;

                    if(length > 0){
                        Map.Entry<Integer, Double> best =  bestModeInInterval(first, length);

                        Segment newSegment = new Segment(best.getKey(), length, best.getValue(), first);
                        potentialLegs.add(newSegment);
                    }

                }

                Segment newSegment = new Segment(
                        strongSegments.get(j).getMode(),
                        strongSegments.get(j).getLength(),
                        strongSegments.get(j).getProbSum(),
                        strongSegments.get(j).getFirstIndex());

                potentialLegs.add(newSegment);

                if((j == strongSegments.size()-1) && (strongSegments.get(j).getFirstIndex() + strongSegments.get(j).getLength())< intervalEnd){

                    first = strongSegments.get(j).getFirstIndex() + strongSegments.get(j).getLength();
                    length = intervalEnd - first;

                    Map.Entry<Integer, Double> best =  bestModeInInterval(first, length);

                    potentialLegs.add(new Segment(best.getKey(), length, best.getValue(), first));

                }
            }
        }

    return  potentialLegs;
    }


    public Map.Entry<Integer, Double> bestModeInInterval(int init, int length){

        if((init == -1) && (length == -1)){
            init = 0;
            length = outputsMetadata.size();
        }

        int end = init+length;

        HashMap<Integer, Double> legProbas = calcModeInInterval(init, end);

        LinkedHashMap<Integer, Double> orderedRes = (LinkedHashMap<Integer, Double>) SortMapByValue.sortByValueDesc(legProbas);

        List<Map.Entry<Integer, Double>> indexedList = new ArrayList<>(orderedRes.entrySet());
        Map.Entry<Integer,Double> bestMode = indexedList.get(0);

        return bestMode;
    }

    public HashMap<Integer, Double> calcModeInInterval(int init, int end){

        ArrayList<Integer> keys = new ArrayList<>(outputsMetadata.get(0).getProbasDicts().keySet());

        HashMap<Integer, Double> legProbas = new HashMap<>();

        for(Integer key : keys){
            legProbas.put(key, 0.0);
        }

        for(int k = init; k<end; k++){

            for(Integer key : keys){
                legProbas.put(key, legProbas.get(key) + outputsMetadata.get(k).getProbabilityByMode(key));

            }

        }
        return legProbas;
    }

    public interface mergeFilters{

        int WALK_EDGE = RawDataPreProcessing.timeInSegmentsNumber(90);
        int OTHERS_EDGE = RawDataPreProcessing.timeInSegmentsNumber(180);
        int STILL_EDGE =  RawDataPreProcessing.timeInSegmentsNumber(135);
        int WALK_BETWEEN_OTHERS =  RawDataPreProcessing.timeInSegmentsNumber(180);
        int OTHERS_BETWEEN_WALK =  RawDataPreProcessing.timeInSegmentsNumber(180);
        int STILL_BETWEEN_OTHERS =  RawDataPreProcessing.timeInSegmentsNumber(180);
        int OTHERS_BETWEEN_STILL =  RawDataPreProcessing.timeInSegmentsNumber(180);

    }

    public interface keys{

        double WALKING_STRONG_MIN_PROB = 0.75;
        double WALKING_STRONG_JOIN_PROB = 0.5;
        double WALKING_MIN_PROB_FOR_CANDIDATES = 0.50;

        double STILL_STRONG_MIN_PROB = 0.9;

        int WALKING_LENGTH_FILTER = RawDataPreProcessing.timeInSegmentsNumber(90);
        int STILL_LENGTH_FILTER = RawDataPreProcessing.timeInSegmentsNumber(180);
    }

}