package inesc_id.pt.detectp2p.ModeClassification.dataML;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by admin on 1/4/18.
 */

public abstract class FullTripPart implements Serializable{

    public FullTripPart(ArrayList<LocationDataContainer> locationDataContainers, long initTimestamp, long endTimestamp) {
        this.locationDataContainers = locationDataContainers;
        this.initTimestamp = initTimestamp;
        this.endTimestamp = endTimestamp;

    }


    @SerializedName("locations")
    @Expose
    ArrayList<LocationDataContainer> locationDataContainers;

    @SerializedName("startDate")
    @Expose()
    long initTimestamp;

    @SerializedName("endDate")
    @Expose()
    long endTimestamp;


    public ArrayList<LocationDataContainer> getLocationDataContainers() {
        return locationDataContainers;
    }

    public void setLocationDataContainers(ArrayList<LocationDataContainer> locationDataContainers) {
        this.locationDataContainers = locationDataContainers;
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


//    public void setLegActivities(ArrayList<ActivityLeg> legActivities) {
//        this.legActivities = legActivities;
//    }

    public abstract boolean isTrip();

    public abstract String getDescription();

    public abstract int getFullTripPartType();




    public interface keys{
        int VALIDATED_LEG = 0;
        int NOT_VALIDATED_LEG = 1;
        int WAITING_EVENT = 2;
    }

}
