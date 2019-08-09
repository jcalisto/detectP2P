package inesc_id.pt.detectp2p.ValidatedData;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.ModeClassification.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.ModeClassification.RawDataPreProcessing;
import inesc_id.pt.detectp2p.ModeClassification.dataML.ProcessedAccelerations;

public class ValidatedTrip implements Serializable {

    public String tripID;
    public int classifierMode;
    public int realMode;
    public double accelsBelowFilter;
    public double accelBetw_03_06;
    public double accelBetw_06_1;
    public double accelBetw_1_3;
    public double accelBetw_3_6;
    public double accelAbove_6;
    public double avgAccel;
    public double maxAccel;
    public double minAccel;
    public double stdDevAccel;



    public ValidatedTrip(String tripID, int classifierMode, int realMode, ArrayList<AccelerationData> accelerationData){
        this.tripID = tripID;
        this.classifierMode = classifierMode;
        this.realMode = realMode;
        calculateTripParameters(accelerationData);

    }

    //Default constructor
    ValidatedTrip(){}

    private void calculateTripParameters(ArrayList<AccelerationData> accelerationData){
        double sumAccels = 0;
        int numAccelsBelowFilter = 0;
        double sumFilterAccels = 0;

        ArrayList<Double> accels = new ArrayList<>();
        for(AccelerationData accelValue : accelerationData){
            double currentAccel = magnitude(accelValue.getxValue(), accelValue.getyValue(), accelValue.getzValue());
            sumAccels += currentAccel;

            if(currentAccel < RawDataPreProcessing.values.ACCELERATION_FILTER){
                numAccelsBelowFilter += 1;
            }else{
                sumFilterAccels += currentAccel;
            }

            accels.add(currentAccel);
        }


        ProcessedAccelerations processedAccelerations = RawDataPreProcessing.processAccelerations(accels, sumAccels);
        this.accelsBelowFilter = numAccelsBelowFilter;
        this.accelBetw_03_06 = processedAccelerations.getBetween_03_06();
        this.accelBetw_06_1 = processedAccelerations.getBetween_06_1();
        this.accelBetw_1_3 = processedAccelerations.getBetween_1_3();
        this.accelBetw_3_6 = processedAccelerations.getBetween_3_6();
        this.accelAbove_6 = processedAccelerations.getAbove_6();
        this.avgAccel = processedAccelerations.getAvgAccel();
        this.maxAccel = processedAccelerations.getMaxAccel();
        this.minAccel = processedAccelerations.getMinAccel();
        this.stdDevAccel = processedAccelerations.getStdDevAccel();

    }

    private double magnitude(double x, double y, double z){

        return Math.sqrt(x*x + y*y + z*z);

    }

}
