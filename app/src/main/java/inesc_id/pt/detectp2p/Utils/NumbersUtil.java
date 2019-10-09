package inesc_id.pt.detectp2p.Utils;

import java.util.ArrayList;
import java.util.Iterator;

import inesc_id.pt.detectp2p.TripDetection.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.TripDetection.dataML.LocationDataContainer;

/**
 * Created by admin on 1/5/18.
 */

public class NumbersUtil {

    public static int indexOfMaxInRange(int[] a) {
        int max = 0;
        int maxIndex = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static float getSegmentSpeedKm(double distanceBetweenMeters, double timeBetweenMiliseconds){

        double speed = (distanceBetweenMeters/1000.0)/(timeBetweenMiliseconds/3600000.0);

        if(!Double.isNaN(speed) && !Double.isInfinite(speed)) {
            return (float) ((distanceBetweenMeters/1000.0)/(timeBetweenMiliseconds/3600000.0));
        }else{
            return 0;
        }
    }

    public static boolean areIntsConsecutive(ArrayList<Integer> list) {
        Iterator<Integer> it = list.iterator();
        if (!it.hasNext()) {
            return true;
        }

        Integer prev = it.next();
        while (it.hasNext()) {
            Integer curr = it.next();
            if (prev + 1 != curr /* mismatch */ || prev + 1 < prev /* overflow */) {
                return false;
            }
            prev = curr;
        }
        return true;
    }


    public static long getLastTimestamp(ArrayList<AccelerationData> accelerations, ArrayList<LocationDataContainer> locations){

        if(accelerations.size() == 0){
            return locations.get(locations.size()-1).getSysTimestamp();
        }

        if(locations.size() == 0){
            return accelerations.get(accelerations.size()-1).getTimestamp();
        }

        AccelerationData lastAccel = accelerations.get(accelerations.size()-1);
        LocationDataContainer lastLocation = locations.get(locations.size()-1);

        if(lastAccel.getTimestamp() > lastLocation.getSysTimestamp()) {
            return lastAccel.getTimestamp();
        }
        return lastLocation.getSysTimestamp();

    }



}
