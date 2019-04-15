package inesc_id.pt.detectp2p.ModeClassification;

import android.content.Context;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.DataModels.ActivityDataContainer;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripPart;
import inesc_id.pt.detectp2p.ModeClassification.dataML.LocationDataContainer;

/**
 * Created by Duarte on 20/02/2018.
 */

public class TSMSnapshotHelper {

    //from sharedPreferences - saved state and current to be compared location

    PersistentTripStorage persistentTripStorage;
    Context context;

    public TSMSnapshotHelper(Context context){

        persistentTripStorage = new PersistentTripStorage(context);
        this.context = context;
    }

    //default value must be != null (int)
    public int getSavedState(int defaultValue){
        return SharedPreferencesUtil.readSavedTripState(context,defaultValue);
    }

    public LocationDataContainer getSavedCurrentToBeCompared(String defaultValue){
        return SharedPreferencesUtil.readCurrentToBeCompared(context,defaultValue);
    }

    public boolean existsSavedState(){
        return SharedPreferencesUtil.keyExists(context,SharedPreferencesUtil.SHARED_PREFERENCES_VAR_TRIPSTATE);
    }

    public boolean existsSavedCurrentToBeCompared(){
        return SharedPreferencesUtil.keyExists(context,SharedPreferencesUtil.SHARED_PREFERENCES_VAR_CURRENT);
    }

    public void saveState(int state){
        SharedPreferencesUtil.writeTripState(context,state);
    }

    public void saveCurrentToBeCompared(LocationDataContainer locationDataContainer){
        SharedPreferencesUtil.writeCurrentToBeCompared(context,locationDataContainer);
    }

    public void deleteState(){
        SharedPreferencesUtil.deleteTripState(context);
    }

    public void deleteCurrentToBeCompared(){
        SharedPreferencesUtil.deleteCurrentToBeCompared(context);
    }

    //from sqlite database - saved locations, activities and full trip parts

    //read methods
    public ArrayList<LocationDataContainer> getSavedLocations(){
        return persistentTripStorage.getAllLocationObjects();
    }

    public ArrayList<ActivityDataContainer> getSavedActivities(){
        return persistentTripStorage.getAllActivityObjects();
    }

    public ArrayList<FullTripPart> getSavedFullTripParts(){
        return persistentTripStorage.getAllSavedFullTripPartObjects();
    }
//
    public ArrayList<AccelerationData> getSavedAccelerationValues(){
        return persistentTripStorage.getAllSavedAccelerationObjects();
    }
//
    //write methods
    public void saveLocation(LocationDataContainer locationDataContainer){
        persistentTripStorage.insertLocationObject(locationDataContainer);
    }
//
    public void saveActivity(ActivityDataContainer activityDataContainer){
        persistentTripStorage.insertActivityObject(activityDataContainer);
    }
//
    public void saveFullTripPart(FullTripPart fullTripPart){
        persistentTripStorage.insertTripPart(fullTripPart);
    }
//
    public void saveFullTripParts(ArrayList<FullTripPart> fullTripPart){
        persistentTripStorage.insertTripPartList(fullTripPart);
    }
//
    public void saveAccelerationValues(ArrayList<AccelerationData> accelerationDataArrayList){
        persistentTripStorage.insertAccelerationListObjects(accelerationDataArrayList);
    }
//
    //delete methods
    public void deleteSavedLocations(){
        persistentTripStorage.dropSavedLocations();
    }

    public void deleteSavedFullTripParts(){
        persistentTripStorage.dropAllSavedFullTripParts();
    }

    public void deleteSavedAccelerationValues(){
        persistentTripStorage.dropAllSavedAccelerationObjects();
    }

    public void deleteMLInputMetadata(){
        persistentTripStorage.dropAllMLInputObjects();
    }

    public void deleteAllSnapshotRecords(){
        deleteSavedLocations();
        deleteSavedFullTripParts();
        deleteState();
        deleteCurrentToBeCompared();
        deleteSavedAccelerationValues();
        deleteMLInputMetadata();
    }


}
