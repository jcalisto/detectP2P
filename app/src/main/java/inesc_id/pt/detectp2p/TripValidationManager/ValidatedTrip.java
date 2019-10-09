package inesc_id.pt.detectp2p.TripValidationManager;

import java.io.Serializable;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.TripDetection.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.TripDetection.RawDataPreProcessing;
import inesc_id.pt.detectp2p.TripDetection.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.TripDetection.dataML.ProcessedAccelerations;

public class ValidatedTrip implements Serializable {

    public String tripID;
    public int classifierMode;
    public int realMode;
    public ArrayList<ValidatedCoords> coords;



    public ValidatedTrip(String tripID, int classifierMode, int realMode, ArrayList<LocationDataContainer> locationDataContainers){
        this.tripID = tripID;
        this.classifierMode = classifierMode;
        this.realMode = realMode;
        this.coords = new ArrayList<>();
        for(LocationDataContainer locContainer : locationDataContainers){
            this.coords.add(new ValidatedCoords(locContainer.getLatitude(), locContainer.getLongitude(), locContainer.getSpeed()));
        }
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


    }

    private double magnitude(double x, double y, double z){

        return Math.sqrt(x*x + y*y + z*z);

    }

}
