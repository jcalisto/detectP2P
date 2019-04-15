package inesc_id.pt.detectp2p.ModeClassification;

import android.content.Context;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import inesc_id.pt.detectp2p.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.ModeClassification.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.ModeClassification.dataML.MLAlgorithmInput;
import inesc_id.pt.detectp2p.ModeClassification.dataML.MLInputMetadata;
import inesc_id.pt.detectp2p.ModeClassification.dataML.ProcessedAccelerations;
import inesc_id.pt.detectp2p.ModeClassification.dataML.ProcessedPoints;

import static org.joda.time.DateTimeZone.UTC;

public class RawDataPreProcessing {

    private final static String TAG = "RawDataPreProcess";

    static RawDataPreProcessing instance;

    long startSegmentDate = -1;

    final Object rawDataInsertionLock = new Object();

    ArrayList<LocationDataContainer> locations;
    ArrayList<AccelerationData> accelerations;

    int segmNextIdxLoc = -1;
    int segmNextIdxAccel = -1;

    LocationDataContainer lastPrevPoint = null;

    boolean prevSpeedZero = false;
    boolean ignoreNextZeros = false;
    boolean hasSpeedOnBeginning = false;
    ArrayList<LocationDataContainer> lastSegmentPoints;

    ArrayList<MLAlgorithmInput> buffer;

    Classifier classifier;

    Context context;

    RawDataDetection rawDataDetection;

    private RawDataPreProcessing(Context context){

        clearRawDataPreProcessing();

        this.context = context;
        ///Machine learning

        rawDataDetection = RawDataDetection.getInstance(context);

        Classifier.initClassifier(context, "randomForest.pmml.ser");
        classifier = Classifier.getInstance();


    }

    public void clearRawDataPreProcessing(){

        locations = new ArrayList<>();
        accelerations = new ArrayList<>();
        lastSegmentPoints = new ArrayList<>();
        buffer = new ArrayList<>();
        startSegmentDate = -1;

        segmNextIdxLoc = -1;
        segmNextIdxAccel = -1;

        lastPrevPoint = null;

        prevSpeedZero = false;
        ignoreNextZeros = false;
        hasSpeedOnBeginning = false;

    }

    public static RawDataPreProcessing getInstance(Context context){

        if(instance == null){
            instance = new RawDataPreProcessing(context);
        }

        return instance;

    }


    public void insertAcceleration(AccelerationData accelerationData){
       Log.d(TAG, "trying to insert = "  + accelerationData.getTimestamp() );
        synchronized (rawDataInsertionLock) {
            if (startSegmentDate == -1) {
                startSegmentDate = accelerationData.getTimestamp();
            }

            long currSegmentDuration = accelerationData.getTimestamp() - startSegmentDate;

            if((segmNextIdxAccel == -1) && (currSegmentDuration > values.TIME_BEFORE_OVERLAP)){
                segmNextIdxAccel = accelerations.size();
                segmNextIdxLoc = locations.size();
            }

            if(currSegmentDuration > values.SEGMENT_DURATION) {

                processSegment();
                long newStartDate = getMinTimestamp();
                if (newStartDate == -1){
                    startSegmentDate = accelerationData.getTimestamp();
                } else {
                    startSegmentDate = newStartDate;

                    currSegmentDuration = accelerationData.getTimestamp() - startSegmentDate;
                    if((segmNextIdxAccel == -1) && (currSegmentDuration > values.TIME_BEFORE_OVERLAP)){
                        segmNextIdxAccel = accelerations.size();
                        segmNextIdxLoc = locations.size();
                    }
                    if(currSegmentDuration > values.SEGMENT_DURATION) {
                        processSegment();
                        startSegmentDate = accelerationData.getTimestamp();
                    }
                }
            }
            accelerations.add(accelerationData);
        }

    }

    public void insertLocation(LocationDataContainer locationDataContainer){
        Log.d(TAG, "Trying to insert location at"  + new DateTime(UTC).getMillis() );

        synchronized (rawDataInsertionLock) {

            Log.d(TAG, "Getting to insert location at"  + new DateTime(UTC).getMillis() );

            if (startSegmentDate == -1) {
                startSegmentDate = locationDataContainer.getSysTimestamp();
            }

            long currSegmentDuration = locationDataContainer.getSysTimestamp() - startSegmentDate;

            if((segmNextIdxLoc == -1) && (currSegmentDuration > values.TIME_BEFORE_OVERLAP)){
                segmNextIdxAccel = accelerations.size();
                segmNextIdxLoc = locations.size();
            }

            if(currSegmentDuration > values.SEGMENT_DURATION) {
                processSegment();
                long newStartDate = getMinTimestamp();
                if (newStartDate == -1){
                    startSegmentDate = locationDataContainer.getSysTimestamp();
                } else {
                    startSegmentDate = newStartDate;

                    currSegmentDuration = locationDataContainer.getSysTimestamp() - startSegmentDate;
                    if((segmNextIdxAccel == -1) && (currSegmentDuration > values.TIME_BEFORE_OVERLAP)){
                        segmNextIdxAccel = accelerations.size();
                        segmNextIdxLoc = locations.size();
                    }
                    if(currSegmentDuration > values.SEGMENT_DURATION) {
                        processSegment();
                        startSegmentDate = locationDataContainer.getSysTimestamp();
                    }
                }
            }
            locations.add(locationDataContainer);
        }

    }


    private void processSegment(){

        Log.d(TAG, "Start segment evalution");
        Log.d(TAG, "Start, num locs = " + this.locations.size());
        Log.d(TAG, "Start, num accels = " + this.accelerations.size());

        if (rawDataDetection.getOutputsMetadata().size() == 37){
            boolean debugVar = true;
        }

        MLAlgorithmInput mlAlgorithmInput = calcNewEntry();

        if(mlAlgorithmInput.getProcessedPoints().getAvgSpeed() == 0.0){

            // TODO: !!! process buffer if len(buffer)>0 and there are no more segments!!! -> call from TripEvaluation?
            if(!ignoreNextZeros && hasSpeedOnBeginning){
                buffer.add(mlAlgorithmInput);
                if (prevSpeedZero && buffer.size() > timeInSegmentsNumber(values.maxLimitWithoutGPS)){
                    ignoreNextZeros = true;
                    for(MLAlgorithmInput input : buffer) {
                        //classification
                        MLInputMetadata mlInputMeta = classifier.evaluateSegment(input);
                        rawDataDetection.insertMLMetadata(mlInputMeta);
                    }
                    buffer.clear();
                }
                prevSpeedZero = true;
            } else {    // must ignore zeros or doesnt has speeds on beginning
                MLInputMetadata mlInputMeta = classifier.evaluateSegment(mlAlgorithmInput);
                rawDataDetection.insertMLMetadata(mlInputMeta);
            }

        }else{
            hasSpeedOnBeginning = true;

            if(prevSpeedZero){
                Double estimatedAvgSpeed = estimateAVGSpeed(lastSegmentPoints, locations);

                if(estimatedAvgSpeed != null){
                    for(MLAlgorithmInput input : buffer){
                        ProcessedPoints processedPoints = input.getProcessedPoints();
                        processedPoints.setAvgSpeed(estimatedAvgSpeed);
                        processedPoints.setMaxSpeed(estimatedAvgSpeed);
                        processedPoints.setMinSpeed(estimatedAvgSpeed);
                        processedPoints.setEstimatedSpeed(1);

                        input.setProcessedPoints(processedPoints);
                    }
                }
                for (MLAlgorithmInput input : buffer){
                    //classification
                    MLInputMetadata mlInputMeta = classifier.evaluateSegment(input);
                    rawDataDetection.insertMLMetadata(mlInputMeta);
                }
                buffer.clear();
            }

            //classification of current input
            MLInputMetadata mlInputMeta = classifier.evaluateSegment(mlAlgorithmInput);
            rawDataDetection.insertMLMetadata(mlInputMeta);

            lastSegmentPoints = new ArrayList<>(locations);
            prevSpeedZero = false;
            ignoreNextZeros = false;
        }

        locations.subList(0, segmNextIdxLoc).clear();
        accelerations.subList(0, segmNextIdxAccel).clear();

        Log.d(TAG, "End, num locs = " + this.locations.size());
        Log.d(TAG, "End, num accels = " + this.accelerations.size());

        segmNextIdxLoc = -1;
        segmNextIdxAccel = -1;

    }

    public void evaluateBuffer(){

        for (MLAlgorithmInput input : buffer){
            //classification
            MLInputMetadata mlInputMeta = classifier.evaluateSegment(input);
            rawDataDetection.insertMLMetadata(mlInputMeta);
        }
        buffer.clear();

    }


    private long getMinTimestamp(){

        int locationSize = locations.size();
        int accelerationsSize = accelerations.size();

        if((locationSize == 0) && (accelerationsSize == 0)) {return -1;}

        if((locationSize == 0) && (accelerationsSize >0)) {return accelerations.get(0).getTimestamp();}

        if((locationSize > 0) && (accelerationsSize ==0)) {return locations.get(0).getSysTimestamp();}

        long firstAccelTS = accelerations.get(0).getTimestamp();
        long firstLocationTS = locations.get(0).getSysTimestamp();

//        Log.d(TAG, "Min timestamp (loc,accel) = " + firstAccelTS + " , " + firstLocationTS);

        if(firstAccelTS > firstLocationTS){
            return firstLocationTS;
        }else{
            return firstAccelTS;
        }

    }


    private MLAlgorithmInput calcNewEntry(){

        //ArrayList<AccelerationData> accels = new ArrayList<>();
        double sumAccels = 0;
        int numAccelsBelowFilter = 0;
        double sumFilterAccels = 0;

        ArrayList<Double> accels = new ArrayList<>();

        ArrayList<LocationDataContainer> locationsToEvaluate = new ArrayList<>(locations);

//        firstPointIsLastBeforeOverlap = False
        boolean firstPointIsLastBeforeOverlap = false;
        if(lastPrevPoint != null && startSegmentDate - lastPrevPoint.getSysTimestamp() < values.SEGMENT_DURATION){
            locationsToEvaluate.add(0, lastPrevPoint);
            firstPointIsLastBeforeOverlap = true;
        }


        for(AccelerationData accelValue : accelerations){
            double currentAccel = magnitude(accelValue.getxValue(), accelValue.getyValue(), accelValue.getzValue());
            sumAccels += currentAccel;

            if(currentAccel < values.ACCELERATION_FILTER){
                numAccelsBelowFilter += 1;
            }else{
                sumFilterAccels += currentAccel;
            }

            accels.add(currentAccel);
        }

        filterLocationsTriangulation(locationsToEvaluate);

        ProcessedAccelerations processedAccelerations = processAccelerations(accels, sumAccels);

        double accelsBelowFilter = 0;
        double avgFilteredAccels = 0;

        if(accels.size() > 0){
            avgFilteredAccels = sumFilterAccels/accels.size();

            if(numAccelsBelowFilter > 0){
                accelsBelowFilter = (double) numAccelsBelowFilter /accels.size();
            }

        }

        long overlapLimit = startSegmentDate + values.TIME_BEFORE_OVERLAP;

        ProcessedPoints processedPoints = processPoints(locationsToEvaluate, startSegmentDate, overlapLimit, firstPointIsLastBeforeOverlap);

        MLAlgorithmInput toBeReturned = new MLAlgorithmInput(accelsBelowFilter, avgFilteredAccels, processedAccelerations, processedPoints, startSegmentDate);
        return toBeReturned;
    }

    private void filterLocationsTriangulation(ArrayList<LocationDataContainer> locationsToEvaluate) {

        // TODO: FILTERING

        Log.d(TAG,"filteringLocationsTriangulation");
        Log.d(TAG,"num locs to evaluate: " + locationsToEvaluate.size());


        if (locationsToEvaluate.size () >= 3){
            for (int i = 1; i< locationsToEvaluate.size() -1 ; i++){

                double alpha=2;

                double prevLat, prevLong, currLat, currLong, nextLat, nextLong;
                long prevTime, currTime, nextTime;
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


                Log.d(TAG,"FILTER: prev: " + locationsToEvaluate.get(i-1).getLatLng().toString() + ", prev: " + locationsToEvaluate.get(i-1).getLatLng().toString() + " ,Next: " + locationsToEvaluate.get(i+1).getLatLng().toString());
                Log.d(TAG,"FILTER: distance: prevNext: " + prevNext + ", prevCurr: " + prevCurr + " ,currNext: " + currNext);


                if (prevNext * alpha < prevCurr + currNext)
                {

                    Log.d(TAG,"FILTER: CHECK distance: prevNext: " + prevNext + ", prevCurr: " + prevCurr + " ,currNext: " + currNext);


                    if ((nextTime-prevTime)==0){
                        Log.d(TAG,"FILTER: skip filtering prevTime too close to nexTime");
                    } else {

                        //TODO FILTER: media aritmetica nao tinha emconta o tempo de intervalo entre as samples e podia estar a linearizar velocidades
                        //locationsToEvaluate.get(i).setLatitude((prevLat + nextLat)/2);
                        //locationsToEvaluate.get(i).setLongitude((prevLong + nextLong)/2);

                        newLat = prevLat + (nextLat - prevLat) * ((currTime - prevTime) / (nextTime - prevTime));
                        newLong = prevLong + (nextLong - prevLong) * ((currTime - prevTime) / (nextTime - prevTime));

                        Log.d(TAG,"FILTER: CORRECTED: sample " + i + "/" + locationsToEvaluate.size() +" currLat: " + currLat +", currLong: " + currLong + ", CORRLat:" + newLat + ", CORRLong: " + newLong);

                        locationsToEvaluate.get(i).setLatitude(newLat);
                        locationsToEvaluate.get(i).setLongitude(newLong);

                    }

                }
            }
        }

    }


    private static ProcessedAccelerations processAccelerations(ArrayList<Double> accelerationData, double sumAccels){

        ProcessedAccelerations processedAccelerations = new ProcessedAccelerations();

        if(accelerationData.size() == 0){

            processedAccelerations.setAvgAccel(0);
            processedAccelerations.setMaxAccel(0);
            processedAccelerations.setMinAccel(0);
            processedAccelerations.setStdDevAccel(0);
            processedAccelerations.setBetween_03_06(0);
            processedAccelerations.setBetween_06_1(0);
            processedAccelerations.setBetween_1_3(0);
            processedAccelerations.setBetween_3_6(0);
            processedAccelerations.setAbove_6(0);

        }else{
            double between_03_06_num = 0;
            double between_06_1_num = 0;
            double between_1_3_num = 0;
            double between_3_6_num = 0;
            double above_6_num = 0;

            double avgAccel = sumAccels / accelerationData.size();
            double sumDeviationsAccels = 0;
            double maxAccel = 0;
            double minAccel = 10000;

            for(double ad : accelerationData){

                if(ad > maxAccel){
                    maxAccel  = ad;
                }

                if(ad < minAccel){
                    minAccel = ad;
                }

                if (ad >= 0.3 && ad < 0.6){
                    between_03_06_num += 1;
                }
                else if (ad >= 0.6 && ad < 1){
                    between_06_1_num += 1;
                }
                else if (ad >= 1 && ad < 3){
                    between_1_3_num += 1;
                }
                else if (ad >= 3 && ad < 6){
                    between_3_6_num += 1;
                }
                else if (ad >= 6){
                    above_6_num += 1;
                }

                double currDiffWithMean = ad - avgAccel;
                sumDeviationsAccels += currDiffWithMean * currDiffWithMean;

            }

            double varianceAccel = sumDeviationsAccels / accelerationData.size();
            double stdDevAccel = Math.sqrt(varianceAccel);

            double between_03_06 = between_03_06_num / ((double) accelerationData.size());
            double between_06_1 = between_06_1_num / ((double) accelerationData.size());
            double between_1_3 = between_1_3_num / ((double) accelerationData.size());
            double between_3_6 = between_3_6_num / ((double) accelerationData.size());
            double above_6 = above_6_num / ((double) accelerationData.size());

            processedAccelerations.setAvgAccel(avgAccel);
            processedAccelerations.setMaxAccel(maxAccel);
            processedAccelerations.setMinAccel(minAccel);
            processedAccelerations.setStdDevAccel(stdDevAccel);
            processedAccelerations.setBetween_03_06(between_03_06);
            processedAccelerations.setBetween_06_1(between_06_1);
            processedAccelerations.setBetween_1_3(between_1_3);
            processedAccelerations.setBetween_3_6(between_3_6);
            processedAccelerations.setAbove_6(above_6);
        }

        return processedAccelerations;

    }

    private ProcessedPoints processPoints(ArrayList<LocationDataContainer> locationDataContainers, long startSegmentDate, long overlapLimit, boolean firstPointIsLastBeforeOverlap){
        lastPrevPoint = null;

        if(locationDataContainers.size() == 0){
            ProcessedPoints processedPoints = new ProcessedPoints(0,0,0,0,0,0,0,0,0,0);
            return processedPoints;

        }else if(locationDataContainers.size() == 1){

            double currAcc = locationDataContainers.get(0).getAccuracy();

            // check if first point is lastPointBeforeOverlap
            long ts = locationDataContainers.get(0).getSysTimestamp();
            if((!firstPointIsLastBeforeOverlap) && (startSegmentDate <= ts) && (ts <= overlapLimit)){
                lastPrevPoint = locationDataContainers.get(0);
            }

            ProcessedPoints processedPoints = new ProcessedPoints(0,0,0,0,currAcc,currAcc,currAcc,0,0,0);
            return processedPoints;

        }else{

            LocationDataContainer firstPoint = locationDataContainers.get(0);

            long firstTime = firstPoint.getSysTimestamp();
            long prevTime = firstPoint.getSysTimestamp();
            double prevLat = firstPoint.getLatitude();
            double prevLon = firstPoint.getLongitude();
            double prevAcc = firstPoint.getAccuracy();

            double accSum = 0;
            double timeDiffSum = 0;
            double distSum = 0;
            long lastTime = -1;
            ArrayList<Double> speeds = new ArrayList<>();

            // check if first point is lastPointBeforeOverlap
            long ts = locationDataContainers.get(0).getSysTimestamp();
            if((!firstPointIsLastBeforeOverlap) && (startSegmentDate <= ts) && (ts <= overlapLimit)){
                lastPrevPoint = locationDataContainers.get(0);
            }

            for(int i=1; i<locationDataContainers.size(); i++){

                LocationDataContainer currPoint = locationDataContainers.get(i);

                long currTime = currPoint.getSysTimestamp();
                double currLat = currPoint.getLatitude();
                double currLon = currPoint.getLongitude();
                double currAcc = currPoint.getAccuracy();

                if((startSegmentDate <= currTime) && (currTime <= overlapLimit)){
                    lastPrevPoint = currPoint;
                }

                accSum += currAcc;
                lastTime = currTime;
                double timeDiff = (double)(currTime - prevTime) / (1000.0*60.0*60.0);   // delta hours

                if(timeDiff <= 0){

                    if(currAcc < prevAcc){
                        prevLat = currLat;
                        prevLon = currLon;
                        prevAcc = currAcc;
                    }

                    continue;

                }

                double distSimpl = simpleDistance(prevLat, prevLon, currLat, currLon);
                timeDiffSum += timeDiff;
                distSum += distSimpl;
                double currSpeed = distSimpl / timeDiff;
                speeds.add(currSpeed);


                prevTime = currTime;
                prevLat = currLat;
                prevLon = currLon;
                prevAcc = currAcc;

            }

            ///////////////////////////////////////////////////// start accel processing

            double avgAcc = accSum / locationDataContainers.size();
            double maxAcc = 0;
            double minAcc = 100000;
            double sumDeviationAcc = 0;

            for(LocationDataContainer point : locationDataContainers){

                double currAcc = point.getAccuracy();

                if(currAcc > maxAcc){
                    maxAcc = currAcc;
                }

                if(currAcc < minAcc){
                    minAcc = currAcc;
                }

                double diffWithMeanAcc = currAcc - avgAcc;
                sumDeviationAcc += diffWithMeanAcc * diffWithMeanAcc;

            }

            double varianceAcc = sumDeviationAcc / locationDataContainers.size();

            double stdDevAcc = Math.sqrt(varianceAcc);

            ///////////////////////////////////////////////////// start speed processing

            double avgSpeed, maxSpeed, minSpeed, stdDevSpeed;
            if (speeds.size() > 0){
                double totalTime = (double)(lastTime - firstTime) / (1000.0 * 60.0 * 60.0);
                avgSpeed = distSum / totalTime;

                maxSpeed = 0.0;
                minSpeed = 100000.0;
                double sumDeviationSpeed = 0.0;

                for(double speed : speeds){

                    if(speed > maxSpeed){
                        maxSpeed = speed;
                    }

                    if(speed < minSpeed){
                        minSpeed = speed;
                    }
                    double diffWithMeanSpeed = speed - avgSpeed;
                    sumDeviationSpeed += diffWithMeanSpeed * diffWithMeanSpeed;
                }
                double varianceSpeed = sumDeviationSpeed / (double) speeds.size();
                stdDevSpeed = Math.sqrt(varianceSpeed);
            } else {
                avgSpeed = 0.0;
                maxSpeed = 0.0;
                minSpeed = 0.0;
                stdDevSpeed = 0.0;
            }



            ///////////////////////////////////////////////// START OTHERS PROCESSING
            double gpsTimeMean;
            if (speeds.size() > 0){
                gpsTimeMean = timeDiffSum / speeds.size();
            } else{
                gpsTimeMean = 0;
            }

            double distance = distSum * 1000;

            return new ProcessedPoints(avgSpeed,maxSpeed,minSpeed,stdDevSpeed,avgAcc,maxAcc,minAcc,stdDevAcc,gpsTimeMean,distance);

        }


    }

    private static double magnitude(double x, double y, double z){

        return Math.sqrt(x*x + y*y + z*z);

    }

    public static double simpleDistance(double prevLat, double prevLon, double currLat, double currLon){

        double earthRadioInKM = 6371;
        double dLat = degreesToRadians(currLat - prevLat);
        double dLon = degreesToRadians(currLon - prevLon);

        double latAux = degreesToRadians(prevLat + currLat);

        double dx = dLon * Math.cos(0.5 * latAux);
        double dy = dLat;

        return earthRadioInKM * Math.sqrt(dx*dx + dy*dy);

    }

    private static double degreesToRadians(double degrees){

        return degrees * 0.0174532925199433; //pi/180

    }

    public static int timeInSegmentsNumber(int seconds){

        return (seconds * 1000 - values.OVERLAP) / (values.SEGMENT_DURATION - values.OVERLAP);
    }

    private static ArrayList<LocationDataContainer> orderPointsByAccuracy(ArrayList<LocationDataContainer> segmPoints, boolean isLastSegm){
        HashMap<Double, ArrayList<Integer>> indexesPerAcc = new HashMap<>();

        if (isLastSegm){
            for (int i = segmPoints.size()-1; i>-1; i--){
                Double accKey = (double) segmPoints.get(i).getAccuracy();
                if (indexesPerAcc.containsKey(accKey)){
                    indexesPerAcc.get(accKey).add(i);
                } else {
                    ArrayList<Integer> newList = new ArrayList<>();
                    newList.add(i);
                    indexesPerAcc.put(accKey, newList);
                }
            }
        } else {
            for (int i = 0; i<segmPoints.size(); i++){
                Double accKey = (double) segmPoints.get(i).getAccuracy();
                if (indexesPerAcc.containsKey(accKey)){
                    indexesPerAcc.get(accKey).add(i);
                } else {
                    ArrayList<Integer> newList = new ArrayList<>();
                    newList.add(i);
                    indexesPerAcc.put(accKey, newList);
                }
            }
        }

        ArrayList<Double> orderedKeys = new ArrayList<> (indexesPerAcc.keySet());
         Collections.sort(orderedKeys, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        });

        ArrayList<LocationDataContainer> orderedPoints = new ArrayList<>();
        for (Double key :orderedKeys){
            for (Integer idx : indexesPerAcc.get(key)){
                orderedPoints.add(segmPoints.get(idx.intValue()));
            }
        }
        return orderedPoints;
    }

    private static Double estimateAVGSpeed(ArrayList<LocationDataContainer> lastSegmentPoints, ArrayList<LocationDataContainer> nextSegmPoints){

        ArrayList<LocationDataContainer> orderedLastSegmentPoints = orderPointsByAccuracy(lastSegmentPoints, true);
        ArrayList<LocationDataContainer> orderedNextSegmentPoints = orderPointsByAccuracy(nextSegmPoints, false);

        if((orderedLastSegmentPoints.size() > 0) && (orderedNextSegmentPoints.size() > 0)){

            int lastSegmIdx = -1;
            int nextSegmIdx = -1;

            if(orderedLastSegmentPoints.get(0).getSysTimestamp() != orderedNextSegmentPoints.get(0).getSysTimestamp()){

                lastSegmIdx = 0;
                nextSegmIdx = 0;

            }else{

                if((orderedLastSegmentPoints.size() == 1) && (orderedNextSegmentPoints.size() == 1)){
                    return null;
                }

                if((orderedLastSegmentPoints.size() > 1) && (orderedNextSegmentPoints.size() == 1)){
                    lastSegmIdx = 1;
                    nextSegmIdx = 0;
                }

                if((orderedLastSegmentPoints.size() == 1) && (orderedNextSegmentPoints.size() > 1)){
                    lastSegmIdx = 0;
                    nextSegmIdx = 1;
                }

                if((orderedLastSegmentPoints.size() > 1) && (orderedNextSegmentPoints.size() > 1)){

                    if(orderedLastSegmentPoints.get(1).getAccuracy() < orderedNextSegmentPoints.get(1).getAccuracy()){
                        lastSegmIdx = 1;
                        nextSegmIdx = 0;
                    }else{
                        lastSegmIdx = 0;
                        nextSegmIdx = 1;
                    }

                }

            }

            LocationDataContainer left = orderedLastSegmentPoints.get(lastSegmIdx);
            LocationDataContainer right = orderedNextSegmentPoints.get(nextSegmIdx);

            double timeDiff = Math.abs((double)((right.getSysTimestamp()) - (left.getSysTimestamp())))/(1000.0*60.0*60.0);
            double distSimpl = simpleDistance(left.getLatitude(), left.getLongitude(), right.getLatitude(), right.getLongitude());
            double estimatedSpeed = distSimpl/timeDiff;

            return estimatedSpeed;


        }else{
           return null;
        }

    }




    public interface values{

        int SEGMENT_DURATION = 90*1000;    // 90 segs
        int OVERLAP = 45*1000;              // 45 segs
        int TIME_BEFORE_OVERLAP = SEGMENT_DURATION - OVERLAP;
        double ACCELERATION_FILTER = 0.3;
        int maxLimitWithoutGPS = 30*60;
    }



}
