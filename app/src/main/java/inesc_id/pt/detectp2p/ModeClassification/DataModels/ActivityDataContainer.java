package inesc_id.pt.detectp2p.ModeClassification.DataModels;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 7/11/17.
 */

public class ActivityDataContainer implements Serializable {

    @Expose
    long timestamp;

    @Expose
    ArrayList<ActivityDetected> listOfDetectedActivities;

    public ActivityDataContainer() {
    }

    public ActivityDataContainer(long timestamp, ActivityRecognitionResult result) {
        this.timestamp = timestamp;
        //this.result = result;

        List<DetectedActivity> detectedActivityArrayList =result.getProbableActivities();

        listOfDetectedActivities = new ArrayList<>();

        for(DetectedActivity a : detectedActivityArrayList){
            listOfDetectedActivities.add(new ActivityDetected(a.getType(),a.getConfidence()));
        }

    }

    public ActivityDataContainer(long timestamp, ArrayList<ActivityDetected> result) {
        this.timestamp = timestamp;
        //this.result = result;
        this.listOfDetectedActivities = result;

    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<ActivityDetected> getListOfDetectedActivities() {
        return listOfDetectedActivities;
    }

    public void setListOfDetectedActivities(ArrayList<ActivityDetected> listOfDetectedActivities) {
        this.listOfDetectedActivities = listOfDetectedActivities;
    }

}
