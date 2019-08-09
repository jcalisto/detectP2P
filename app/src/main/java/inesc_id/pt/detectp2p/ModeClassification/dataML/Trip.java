package inesc_id.pt.detectp2p.ModeClassification.dataML;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.nio.DoubleBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import inesc_id.pt.detectp2p.ModeClassification.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.ModeClassification.DataModels.ActivityDataContainer;
import inesc_id.pt.detectp2p.ModeClassification.DataModels.ActivityDetected;
import inesc_id.pt.detectp2p.Utils.DateHelper;

/**
 * Created by admin on 1/2/18.
 */

public class Trip extends FullTripPart implements Serializable{

    @SerializedName("activityList")
    @Expose
    ArrayList<ActivityDataContainer> activityDataContainers;

    @SerializedName("modeOfTransport")
    @Expose
    int modality;

    @SerializedName("distance")
    @Expose
    long distanceTraveled;

    @SerializedName("duration")
    @Expose
    long timeTraveled;

    @SerializedName("avSpeed")
    @Expose
    float averageSpeed;

    @SerializedName("mSpeed")
    @Expose
    float maxSpeed;

    @SerializedName("accelerationMean")
    @Expose
    double accelerationAverage;

    @SerializedName("accelerations")
    @Expose
    ArrayList<AccelerationData> accelerationData;

    @SerializedName("wrongLeg")
    @Expose
    boolean isWrongLeg;

    @SerializedName("correctedModeOfTransport")
    @Expose
    int correctedModeOfTransport;

    @SerializedName("filteredAcceleration")
    @Expose
    double filteredAcceleration;

    @SerializedName("filteredAccelerationBelowThreshold")
    @Expose
    double filteredAccelerationBelowThreshold;

    @SerializedName("filteredSpeed")
    @Expose
    double filteredSpeed;

    @SerializedName("filteredSpeedBelowThreshold")
    @Expose
    double filteredSpeedBelowThreshold;

    @SerializedName("accuracyMean")
    @Expose
    float accuracyMean;

    @SerializedName("detectedModeOfTransport")
    @Expose
    int suggestedModeOfTransport;


    //    todo uncomment for new trip structure
    @Expose
    private String otherMotText;

    @Expose
    private HashMap<Integer, Double> probasDict;

    public Trip(ArrayList<LocationDataContainer> locationDataContainers, ArrayList<ActivityDataContainer> activityDataContainers,
                int modality, long initTimestamp, long endTimestamp, long distanceTraveled, long timeTraveled, float averageSpeed, float maxSpeed,
                double accelerationAverage, ArrayList<AccelerationData> accelerationData,
                double filteredAcceleration, double filteredAccelerationBelowThreshold, double filteredSpeed, double filteredSpeedBelowThreshold,
                float accuracyMean, int suggestedModeOfTransport, int correctedModeOfTransport) {

        super(locationDataContainers, initTimestamp, endTimestamp);

        this.activityDataContainers = activityDataContainers;
        this.modality = modality;
        this.distanceTraveled = distanceTraveled;
        this.timeTraveled = timeTraveled;
        this.averageSpeed = averageSpeed;
        this.maxSpeed = maxSpeed;
        this.accelerationAverage = accelerationAverage;
        this.accelerationData = accelerationData;
        this.isWrongLeg = false;
        this.filteredAcceleration = filteredAcceleration;
        this.filteredAccelerationBelowThreshold = filteredAccelerationBelowThreshold;
        this.filteredSpeed = filteredSpeed;
        this.filteredSpeedBelowThreshold = filteredSpeedBelowThreshold;
        this.accuracyMean = accuracyMean;
        //defaultValue
        this.correctedModeOfTransport = correctedModeOfTransport;
        this.suggestedModeOfTransport = suggestedModeOfTransport;
        this.activityDataContainers = new ArrayList<>();
    }

    //copy constructor
    public Trip(Trip trip) {
        super(trip.locationDataContainers, trip.initTimestamp, trip.endTimestamp);
        this.activityDataContainers = trip.activityDataContainers;
        this.modality = trip.modality;
        this.distanceTraveled = trip.distanceTraveled;
        this.timeTraveled = trip.timeTraveled;
        this.averageSpeed = trip.averageSpeed;
        this.maxSpeed = trip.maxSpeed;
        this.accelerationAverage = trip.accelerationAverage;
        this.accelerationData = trip.accelerationData;
        this.isWrongLeg = trip.isWrongLeg;
        this.correctedModeOfTransport = trip.correctedModeOfTransport;
        this.filteredAcceleration = trip.filteredAcceleration;
        this.filteredAccelerationBelowThreshold = trip.filteredAccelerationBelowThreshold;
        this.filteredSpeed = trip.filteredSpeed;
        this.filteredSpeedBelowThreshold = trip.filteredSpeedBelowThreshold;
        this.accuracyMean = trip.accuracyMean;
        this.suggestedModeOfTransport = trip.suggestedModeOfTransport;
    }

    public Trip(ArrayList<LocationDataContainer> locationDataContainers, long initTimestamp, long endTimestamp) {
        super(locationDataContainers, initTimestamp, endTimestamp);
    }


    public Trip(ArrayList<LocationDataContainer> locationDataContainers, ArrayList<AccelerationData> accelerationData,
                long initTimestamp, long endTimestamp, int identifiedModality,
                float averageSpeed, float maxSpeed, long distance
    ){

        super(locationDataContainers, initTimestamp, endTimestamp);

        this.suggestedModeOfTransport = identifiedModality;
        this.modality = identifiedModality;
        this.correctedModeOfTransport = -1;

        this.accelerationData = accelerationData;

        this.distanceTraveled = distance;
        this.maxSpeed = maxSpeed;
        this.averageSpeed = averageSpeed;

    }

    public int getModality() {
        return modality;
    }

    public void setModality(int modality) {
        this.modality = modality;
    }

    public long getInitTimestamp() {
        return initTimestamp;
    }

    public void setInitTimestamp(long initTimestamp) {
        this.initTimestamp = initTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public void setProbasDict(HashMap<Integer, Double> probasDict){ this.probasDict = probasDict;}
    public HashMap<Integer, Double> getProbasDict(){
        return probasDict;
    }

    @Override
    public boolean isTrip() {
        return true;
    }

    @Override
    public String getDescription() {

        StringBuilder sb = new StringBuilder();
        sb.append("-----------------------"+"\n");
        sb.append("--------Leg---------" +"\n");

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a");
        String initDate = sdf.format(this.getInitTimestamp());
        String endDate = sdf.format(this.getEndTimestamp());

        sb.append("Start Date: "+initDate +"\n");
        sb.append("End Date: "+endDate +"\n");
        sb.append("Distance traveled: "+this.getDistanceTraveled() + "m" +"\n");
        sb.append("Time traveled: " + DateHelper.getHMSfromMS(this.getTimeTraveled()) +"\n");
        sb.append("Average speed: "+ this.getAverageSpeed() + "km/h" +"\n");
        sb.append("Maximum speed: "+ this.getMaxSpeed() + "km/h" +"\n");
        sb.append("Mode of transport: " + ActivityDetected.keys.modalities[modality] +"\n");
        if(this.getCorrectedModeOfTransport() != -1) {
            sb.append("Corrected mode of transport: " + ActivityDetected.keys.modalities[getCorrectedModeOfTransport()] + "\n");
        }
        sb.append("Average acceleration: "+ this.getAccelerationAverage() + " m/s2" +"\n");
        sb.append("Average accuracy: "+ this.getAccuracyMean() + " m" +"\n");
        sb.append("Filtered acceleration: "+ this.getFilteredAcceleration() + " m/s2"+"\n");
        sb.append("Acceleration below threshold percentage: "+ this.getFilteredAccelerationBelowThreshold() * 100 + "%"+"\n");
        sb.append("Filtered speed: "+ this.getFilteredSpeed() + " m/s"+"\n");
        sb.append("Speed below threshold percentage: "+ this.getFilteredSpeedBelowThreshold() * 100 + "%"+"\n");

        return sb.toString();

    }

    @Override
    public int getFullTripPartType() {
        if (correctedModeOfTransport == -1){
            return keys.NOT_VALIDATED_LEG;
        }
        return keys.VALIDATED_LEG;
    }

    public long getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setDistanceTraveled(long distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }

    public long getTimeTraveled() {
        return timeTraveled;
    }

    public void setTimeTraveled(long timeTraveled) {
        this.timeTraveled = timeTraveled;
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(float averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public ArrayList<LocationDataContainer> getLocationDataContainers() {
        return locationDataContainers;
    }

    public void setLocationDataContainers(ArrayList<LocationDataContainer> locationDataContainers) {
        this.locationDataContainers = locationDataContainers;
    }

    public ArrayList<ActivityDataContainer> getActivityDataContainers() {
        return activityDataContainers;
    }

    public void setActivityDataContainers(ArrayList<ActivityDataContainer> activityDataContainers) {
        this.activityDataContainers = activityDataContainers;
    }

    public double getAccelerationAverage() {
        return accelerationAverage;
    }

    public void setAccelerationAverage(double accelerationAverage) {
        this.accelerationAverage = accelerationAverage;
    }

    public ArrayList<AccelerationData> getAccelerationData() {
        return accelerationData;
    }

    public void setAccelerationData(ArrayList<AccelerationData> accelerationData) {
        this.accelerationData = accelerationData;
    }

    public boolean isWrongLeg() {
        return isWrongLeg;
    }

    public void setWrongLeg(boolean wrongLeg) {
        isWrongLeg = wrongLeg;
    }

    public int getCorrectedModeOfTransport() {
        return correctedModeOfTransport;
    }

    public void setCorrectedModeOfTransport(int correctedModeOfTransport) {
        this.correctedModeOfTransport = correctedModeOfTransport;
    }

    public double getFilteredAcceleration() {
        return filteredAcceleration;
    }

    public void setFilteredAcceleration(double filteredAcceleration) {
        this.filteredAcceleration = filteredAcceleration;
    }

    public double getFilteredAccelerationBelowThreshold() {
        return filteredAccelerationBelowThreshold;
    }

    public void setFilteredAccelerationBelowThreshold(double filteredAccelerationBelowThreshold) {
        this.filteredAccelerationBelowThreshold = filteredAccelerationBelowThreshold;
    }

    public double getFilteredSpeed() {
        return filteredSpeed;
    }

    public void setFilteredSpeed(double filteredSpeed) {
        this.filteredSpeed = filteredSpeed;
    }

    public double getFilteredSpeedBelowThreshold() {
        return filteredSpeedBelowThreshold;
    }

    public void setFilteredSpeedBelowThreshold(double filteredSpeedBelowThreshold) {
        this.filteredSpeedBelowThreshold = filteredSpeedBelowThreshold;
    }

    public float getAccuracyMean() {
        return accuracyMean;
    }

    public void setAccuracyMean(float accuracyMean) {
        this.accuracyMean = accuracyMean;
    }

    public int getSugestedModeOfTransport() {
        return suggestedModeOfTransport;
    }

    public void setSugestedModeOfTransport(int suggestedModeOfTransport) {
        this.suggestedModeOfTransport = suggestedModeOfTransport;
    }



}