package inesc_id.pt.detectp2p.TripStateMachine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import org.joda.time.DateTime;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.DataModels.AccelerationData;
import inesc_id.pt.detectp2p.DataModels.ActivityDataContainer;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.FullTrip;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.FullTripDigest;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.FullTripPart;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.LocationDataContainer;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.MLInputMetadata;
import inesc_id.pt.detectp2p.Utils.DateHelper;
import inesc_id.pt.detectp2p.Utils.JSONUtils;
import inesc_id.pt.detectp2p.Utils.ToStringSample;

import static org.joda.time.DateTimeZone.UTC;

/**
 * Created by admin on 1/4/18.
 */



public class PersistentTripStorage {

    private final static String TAG = "PersistentStorage";

    private TripStorageDBHelper tripStorageDBHelper;

    Context context;


    public PersistentTripStorage(Context context) {
        tripStorageDBHelper = new TripStorageDBHelper(context, true);
        this.context = context;
    }


    /*
    public ArrayList<FullTrip> getAllFullTripsObject() {

        //see https://stackoverflow.com/questions/10723770/whats-the-best-way-to-iterate-an-android-cursor

        ArrayList<FullTrip> resultSet = new ArrayList<>();

        try(Cursor cursor = tripStorageDBHelper.getAllFullTrips()) {
            while (cursor.moveToNext()) {


                try {
                    Gson gson = JSONUtils.getInstance().getGson();
                    FullTrip result = gson.fromJson(cursor.getString(1), FullTrip.class);
                    resultSet.add(result);
                }catch(Exception e){
                    Crashlytics.log(Log.DEBUG,"Persistence",e.getMessage());

                }

            }
        }catch (Exception e){
            Crashlytics.log(Log.DEBUG,"Persistence resource",e.getMessage());

        }

        tripStorageDBHelper.close();

        return  resultSet;
    }

    */



        /*
    public ArrayList<String> getAllFullTripsDates() {

        //see https://stackoverflow.com/questions/10723770/whats-the-best-way-to-iterate-an-android-cursor

        //final Cursor cursor = tripStorageDBHelper.getFullTripDates();

        ArrayList<String> resultSet = new ArrayList<>();

        try(Cursor cursor = tripStorageDBHelper.getFullTripDates()) {
            while (cursor.moveToNext()) {

                String result = cursor.getString(0);
                resultSet.add(result);



            }
        }


        return  resultSet;
    }
    */

    public FullTrip getFullTripByDate(String date) {

        FullTrip resultSet = null;

        //read from json
        long currentTS = new DateTime(UTC).getMillis();
        Log.d("Persistence", "Started reading at: " + currentTS );
        resultSet = readFullTripFromDiskWithJSON(date);
        Log.d("Persistence", "Stopped reading - Time Elapsed= " + (new DateTime(UTC).getMillis() - currentTS));



        return  resultSet;
    }
//
//    public boolean deleteFullTripByDate(String tripID){
//
//        tripStorageDBHelper.deleteFullTripData(tripID);
//        tripStorageDBHelper.deleteFullTripDigestData(tripID);
//
//        return true;
//
//    }

//    public boolean updateFullTripDataObject(FullTrip fullTrip, String date){
//
////        Gson gson = JSONUtils.getInstance().getGson();
////        String fullTripInJson = gson.toJson(fullTrip, FullTrip.class);
//
////        tripStorageDBHelper.updateFullTripData(fullTripInJson, date);
//
////        Log.e("Persistence","initaddress" +  fullTrip.getFullTripDigest().getStartAddress());
////        Log.e("Persistence","finaladdress" +  fullTrip.getFullTripDigest().getFinalAddress());
////
////        Log.e("Persistence", fullTrip.getDateId());
////        Log.d("Persistence", fullTrip.getFullTripDigest().getTripID());
//
////        updateFullTripDigestDataObject(fullTripDigest, fullTripDigest.getTripID());
//
//
//        //write object in json to disk
//        writeFullTripToDiskWithJSON(fullTrip);
//
//        Gson gson = JSONUtils.getInstance().getGson();
//        String fullTripDigestInJson = gson.toJson(fullTrip.getFullTripDigest(), FullTripDigest.class);
//
//        tripStorageDBHelper.updateFullTripDigestData(fullTripDigestInJson, date);
//
//
//        //write object directly to disk
////        writeFullTripToDisk(fullTrip);
////
////        Gson gson = JSONUtils.getInstance().getGson();
////        String fullTripDigestInJson = gson.toJson(fullTrip.getFullTripDigest(), FullTripDigest.class);
////
////        tripStorageDBHelper.updateFullTripDigestData(fullTripDigestInJson, date);
//
//        return true;
//    }
//


    public boolean insertFullTripObject(FullTrip fullTrip) {


        long currentTS = new DateTime(UTC).getMillis();
        Log.d("Persistence", "Started writing at: " + currentTS );
        writeFullTripToDiskWithJSON(fullTrip);
        Log.d("Persistence", "Stopped writing - Time Elapsed= " + (new DateTime(UTC).getMillis() - currentTS));

        insertFullTripDigestObject(fullTrip.getFullTripDigest());

        return true;
    }


//    public boolean writeFullTripToDisk(FullTrip fullTrip){
//
////        Gson gson = JSONUtils.getInstance().getGson();
////
////        try (OutputStream fileOut = context.openFileOutput(fullTrip.getDateId(), Context.MODE_PRIVATE);
////             OutputStream bufferedOut = new BufferedOutputStream(fileOut);
////             Writer writer = new OutputStreamWriter(bufferedOut)) {
////             gson.toJson(fullTrip, FullTrip.class, writer);
////
////
////
//////            String s = ToStringSample.objectToString(fullTrip);
//////            writer.write(s);
////
////
////
////             return true;
////
////        } catch (IOException e) {
////            e.printStackTrace();
////            return false;
////        }
//
//        File tripDataDir = new File(context.getFilesDir().getPath() + "/tripData");
//
//        if(!tripDataDir.exists()) {
//            tripDataDir.mkdir();
//        }
//
//        try {
//            String filePath = context.getFilesDir().getPath() + "/tripData/" + fullTrip.getDateId();
//
//            FileOutputStream fos = new FileOutputStream(filePath);
//            ObjectOutputStream oos = new ObjectOutputStream(fos);
//            // write object to file
//            oos.writeObject(fullTrip);
//            System.out.println("Done");
//            // closing resources
//            oos.close();
//            fos.close();
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    public boolean writeFullTripToDiskWithJSON(FullTrip fullTrip){
        try {
            Log.d(TAG, "Writing Trip to Disk with ID= " + fullTrip.getDateId());
            Log.d(TAG, "DESCRIPTION: " +  fullTrip.getDescription());

            Log.d(TAG, "Number of trips: " + fullTrip.getTripList().size());

            Gson gson = JSONUtils.getInstance().getGson();
            String fulltripjson = gson.toJson(fullTrip, FullTrip.class);
            String filePath = context.getFilesDir().getPath() + "/" + fullTrip.getDateId();

            FileOutputStream fos = new FileOutputStream(filePath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // write object to file

            Log.d(TAG, "TRIP AS JSON STRING: " + fulltripjson);
            oos.writeObject(fulltripjson);

            System.out.println("Done");
            // closing resources
            oos.close();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

//    public FullTrip readFullTripFromDisk(String tripID){
//
////        Gson gson = JSONUtils.getInstance().getGson();
////
////        try (InputStream fileIn = context.openFileInput(tripID);
////             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
////             Reader reader = new InputStreamReader(bufferedIn, StandardCharsets.UTF_8)) {
////             FullTrip result = gson.fromJson(reader, FullTrip.class);
////
//////            FullTrip result = (FullTrip) ToStringSample.stringToObject(reader.toString());
//////            resultSet.add(result);
////
////             return result;
////
////        } catch (IOException e) {
////            e.printStackTrace();
////            return null;
////        }
//
//        String filePath = context.getFilesDir().getPath()  + "/tripData/" + tripID;
//
//        FileInputStream is = null;
//        try {
//            is = new FileInputStream(filePath);
//            ObjectInputStream ois = new ObjectInputStream(is);
//            FullTrip emp = (FullTrip) ois.readObject();
//            ois.close();
//            is.close();
//
//            return emp;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//
//    }

    public FullTrip readFullTripFromDiskWithJSON(String tripID){
        Log.d(TAG, "Reading Trip from Disk with ID= " + tripID);

        String filePath = context.getFilesDir().getPath()  + "/" + tripID;

        FileInputStream is = null;
        try {
            is = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(is);
            String emp = (String) ois.readObject();
            ois.close();
            is.close();

            Gson gson = JSONUtils.getInstance().getGson();
            FullTrip fullTrip = gson.fromJson(emp, FullTrip.class);

            return fullTrip;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    public boolean insertLocationObject(LocationDataContainer location) {

        Gson gson = new Gson();
        String json = gson.toJson(location);
        tripStorageDBHelper.insertLocation(json);

        return true;
    }

    public ArrayList<LocationDataContainer> getAllLocationObjects() {

        ArrayList<LocationDataContainer> resultSet = new ArrayList<>();

        Gson gson = new Gson();

        Cursor cursor = tripStorageDBHelper.getAllSavedLocations();
        try {
            while (cursor.moveToNext()) {
                //LocationDataContainer result = (LocationDataContainer) (cursor.getString(1));
                LocationDataContainer result = gson.fromJson((cursor.getString(1)),LocationDataContainer.class);
                resultSet.add(result);
            }
        }catch (Exception e){
            Log.d("PersistentTripStorage", e.getMessage());
        } finally {
            cursor.close();
        }

        return  resultSet;
    }

    public boolean dropSavedLocations() {

        tripStorageDBHelper.dropAllLocationRecords();
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Full trip digest

//    public boolean updateFullTripDigestDataObject(FullTripDigest fullTrip, String date){
//
//        Gson gson = JSONUtils.getInstance().getGson();
//        String fullTripDigestInJson = gson.toJson(fullTrip, FullTripDigest.class);
//
//        tripStorageDBHelper.updateFullTripDigestData(fullTripDigestInJson, date);
//
//        return true;
//    }

    public ArrayList<FullTripDigest> getAllFullTripDigestsObjects() {

        //see https://stackoverflow.com/questions/10723770/whats-the-best-way-to-iterate-an-android-cursor

        ArrayList<FullTripDigest> resultSet = new ArrayList<>();

        Cursor cursor = tripStorageDBHelper.getAllFullTripDigests();
        try {
            while (cursor.moveToNext()) {

                try {
                    Gson gson = JSONUtils.getInstance().getGson();
                    FullTripDigest result = gson.fromJson(cursor.getString(1), FullTripDigest.class);
                    resultSet.add(result);
                }catch(Exception e){
                    Log.e(TAG,e.getMessage());
                }
                /*FullTrip result = (FullTrip) ToStringSample.stringToObject(cursor.getString(1));
                resultSet.add(result);*/

            }
        }catch (Exception e){
            Log.e(TAG,e.getMessage());

        }

        //tripStorageDBHelper.close();

        return  resultSet;
    }

    public boolean insertFullTripDigestObject(FullTripDigest fullTripDigest) {

        Gson gson = JSONUtils.getInstance().getGson();
        String json = gson.toJson(fullTripDigest);

        String userID = fullTripDigest.getUserID();
        Long initTimestamp = fullTripDigest.getInitTimestamp();
        String tripID = initTimestamp + "";

        tripStorageDBHelper.insertFullTripDigest(json, userID, tripID);


        return true;
    }

//    public boolean deleteFullTripDigestObject(String dateID) {
//
//        /*Gson gson = new Gson();
//        String json = gson.toJson(fullTrip);
//
//        tripStorageDBHelper.insertFullTrip(json);*/
//
//        tripStorageDBHelper.deleteFullTripDigestData(dateID);
//
//        /*String rep = ToStringSample.objectToString(fullTrip);
//        tripStorageDBHelper.insertFullTrip(rep);*/
//
//        return true;
//    }

    //////////////////////////////////////////
    ////////////////ML
    //////////////////////////////////////////

    public boolean insertMLInputObject(MLInputMetadata mlInputMetadata) {

        Gson gson = new Gson();
        String json = gson.toJson(mlInputMetadata);
        tripStorageDBHelper.insertMLInputMetadata(json);

        return true;
    }

    public ArrayList<MLInputMetadata> getAllMLInputObjects() {

        ArrayList<MLInputMetadata> resultSet = new ArrayList<>();

        Gson gson = new Gson();

        Cursor cursor = tripStorageDBHelper.getAllMLInputMetadata();
        try{
            while (cursor.moveToNext()) {
                //LocationDataContainer result = (LocationDataContainer) (cursor.getString(1));
                MLInputMetadata result = gson.fromJson((cursor.getString(1)),MLInputMetadata.class);
                resultSet.add(result);
            }
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
        finally {
            cursor.close();
        }

        return  resultSet;
    }

    public boolean dropAllMLInputObjects() {

        tripStorageDBHelper.dropAllMLInputMetadata();
        return true;
    }

    //////////////////////////////////////////
    ////////////////ML
    //////////////////////////////////////////



    public boolean insertActivityObject(ActivityDataContainer activityDataContainer) {

        Gson gson = new Gson();
        String json = gson.toJson(activityDataContainer);
        tripStorageDBHelper.insertActivity(json);

        return true;
    }

    public ArrayList<ActivityDataContainer> getAllActivityObjects() {

        ArrayList<ActivityDataContainer> resultSet = new ArrayList<>();
        Gson gson = new Gson();

        Cursor cursor = tripStorageDBHelper.getAllSavedActivities();
        try{
            while (cursor.moveToNext()) {
                ActivityDataContainer result = gson.fromJson((cursor.getString(1)),ActivityDataContainer.class);
                resultSet.add(result);
            }
        }catch (Exception e){
            Log.d("PersistentTripStorage", e.getMessage());
        } finally {
            cursor.close();
        }

        return  resultSet;
    }

    public boolean dropSavedActivities() {

        tripStorageDBHelper.dropAllActivityRecords();
        return true;
    }

    public boolean insertTripPart(FullTripPart fullTripPart) {

        String rep = ToStringSample.objectToString(fullTripPart);
        tripStorageDBHelper.insertFullTripPart(rep);

        return true;
    }

    public boolean insertTripPartList(ArrayList<FullTripPart> fullTripPartArrayList){

        ArrayList<String> temp = new ArrayList<>();

        for(FullTripPart ftp :fullTripPartArrayList){
            temp.add(ToStringSample.objectToString(ftp));
        }

        tripStorageDBHelper.insertFullTripParts(temp);

        return true;
    }
//
    public ArrayList<FullTripPart> getAllSavedFullTripPartObjects() {

        ArrayList<FullTripPart> resultSet = new ArrayList<>();

        Cursor cursor = tripStorageDBHelper.getAllFullTripParts();
        try {
            while (cursor.moveToNext()) {
                FullTripPart result = (FullTripPart) ToStringSample.stringToObject(cursor.getString(1));
                resultSet.add(result);
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        } finally {
            cursor.close();
        }

        return  resultSet;
    }

    public boolean dropAllSavedFullTripParts() {

        tripStorageDBHelper.dropAllFullTripParts();
        return true;
    }

    ///////////
    //ACCELERATIONS

    public boolean insertAccelerationListObjects(ArrayList<AccelerationData> accelerationsArrayList){

        ArrayList<String> temp = new ArrayList<>();

        Gson gson = JSONUtils.getInstance().getGson();

        for(AccelerationData ad : accelerationsArrayList){

            temp.add(gson.toJson(ad));
        }

        tripStorageDBHelper.insertAccelerationValues(temp);

        return true;
    }

    public ArrayList<AccelerationData> getAllSavedAccelerationObjects() {

        ArrayList<AccelerationData> resultSet = new ArrayList<>();

        Gson gson = JSONUtils.getInstance().getGson();

        Cursor cursor = tripStorageDBHelper.getAllAccelerationValues();
        try {
            while (cursor.moveToNext()) {
                AccelerationData result = (AccelerationData) gson.fromJson(cursor.getString(1), AccelerationData.class);
                resultSet.add(result);
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        } finally {
            cursor.close();
        }

        return  resultSet;
    }

    public boolean dropAllSavedAccelerationObjects() {

        tripStorageDBHelper.dropAllAccelerationValues();
        return true;
    }

    //see https://www.androidauthority.com/use-sqlite-store-data-app-599743/
    private class TripStorageDBHelper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "TripLocalDB.db";
        private static final int DATABASE_VERSION = 36;

        //full trip storage constants
        public static final String TRIP_TABLE_NAME = "FullTripsTable";
        public static final String TRIP_COLUMN_ID = "_id";
        public static final String TRIP_COLUMN_DATA = "data";
        public static final String TRIP_COLUMN_USERID = "uid";
        public static final String TRIP_COLUMN_INITDATE = "initDate";

        public static final String TRIP_DIGEST_TABLE_NAME = "FullTripsDigestTable";
        public static final String TRIP_DIGEST_COLUMN_ID = "_id";
        public static final String TRIP_DIGEST_COLUMN_DATA = "data";
        public static final String TRIP_DIGEST_COLUMN_USERID = "uid";
        public static final String TRIP_DIGEST_COLUMN_INITDATE = "initDate";

        //snapshot data constants

        //last locations saved
        public static final String SAVED_LOCATIONS_TABLE_NAME = "SavedLocationsTable";
        public static final String SAVED_LOCATIONS_COLUMN_ID = "_id";
        public static final String SAVED_LOCATIONS_COLUMN_DATA = "location";

        //last activity data saved
        public static final String SAVED_ACTIVITIES_TABLE_NAME = "SavedActivitiesTable";
        public static final String SAVED_ACTIVITIES_COLUMN_ID = "_id";
        public static final String SAVED_ACTIVITIES_COLUMN_DATA = "data";

        //temp full trip parts
        public static final String SNAPSHOTS_TRIPPARTS_TABLE_NAME="SnapshotFullTripPartsTable";
        public static final String SNAPSHOTS_TRIPPARTS_COLUMN_ID="_id";
        public static final String SNAPSHOTS_TRIPPARTS_COLUMN_DATA="fulltrippartdata";

        //temp last acceleration values
        public static final String SNAPSHOTS_ACCELERATIONS_TABLE_NAME="SnapshotAccelerationsTable";
        public static final String SNAPSHOTS_ACCELERATIONS_COLUMN_ID="_id";
        public static final String SNAPSHOTS_ACCELERATIONS_COLUMN_DATA="accelerationvalue";

        //ML input data
        public static final String SNAPSHOTS_ML_INPUT_METADATA_TABLE_NAME = "MLInputMetadata";
        public static final String SNAPSHOTS_ML_INPUT_METADATA_COLUMN_ID  = "_id";
        public static final String SNAPSHOTS_ML_INPUT_METADATA_COLUMN_DATA = "data";

        ////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////
        ///// Survey data

        public static final String SURVEYS_TABLE_NAME="SurveysTable";
        public static final String SURVEYS_COLUMN_ID="_id";
        public static final String SURVEYS_COLUMN_SURVEY_ID="SurveyID";
        public static final String SURVEYS_COLUMN_DATA="SurveyData";


        public static final String TRIGGERED_SURVEYS_TABLE_NAME="TriggeredSurveysTable";
        public static final String TRIGGERED_SURVEYS_COLUMN_ID="_id";
        public static final String TRIGGERED_SURVEYS_COLUMN_DATA="TriggeredSurveyData";
        //surveyid+triggertimestamp
        public static final String TRIGGERED_SURVEYS_COLUMN_SURVEY_ID="TriggeredSurveyID";

        ////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////
        ///// Log data

        public static final String LOG_TABLE_NAME="LogTable";
        public static final String LOG_COLUMN_ID="_id";
        public static final String LOG_COLUMN_DATA="LogData";
        //surveyid+triggertimestamp
        public static final String LOG_TIMESTAMP="LogTimestamp";

        boolean walModeEnabled;

        public TripStorageDBHelper(Context context, boolean gWalMode) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);

            //setIdleConnectionTimeout(5000);

            walModeEnabled = gWalMode;

//            if(walModeEnabled) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                    setWriteAheadLoggingEnabled(true);
//                    Log.d("sqlLiteHelper", "Setting WAL");
//                }
//            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            Log.e("sqlite","oncreate");

            db.execSQL("CREATE TABLE " + TRIP_TABLE_NAME + "(" + TRIP_COLUMN_ID + " INTEGER PRIMARY KEY, " + TRIP_COLUMN_DATA + " TEXT, " + TRIP_COLUMN_INITDATE + " TEXT UNIQUE, " + TRIP_COLUMN_USERID + " TEXT)"
            );

            db.execSQL("CREATE TABLE " + TRIP_DIGEST_TABLE_NAME + "(" + TRIP_DIGEST_COLUMN_ID + " INTEGER PRIMARY KEY, " + TRIP_DIGEST_COLUMN_DATA + " TEXT, " + TRIP_DIGEST_COLUMN_INITDATE + " TEXT UNIQUE, " + TRIP_DIGEST_COLUMN_USERID + " TEXT)"
            );

            db.execSQL("CREATE TABLE " + SAVED_LOCATIONS_TABLE_NAME + "(" +
                    SAVED_LOCATIONS_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    SAVED_LOCATIONS_COLUMN_DATA + " TEXT)"
            );

            db.execSQL("CREATE TABLE " + SNAPSHOTS_TRIPPARTS_TABLE_NAME + "(" +
                    SNAPSHOTS_TRIPPARTS_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    SNAPSHOTS_TRIPPARTS_COLUMN_DATA + " TEXT)"
            );

            db.execSQL("CREATE TABLE " + SAVED_ACTIVITIES_TABLE_NAME + "(" +
                    SAVED_ACTIVITIES_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    SAVED_ACTIVITIES_COLUMN_DATA + " TEXT)"
            );

            db.execSQL("CREATE TABLE " + SNAPSHOTS_ACCELERATIONS_TABLE_NAME + "(" +
                    SNAPSHOTS_ACCELERATIONS_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    SNAPSHOTS_ACCELERATIONS_COLUMN_DATA+ " TEXT)"
            );

            db.execSQL("CREATE TABLE " + SURVEYS_TABLE_NAME + "(" + SURVEYS_COLUMN_ID + " INTEGER PRIMARY KEY, " + SURVEYS_COLUMN_SURVEY_ID+ " TEXT, " + SURVEYS_COLUMN_DATA + " TEXT)");

            db.execSQL("CREATE TABLE " + TRIGGERED_SURVEYS_TABLE_NAME + "(" + TRIGGERED_SURVEYS_COLUMN_ID + " INTEGER PRIMARY KEY, " + TRIGGERED_SURVEYS_COLUMN_SURVEY_ID + " TEXT, " + TRIGGERED_SURVEYS_COLUMN_DATA + " TEXT)");


            //ML
            db.execSQL("CREATE TABLE " + SNAPSHOTS_ML_INPUT_METADATA_TABLE_NAME + "(" +
                    SNAPSHOTS_ML_INPUT_METADATA_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    SNAPSHOTS_ML_INPUT_METADATA_COLUMN_DATA + " TEXT)"
            );

//            db.execSQL("CREATE TABLE " + LOG_TABLE_NAME + "(" +
//                    LOG_COLUMN_ID + " INTEGER PRIMARY KEY, " +
//                    LOG_TIMESTAMP + " INTEGER, " +
//                    LOG_COLUMN_DATA + " TEXT)"
//            );

        }

        void deleteRecursive(File fileOrDirectory) {

//            if (fileOrDirectory.isDirectory()) {
//                Log.e("Persistence", "isDirectory" + fileOrDirectory.isDirectory());
//                for (File child : fileOrDirectory.listFiles()) {
//                    Log.e("Persistence", "child " + child);
//                    deleteRecursive(child);
//                }
//            }


            if (fileOrDirectory.isDirectory())
            {
                String[] children = fileOrDirectory.list();

                if(children != null){

                    for (int i = 0; i < children.length; i++)
                    {
                        new File(fileOrDirectory, children[i]).delete();
                    }

                }
            }

        }

        // full trip storage methods
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


            Log.e("Persistence", "old " + oldVersion + " new " + newVersion);

            if(newVersion == 36){

                deleteRecursive(new File(Environment.getExternalStorageDirectory().toString() +"/motivAndroidLogs/"));

            }else {

                db.execSQL("DROP TABLE IF EXISTS " + TRIP_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + TRIP_DIGEST_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + SAVED_LOCATIONS_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + SAVED_ACTIVITIES_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + SNAPSHOTS_TRIPPARTS_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + SNAPSHOTS_ACCELERATIONS_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + SURVEYS_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + TRIGGERED_SURVEYS_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + SNAPSHOTS_ML_INPUT_METADATA_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);

                onCreate(db);

            }
        }


        public boolean insertFullTrip(String data, String userID, Long initTimestamp) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(TRIP_COLUMN_DATA, data);
            contentValues.put(TRIP_COLUMN_USERID, userID);
            contentValues.put(TRIP_COLUMN_INITDATE, DateHelper.getDateFromTSString(initTimestamp));
            db.insert(TRIP_TABLE_NAME, null, contentValues);

            db.close();

            return true;
        }

        public boolean updateFullTripData(String data, String initDate){

            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(TRIP_COLUMN_DATA, data);

            db.update(TRIP_TABLE_NAME, contentValues, TRIP_COLUMN_INITDATE + " = ?", new String[]{initDate});

            db.close();

            return true;

        }

        public boolean deleteFullTripData(String initDate){

            SQLiteDatabase db = getWritableDatabase();
            db.delete(TRIP_TABLE_NAME, TRIP_COLUMN_INITDATE + " = ?", new String[]{initDate});

            db.close();

            return true;

        }

        public Cursor getFullTrip(int id) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + TRIP_TABLE_NAME + " WHERE " + TRIP_COLUMN_ID + "=?", new String[]{Integer.toString(id)});
            return res;
        }

        public Cursor getFullTripByDate(String date) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + TRIP_TABLE_NAME + " WHERE " + TRIP_COLUMN_INITDATE + "=?" + " LIMIT 1", new String[]{date});



            return res;
        }

        public Cursor getFullTripDates(){
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT initDate FROM " + TRIP_TABLE_NAME,null);

            return res;
        }

        public Cursor getAllFullTrips() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + TRIP_TABLE_NAME, null);

            return res;
        }

        public void dropAllFullTrips(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(TRIP_TABLE_NAME,null,null);

            db.close();

        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Full trip digest

        public boolean insertFullTripDigest(String data, String userID, String tripID) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(TRIP_DIGEST_COLUMN_DATA, data);
            contentValues.put(TRIP_DIGEST_COLUMN_USERID, userID);
            contentValues.put(TRIP_DIGEST_COLUMN_INITDATE, tripID);
            db.insert(TRIP_DIGEST_TABLE_NAME, null, contentValues);

            db.close();

            return true;
        }

        public boolean updateFullTripDigestData(String data, String initDate){

            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(TRIP_DIGEST_COLUMN_DATA, data);

            db.update(TRIP_DIGEST_TABLE_NAME, contentValues, TRIP_DIGEST_COLUMN_INITDATE + " = ?", new String[]{initDate});

            db.close();

            return true;

        }

        public boolean deleteFullTripDigestData(String initDate){

            SQLiteDatabase db = getWritableDatabase();
            db.delete(TRIP_DIGEST_TABLE_NAME, TRIP_DIGEST_COLUMN_INITDATE + " = ?", new String[]{initDate});

            db.close();

            return true;

        }

//        public Cursor getFullTrip(int id) {
//            SQLiteDatabase db = this.getReadableDatabase();
//            Cursor res = db.rawQuery("SELECT * FROM " + TRIP_TABLE_NAME + " WHERE " + TRIP_COLUMN_ID + "=?", new String[]{Integer.toString(id)});
//            return res;
//        }

        public Cursor getFullTripDigestByDate(String date) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + TRIP_DIGEST_TABLE_NAME + " WHERE " + TRIP_DIGEST_COLUMN_INITDATE + "=?" + " LIMIT 1", new String[]{date});

            return res;
        }


        public Cursor getAllFullTripDigests() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + TRIP_DIGEST_TABLE_NAME, null);

            return res;
        }

//        public void dropAllFullTripDigests(){
//            SQLiteDatabase db = this.getReadableDatabase();
//            db.delete(TRIP_DIGEST_TABLE_NAME,null,null);
//
//            db.close();
//
//        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        //

        public boolean insertFullTripPart(String data) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(SNAPSHOTS_TRIPPARTS_COLUMN_DATA, data);
            db.insert(SNAPSHOTS_TRIPPARTS_TABLE_NAME, null, contentValues);

            db.close();

            return true;
        }

        public boolean insertFullTripParts(ArrayList<String> data) {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try{
                for(String tripPart : data){
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SNAPSHOTS_TRIPPARTS_COLUMN_DATA, tripPart);
                    db.insert(SNAPSHOTS_TRIPPARTS_TABLE_NAME, null, contentValues);
                }
                db.setTransactionSuccessful();
            }finally{
                db.endTransaction();

            }

            db.close();

            return true;
        }

        /*public Cursor getFullTripPart(int id) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SNAPSHOTS_TRIPPARTS_TABLE_NAME + " WHERE " + SNAPSHOTS_TRIPPARTS_COLUMN_ID + "=", new String[]{Integer.toString(id)});
            return res;
        }*/

        public Cursor getAllFullTripParts() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SNAPSHOTS_TRIPPARTS_TABLE_NAME, null);

            return res;
        }

        public void dropAllFullTripParts(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(SNAPSHOTS_TRIPPARTS_TABLE_NAME,null,null);

            db.close();
        }

        // snapshot db methods
        public boolean insertLocation(String data) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put(SAVED_LOCATIONS_COLUMN_DATA, data);
            db.insert(SAVED_LOCATIONS_TABLE_NAME, null, contentValues);

            db.close();

            return true;
        }

        public Cursor getAllSavedLocations() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SAVED_LOCATIONS_TABLE_NAME, null);


            return res;
        }

        public void dropAllLocationRecords(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(SAVED_LOCATIONS_TABLE_NAME,null,null);

            db.close();

        }

        public Cursor getAllSavedActivities() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SAVED_ACTIVITIES_TABLE_NAME, null);


            return res;
        }

        public void dropAllActivityRecords(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(SAVED_ACTIVITIES_TABLE_NAME,null,null);

            db.close();
        }

        public boolean insertActivity(String data) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put(SAVED_ACTIVITIES_COLUMN_DATA, data);
            db.insert(SAVED_ACTIVITIES_TABLE_NAME, null, contentValues);

            db.close();

            return true;
        }



        // snapshot data constants
        public boolean insertTripPart(String data, String tripId) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put(SNAPSHOTS_TRIPPARTS_COLUMN_DATA, data);
            db.insert(SNAPSHOTS_TRIPPARTS_COLUMN_DATA, null, contentValues);


            return true;
        }

        public Cursor getAllTripParts() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SNAPSHOTS_TRIPPARTS_TABLE_NAME, null);


            return res;
        }

        public void dropAllTripParts(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(SNAPSHOTS_TRIPPARTS_TABLE_NAME,null,null);

        }


        public boolean insertAccelerationValues(ArrayList<String> accelerationData) {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try{
                for(String accelerationValue : accelerationData){
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SNAPSHOTS_ACCELERATIONS_COLUMN_DATA, accelerationValue);
                    db.insert(SNAPSHOTS_ACCELERATIONS_TABLE_NAME, null, contentValues);
                }
                db.setTransactionSuccessful();
            }finally{
                db.endTransaction();
            }

            db.close();

            return true;
        }

        public Cursor getAllAccelerationValues() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SNAPSHOTS_ACCELERATIONS_TABLE_NAME, null);


            return res;
        }

        public void dropAllAccelerationValues(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(SNAPSHOTS_ACCELERATIONS_TABLE_NAME,null,null);

            db.close();
        }

        public boolean insertMLInputMetadata(String data) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put(SNAPSHOTS_ML_INPUT_METADATA_COLUMN_DATA, data);
            db.insert(SNAPSHOTS_ML_INPUT_METADATA_TABLE_NAME, null, contentValues);

            db.close();

            return true;
        }

        public Cursor getAllMLInputMetadata() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + SNAPSHOTS_ML_INPUT_METADATA_TABLE_NAME, null);

            return res;
        }

        public void dropAllMLInputMetadata(){
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete(SNAPSHOTS_ML_INPUT_METADATA_TABLE_NAME,null,null);

            db.close();

        }

    }




}

