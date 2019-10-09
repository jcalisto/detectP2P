package inesc_id.pt.detectp2p.TripValidationManager;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import inesc_id.pt.detectp2p.TripDetection.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.TripDetection.dataML.Trip;
import inesc_id.pt.detectp2p.Utils.ValidatedTripStorage;

public class ValidatedDataManager {

    public static ValidatedDataManager instance;

    private static final String TAG = "ValidatedDataManager";

    //UserID -> [Trips]
    private ArrayList<ValidatedTrip> userValidations;

    private ArrayList<ValidatedTrip> localValidations;


    public static synchronized  ValidatedDataManager getInstance(){
        if (instance == null){ //if there is no instance available... create new one
            instance = new ValidatedDataManager();
        }

        return instance;
    }

    private ValidatedDataManager(){
        userValidations = new ArrayList<>();
        localValidations = ValidatedTripStorage.readLocalValidations();
        userValidations = ValidatedTripStorage.readAllValidations();
        Log.d(TAG, "Read local validations count: " + localValidations.size());
    }

    public synchronized  void addUserValidation(String userId, ArrayList<ValidatedTrip> trips) {

        ArrayList<ValidatedTrip> tripsToAddToUser = userValidations;
        boolean newValidations = false;

        for(ValidatedTrip trip : trips){
            if(checkIfTripArrayHasTrip(trip.tripID, tripsToAddToUser)) continue;
            newValidations = true;
            userValidations.add(trip);
        }

        if(newValidations){
            ValidatedTripStorage.writeExternalValidationsToStorage(userValidations);
        }
    }

    private boolean checkIfTripArrayHasTrip(String tripID, ArrayList<ValidatedTrip> trips){
        for(ValidatedTrip trip : trips){
            if(tripID == trip.tripID) return true;
        }
        return false;
    }

    public void addLocalValidation(Trip trip, int correctedMode){
        String tripId = generateTripId(trip);
        Log.d("ValidatedDataManager", "Adding validation for tripID = " + tripId);

        ValidatedTrip validatedTrip = new  ValidatedTrip(tripId, trip.getSugestedModeOfTransport(), correctedMode, trip.getLocationDataContainers());

        if(checkIfTripArrayHasTrip(tripId, localValidations)) return;

        localValidations.add(validatedTrip);
        userValidations.add(validatedTrip);
        ValidatedTripStorage.writeLocalValidationsToStorage(localValidations);
        ValidatedTripStorage.writeExternalValidationsToStorage(userValidations);
    }

    public String generateTripId(Trip trip){
        String id = "trip_";
        String info = "" + trip.getInitTimestamp() + trip.getEndTimestamp() + trip.getAccelerationAverage() + trip.getDistanceTraveled();
        return id + info.hashCode();
    }

    public Float[][] getLocalValidationMatrix(){

        Float[][] matrix = new Float[16][16];

        Float[] walkingPredictions = new Float[16];
        Float[] bicyclePredictions = new Float[16];
        Float[] carPredictions  = new Float[16];
        Float[] busPredictions  = new Float[16];
        Float[] trainPredictions  = new Float[16];

        /////////////////////////// POPULATE CONFUSION MATRIX WITH 0s //////////////////////////////
        walkingPredictions[1] = bicyclePredictions[1] = carPredictions[1] = busPredictions[1] = trainPredictions[1] = 0.0f;
        walkingPredictions[7] = bicyclePredictions[7] = carPredictions[7] = busPredictions[7] = trainPredictions[7] = 0.0f;
        walkingPredictions[9] = bicyclePredictions[9] = carPredictions[9] = busPredictions[9] = trainPredictions[9] = 0.0f;
        walkingPredictions[10] = bicyclePredictions[10] = carPredictions[10] = busPredictions[10] = trainPredictions[10] = 0.0f;
        walkingPredictions[15] = bicyclePredictions[15] = carPredictions[15] = busPredictions[15] = trainPredictions[15] = 0.0f;

        float walkingCount = 0;
        float bicycleCount = 0;
        float carCount = 0;
        float busCount = 0;
        float trainCount = 0;

        for(ValidatedTrip trip : localValidations){
            switch(trip.classifierMode){
                case 1: //bicycle
                    bicyclePredictions[trip.realMode] += 1;
                    bicycleCount += 1;
                    break;
                case 7: //walking
                    walkingPredictions[trip.realMode] += 1;
                    walkingCount += 1;
                    break;
                case 9: //car
                    carPredictions[trip.realMode] += 1;
                    carCount += 1;
                    break;
                case 10: //train
                    trainPredictions[trip.realMode] += 1;
                    trainCount += 1;
                    break;
                case 15: //bus
                    busPredictions[trip.realMode] += 1;
                    busCount += 1;
                    break;
            }
        }

        walkingPredictions[0] = walkingCount;
        bicyclePredictions[0] = bicycleCount;
        carPredictions[0] = carCount;
        trainPredictions[0] = trainCount;
        busPredictions[0] = busCount;

        matrix[1] = bicyclePredictions;
        matrix[7] = walkingPredictions;
        matrix[9] = carPredictions;
        matrix[10] = trainPredictions;
        matrix[15] = busPredictions;

        return matrix;
    }

    public int getValidatedModeForTrip(Trip trip){
        String tripId = generateTripId(trip);
        for(ValidatedTrip t : localValidations){
            if(t.tripID.equals(tripId)){
                return t.realMode;
            }
        }
        return 0;
    }

    public ValidatedTrip getValidatedTrip(String tripId){
        for(ValidatedTrip t : localValidations){
            if(t.tripID.equals(tripId)){
                return t;
            }
        }
        return null;
    }

    public ArrayList<ValidatedTrip> getUserValidations() {
        return userValidations;
    }

    public void setUserValidations(ArrayList<ValidatedTrip> userValidations) {
        this.userValidations = userValidations;
    }

    public ArrayList<ValidatedTrip> getLocalValidations() {
        return localValidations;
    }

    public void setLocalValidations(ArrayList<ValidatedTrip> localValidations) {
        this.localValidations = localValidations;
    }
    public int compareTrips(Trip trip, ValidatedTrip vTrip){
        ArrayList<LocationDataContainer> tripCoords = trip.getLocationDataContainers();
        ArrayList<ValidatedCoords> vTripCoords = vTrip.coords;

        int commonCoords = 0;
        int totalCoords = 0;

        for(LocationDataContainer location : tripCoords){
            totalCoords++;
            for(ValidatedCoords coord : vTripCoords){
                if(compareCoords(location.getLatitude(), location.getLongitude(), coord.latitude, coord.longitude)){
                    commonCoords++;
                }
            }
        }

        Log.d("Comprare TRIPS", "totalCoords=" + totalCoords + ", commonCoords=" + commonCoords);

        return commonCoords;
    }

    public boolean compareCoords(double tripLatitude, double tripLongitude, double vLatitude, double vLongitude){
        double dist = distance(tripLatitude, tripLongitude, vLatitude, vLongitude);

        if(dist < 50){
            return true;
        }
        return false;
    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {

        double earthRadius = 6371; // in miles, change to 6371 for kilometer output

        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double dist = earthRadius * c;

        return dist * 1000; // output distance, in KM
    }
}
