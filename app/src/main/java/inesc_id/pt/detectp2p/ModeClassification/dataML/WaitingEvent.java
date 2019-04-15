package inesc_id.pt.detectp2p.ModeClassification.dataML;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by admin on 1/4/18.
 */

public class WaitingEvent extends FullTripPart implements Serializable{

    @SerializedName("avLocationLat")
    @Expose
    double averageLocationLatitude;

    @SerializedName("avLocationLon")
    @Expose
    double averageLocationLongitude;


    public WaitingEvent(ArrayList<LocationDataContainer> locationDataContainers, long initTimestamp, long endTimestamp, double averageLocationLatitude, double averageLocationLongitude) {
        super(locationDataContainers, initTimestamp, endTimestamp);
        this.averageLocationLatitude = averageLocationLatitude;
        this.averageLocationLongitude = averageLocationLongitude;
    }

    public double getAverageLocationLatitude() {
        return averageLocationLatitude;
    }

    public void setAverageLocationLatitude(long averageLocationLatitude) {
        this.averageLocationLatitude = averageLocationLatitude;
    }

    public double getAverageLocationLongitude() {
        return averageLocationLongitude;
    }

    public void setAverageLocationLongitude(double averageLocationLongitude) {
        this.averageLocationLongitude = averageLocationLongitude;
    }

    @Override
    public boolean isTrip() {
        return false;
    }

    @Override
    public String getDescription() {

        StringBuilder sb = new StringBuilder();
        sb.append("---------------------"+"\n");
        sb.append("----Waiting Event----" +"\n");

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a");
        String initDate = sdf.format(this.getInitTimestamp());
        String endDate = sdf.format(this.getEndTimestamp());

        sb.append("Start Date: "+initDate + "\n");
        sb.append("End Date: "+endDate +"\n");
        sb.append("Average location:"+"\n");
        sb.append("- Latitude: "+this.getAverageLocationLatitude() +"\n");
        sb.append("- Longitude: "+this.getAverageLocationLongitude() +"\n");



        return sb.toString();
    }

    @Override
    public int getFullTripPartType() {
        return keys.WAITING_EVENT;
    }


}
