package inesc_id.pt.detectp2p.TripDetection.dataML;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class FullTripDigest implements Serializable{



    public FullTripDigest(long initTimestamp, long endTimestamp, String userID, String startAddress, String finalAddress, LocationDataContainer startLocation, LocationDataContainer finalLocation, String tripID) {
        this.initTimestamp = initTimestamp;
        this.endTimestamp = endTimestamp;
        this.userID = userID;
        this.startAddress = startAddress;
        this.finalAddress = finalAddress;
        this.startLocation = startLocation;
        this.finalLocation = finalLocation;

        this.tripID = tripID;
    }

    @SerializedName("startDate")
    @Expose
    private long initTimestamp;

    @SerializedName("endDate")
    @Expose
    private long endTimestamp;

    @SerializedName("userID")
    @Expose
    private String userID;

    @SerializedName("startAddress")
    @Expose
    private String startAddress;

    @SerializedName("finalAddress")
    @Expose
    private String finalAddress;

    @SerializedName("startLocation")
    @Expose
    private LocationDataContainer startLocation;

    @SerializedName("finalLocation")
    @Expose
    private LocationDataContainer finalLocation;

    @Expose
    private String tripID;

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

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }


    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getFinalAddress() {
        return finalAddress;
    }

    public void setFinalAddress(String finalAddress) {
        this.finalAddress = finalAddress;
    }

    public LocationDataContainer getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(LocationDataContainer startLocation) {
        this.startLocation = startLocation;
    }

    public LocationDataContainer getFinalLocation() {
        return finalLocation;
    }

    public void setFinalLocation(LocationDataContainer finalLocation) {
        this.finalLocation = finalLocation;
    }

    public String getTripID() {
        return tripID;
    }

    public void setTripID(String tripID) {
        this.tripID = tripID;
    }

}
