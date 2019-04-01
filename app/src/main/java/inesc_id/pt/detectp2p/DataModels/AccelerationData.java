package inesc_id.pt.detectp2p.DataModels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Duarte on 13/03/2018.
 */

public class AccelerationData implements Serializable{

    @Expose
    @SerializedName("xvalue")
    double xValue;
    @Expose
    @SerializedName("yvalue")
    double yValue;
    @Expose
    @SerializedName("zvalue")
    double zValue;
    @Expose
    @SerializedName("timestamp")
    long timestamp;

    public AccelerationData(double xValue, double yValue, double zValue, long timestamp) {
        this.xValue = xValue;
        this.yValue = yValue;
        this.zValue = zValue;
        this.timestamp = timestamp;
    }

    public AccelerationData() {
    }

    public double getxValue() {
        return xValue;
    }

    public void setxValue(double xValue) {
        this.xValue = xValue;
    }

    public double getyValue() {
        return yValue;
    }

    public void setyValue(double yValue) {
        this.yValue = yValue;
    }

    public double getzValue() {
        return zValue;
    }

    public void setzValue(double zValue) {
        this.zValue = zValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
