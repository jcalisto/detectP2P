package inesc_id.pt.detectp2p.TripStateMachine.dataML;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by admin on 1/2/18.
 */

public class LocationDataContainer implements Serializable {

    @Expose
    @SerializedName("timestamp")
    long sysTimestamp;

    @SerializedName("acc")
    @Expose
    float accuracy;

    @SerializedName("lat")
    @Expose
    double latitude;

    @SerializedName("lon")
    @Expose
    double longitude;

    @Expose(serialize = false)
    float speed;

    @Expose(serialize = false)
    long locTimestamp;

    public LocationDataContainer(long sysTimestamp, float accuracy, double latitude, double longitude, float speed, long locTimestamp) {
        this.sysTimestamp = sysTimestamp;
        this.accuracy = accuracy;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.locTimestamp = locTimestamp;
    }

    public LocationDataContainer() {
    }

    public LocationDataContainer(LatLng latLng){

        this.sysTimestamp = 0;
        this.accuracy = 0;
        this.latitude = latLng.latitude;
        this.longitude = latLng.latitude;
        this.speed = 0;
        this.locTimestamp = 0;

    }

    public long getSysTimestamp() {
        return sysTimestamp;
    }

    public void setSysTimestamp(long sysTimestamp) {
        this.sysTimestamp = sysTimestamp;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getLocTimestamp() {
        return locTimestamp;
    }

    public void setLocTimestamp(long locTimestamp) {
        this.locTimestamp = locTimestamp;
    }

    public LatLng getLatLng(){
        return new LatLng(getLatitude(), getLongitude());
    }

}
