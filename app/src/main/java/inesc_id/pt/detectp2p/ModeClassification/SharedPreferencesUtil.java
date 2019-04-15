package inesc_id.pt.detectp2p.ModeClassification;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import inesc_id.pt.detectp2p.ModeClassification.dataML.LocationDataContainer;

/**
 * Created by Duarte on 16/02/2018.
 */

public class SharedPreferencesUtil {

    //Context context;

    //constants
    public static final String SHARED_PREFERENCES_FILE = "inesc_id.pt.detectp2p.SF_FILE";

    public static final String SHARED_PREFERENCES_VAR_CURRENT = "inesc_id.pt.detectp2p.var.currentToBeCompared";
    public static final String SHARED_PREFERENCES_VAR_TRIPSTATE = "inesc_id.pt.detectp2p.var.tripstate";
    public static final String SHARED_PREFERENCES_FCMTOKEN = "inesc_id.pt.detectp2p.var.fcmToken";

    public static final String SHARED_PREFERENCES_CURRENT_GLOBAL_TIMESTAMP_ON_DEVICE = "inesc_id.pt.detectp2p.var.currentGlobalTimestampOnDevice";

    public static final String SHARED_PREFERENCES_ONBOARDING_DATA = "inesc_id.pt.detectp2p.onboardingDataOnDevice";

    public static final String SHARED_PREFERENCES_LOGGED_TIMESTAMP = "inesc_id.pt.detectp2p.loggedTimestamp";

    public static final String SHARED_PREFERENCES_LAST_SEARCHED_ROUTE = "inesc_id.pt.detectp2p.lastSearchedRoute";


    public static LocationDataContainer readCurrentToBeCompared(Context context, String defaultValue){
        Gson gson = new Gson();
        if(keyExists(context,SHARED_PREFERENCES_VAR_CURRENT)) {
            return gson.fromJson(getPersistentString(context,SHARED_PREFERENCES_VAR_CURRENT, defaultValue),LocationDataContainer.class);
        }else{
            return null;
        }
    }

    public static void writeCurrentToBeCompared(Context context, LocationDataContainer locationDataContainer){

        Gson gson = new Gson();
        setPersistentString(context,SHARED_PREFERENCES_VAR_CURRENT,gson.toJson(locationDataContainer));
    }

    public static void deleteCurrentToBeCompared(Context context){
        deleteKey(context,SHARED_PREFERENCES_VAR_CURRENT);
    }

    public static int readSavedTripState(Context context, int defaultValue){
        // I assume that if the key does not exist, defaultValue is returned
        return getPersistentInt(context,SHARED_PREFERENCES_VAR_TRIPSTATE,defaultValue);
    }

    public static void writeTripState(Context context, int state){

        setPersistentInt(context,SHARED_PREFERENCES_VAR_TRIPSTATE,state);
    }

    public static void deleteTripState(Context context){
       deleteKey(context,SHARED_PREFERENCES_VAR_TRIPSTATE);
    }

    //methods to read and write to the shared preferences file defined in variable SHARED_PREFERENCES_FILE
    private static String getPersistentString(Context context, String id, String defaultValue) {
        return getPersistentPrefs(context).getString(id,defaultValue);
    }

    private static void setPersistentString(Context context, String id, String value) {
        SharedPreferences.Editor editor = getPersistentEditor(context);
        editor.putString(id, value);
        editor.commit();
    }

    private static int getPersistentInt(Context context, String id, int defaultValue) {
        return getPersistentPrefs(context).getInt(id,defaultValue);
    }

    private static void setPersistentInt(Context context, String id, int value) {
        SharedPreferences.Editor editor = getPersistentEditor(context);
        editor.putInt(id, value);
        editor.commit();
    }

    private static long getPersistentLong(Context context, String id, long defaultValue) {
        return getPersistentPrefs(context).getLong(id,defaultValue);
    }

    private static void setPersistentLong(Context context, String id, long value) {
        SharedPreferences.Editor editor = getPersistentEditor(context);
        editor.putLong(id, value);
        editor.commit();
    }

    private static SharedPreferences.Editor getPersistentEditor(Context context){
        SharedPreferences sharedPreferences = getPersistentPrefs(context);
        return sharedPreferences.edit();
    }

    private static SharedPreferences getPersistentPrefs(Context context){
        return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public static boolean keyExists(Context context, String key) {
        return getPersistentPrefs(context).contains(key);
    }

    private static void deleteKey(Context context, String key){
        SharedPreferences.Editor editor = getPersistentEditor(context);
        editor.remove(key);
        editor.commit();
    }

    public static int readCurrentGlobalTimestampOnDevice(Context context){
        return getPersistentInt(context,SHARED_PREFERENCES_CURRENT_GLOBAL_TIMESTAMP_ON_DEVICE,-1);
    }

    public static void writeCurrentGlobalTimestampOnDevice(Context context, int timestamp){
        setPersistentInt(context,SHARED_PREFERENCES_CURRENT_GLOBAL_TIMESTAMP_ON_DEVICE,timestamp);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long readCurrentLoggedTimestamp(Context context, long defaultValue){
        return getPersistentLong(context,SHARED_PREFERENCES_LOGGED_TIMESTAMP,defaultValue);
    }

    public static void writeCurrentLoggedTimestamp(Context context, long timestamp){
        setPersistentLong(context,SHARED_PREFERENCES_LOGGED_TIMESTAMP, timestamp);
    }
}
