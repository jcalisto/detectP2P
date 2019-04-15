package inesc_id.pt.detectp2p.ModeClassification.dataML;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.Utils.DateHelper;

/**
 * Created by admin on 1/4/18.
 */

public class FullTrip implements Serializable{

    @SerializedName("trips")
    @Expose
    private ArrayList<FullTripPart> tripList;

    @SerializedName("startDate")
    @Expose
    private long initTimestamp;

    @SerializedName("endDate")
    @Expose
    private long endTimestamp;

    @SerializedName("distance")
    @Expose
    private long distanceTraveled;

    @SerializedName("duration")
    @Expose
    private long timeTraveled;

    @SerializedName("avSpeed")
    @Expose
    private float averageSpeed;

    @SerializedName("mSpeed")
    @Expose
    private float maxSpeed;

    @SerializedName("model")
    @Expose
    private String smartphoneModel;

    @SerializedName("oS")
    @Expose
    private String operatingSystem;

    @SerializedName("oSVersion")
    @Expose
    private String oSVersion;

    @SerializedName("userID")
    @Expose
    private String userID;

    @SerializedName("startAddress")
    @Expose
    private String startAddress;

    @SerializedName("finalAddress")
    @Expose
    private String finalAddress;


    @Expose
    private boolean manualTripStart = false;

    @Expose
    private boolean manualTripEnd = false;


    public FullTrip(ArrayList<FullTripPart> tripList, long initTimestamp, long endTimestamp, long distanceTraveled,
                    long timeTraveled, float averageSpeed, float maxSpeed, String smartphoneModel, String operatingSystem, String oSVersion, String userID) {
        this.tripList = tripList;
        this.initTimestamp = initTimestamp;
        this.endTimestamp = endTimestamp;
        this.distanceTraveled = distanceTraveled;
        this.timeTraveled = timeTraveled;
        this.averageSpeed = averageSpeed;
        this.maxSpeed = maxSpeed;
        this.smartphoneModel = smartphoneModel;
        this.operatingSystem = operatingSystem;
        this.oSVersion = oSVersion;
        this.userID = userID;
    }


    public FullTrip() {
    }





    public String getDescription(){

        StringBuilder sb = new StringBuilder();
        sb.append("Trip"+"\n");

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a");
        String initDate = sdf.format(this.getInitTimestamp());
        String endDate = sdf.format(this.getEndTimestamp());

        sb.append("Start Date: "+initDate +"\n");
        sb.append("End Date: "+endDate +"\n");
        sb.append("Distance traveled: "+this.getDistanceTraveled()+ "m" +"\n");
        sb.append("Time traveled: " + DateHelper.getHMSfromMS(this.getTimeTraveled()) +"\n");
        sb.append("Average speed: "+ this.getAverageSpeed() + "km/h"+"\n");
        sb.append("Maximum speed: "+ this.getMaxSpeed() + "km/h"+"\n");

        for(FullTripPart ftp: tripList){

            sb.append(ftp.getDescription());

        }

        return sb.toString();
    }

    public String toString(){

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a");
        String initDate = sdf.format(this.getInitTimestamp());

        return initDate;
    }

    public String getDateId(){
//        return DateHelper.getDateFromTSString(getTripList().get(0).getInitTimestamp());
          return initTimestamp+"";
    }

    public ArrayList<Trip> getAllLegs(){

        ArrayList<Trip> legList = new ArrayList<>();

        for(FullTripPart fullTripPart : tripList){
            if(fullTripPart.isTrip()) legList.add((Trip)fullTripPart);
        }

        return legList;
    }


    public LocationDataContainer getDeparturePlace(){

        Log.e("s", "size legs" + getAllLegs().size());

        for (FullTripPart ftp : getAllLegs()){

            Log.e("l", "lcds" + ftp.getLocationDataContainers().size());

        }

        return getAllLegs().get(0).getLocationDataContainers().get(0);
    }

    public LocationDataContainer getArrivalPlace(){
        return getAllLegs().get(getAllLegs().size()-1).getLocationDataContainers().get(getAllLegs().get(getAllLegs().size()-1).getLocationDataContainers().size()-1);
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }
    public float getMaxSpeed() {
        return maxSpeed;
    }

    public long getDistanceTraveled() {
        return distanceTraveled;
    }

    public long getInitTimestamp() {
        return initTimestamp;
    }
    public long getEndTimestamp() {
        return endTimestamp;
    }
    public long getTimeTraveled() { return timeTraveled; }

    public ArrayList<FullTripPart> getTripList() {
        return tripList;
    }

    public FullTripDigest getFullTripDigest(){
        return new FullTripDigest(initTimestamp, endTimestamp, userID, startAddress, finalAddress, getDeparturePlace(), getArrivalPlace(), getDateId());
    }






}