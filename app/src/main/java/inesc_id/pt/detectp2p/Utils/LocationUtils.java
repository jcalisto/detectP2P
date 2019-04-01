package inesc_id.pt.detectp2p.Utils;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import inesc_id.pt.detectp2p.TripStateMachine.dataML.LocationDataContainer;
/**
 * Created by admin on 1/2/18.
 */

public class LocationUtils {

    public static double meterDistanceBetweenPoints(double lat_a, double lng_a, double lat_b, double lng_b) {
        double pk = (double) (180.f/Math.PI);

        double a1 = lat_a / pk;
        double a2 = lng_a / pk;
        double b1 = lat_b / pk;
        double b2 = lng_b / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        double result = 6366000 * tt;

        if(Double.isNaN(result)){
            return 0;
        }else{
            return result;
        }
    }

    public static double meterDistanceBetweenTwoLocations(Location locA, Location locB) {

        return meterDistanceBetweenPoints(locA.getLatitude(),locA.getLongitude(), locB.getLatitude(), locB.getLongitude());

    }

    public static double meterDistanceBetweenTwoLocations(LocationDataContainer locA, LocationDataContainer locB) {

        return meterDistanceBetweenPoints(locA.getLatitude(),locA.getLongitude(), locB.getLatitude(), locB.getLongitude());

    }

    private static double meterDistanceBetweenTwoLocations(LatLng latLng, LocationDataContainer locationDataContainer){

        return meterDistanceBetweenPoints(latLng.latitude,latLng.longitude, locationDataContainer.getLatitude(), locationDataContainer.getLongitude());

    }

    /**
     * Based on `distanceToLine` method from
     * https://github.com/googlemaps/android-maps-utils/blob/master/library/src/com/google/maps/android/PolyUtil.java
     */
    private static LatLng findNearestPoint(final LatLng p, final LatLng start, final LatLng end) {
        if (start.equals(end)) {
            return start;
        }

        final double s0lat = Math.toRadians(p.latitude);
        final double s0lng = Math.toRadians(p.longitude);
        final double s1lat = Math.toRadians(start.latitude);
        final double s1lng = Math.toRadians(start.longitude);
        final double s2lat = Math.toRadians(end.latitude);
        final double s2lng = Math.toRadians(end.longitude);

        double s2s1lat = s2lat - s1lat;
        double s2s1lng = s2lng - s1lng;
        final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
        if (u <= 0) {
            return start;
        }
        if (u >= 1) {
            return end;
        }

        return new LatLng(start.latitude + (u * (end.latitude - start.latitude)),
                start.longitude + (u * (end.longitude - start.longitude)));


    }

    public static LocationDataContainer findNearestLDC(LatLng latLng, List<LocationDataContainer> locationDataContainerList){

        LocationDataContainer nearestLCD = null;
        double minDistance=999999999;

        for(LocationDataContainer lcd : locationDataContainerList){
            double currentDistance = LocationUtils.meterDistanceBetweenTwoLocations(latLng, lcd);

            if(currentDistance < minDistance){
                minDistance = currentDistance;
                nearestLCD = lcd;
            }
        }
        return nearestLCD;
    }

    public static ArrayList<LocationDataContainer> getLatLngArrayFromLDCArray(List<LatLng> latLngList){

        ArrayList<LocationDataContainer> result = new ArrayList<>();

        for (LatLng latLng : latLngList){
            result.add(new LocationDataContainer(latLng));
        }
        return result;
    }

    public static String getTextLatLng(LocationDataContainer locationDataContainer){

        return locationDataContainer.getLatitude() + ", " + locationDataContainer.getLongitude();
    }

    public static LatLng extrapolateMiddleLocation(List<LocationDataContainer> pathLCD) {
        LatLng extrapolated = null;

        ArrayList<LatLng> path = new ArrayList<>();

        LatLng origin = pathLCD.get(0).getLatLng();



        for (LocationDataContainer lcd : pathLCD){
            Log.e("extrapolate", lcd.getLatLng().toString());

            path.add(lcd.getLatLng());
        }

        Double distance = SphericalUtil.computeLength(path) / 2;
//        Log.e("extrapolate", "distance: " + distance);

//        if (!PolyUtil.isLocationOnPath(origin, path, false, 1)) { // If the location is not on path non geodesic, 1 meter tolerance
//            return null;
//        }

        float accDistance = 0f;
        boolean foundStart = false;
        List<LatLng> segment = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {
            LatLng segmentStart = path.get(i);
            LatLng segmentEnd = path.get(i + 1);

            segment.clear();
            segment.add(segmentStart);
            segment.add(segmentEnd);

            double currentDistance = 0d;

            if (!foundStart) {
                if (PolyUtil.isLocationOnPath(origin, segment, false, 1)) {
                    foundStart = true;

                    currentDistance = SphericalUtil.computeDistanceBetween(origin, segmentEnd);

                    if (currentDistance > distance) {
                        double heading = SphericalUtil.computeHeading(origin, segmentEnd);
                        extrapolated = SphericalUtil.computeOffset(origin, distance - accDistance, heading);
                        break;
                    }
                }
            } else {
                currentDistance = SphericalUtil.computeDistanceBetween(segmentStart, segmentEnd);

                if (currentDistance + accDistance > distance) {
                    double heading = SphericalUtil.computeHeading(segmentStart, segmentEnd);
                    extrapolated = SphericalUtil.computeOffset(segmentStart, distance - accDistance, heading);
                    break;
                }
            }

            accDistance += currentDistance;
        }

        return extrapolated;
    }


}
