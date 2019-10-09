package inesc_id.pt.detectp2p.Utils;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import inesc_id.pt.detectp2p.TripValidationManager.ValidatedTrip;

public class ValidatedTripStorage {

    public static void writeExternalValidationsToStorage(ArrayList<ValidatedTrip> validations){

        try {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/detectP2P_Folder");
            File logDirectory = new File(appDirectory + "/validations");
            File logFile = new File(logDirectory, "external-validations.txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            FileOutputStream fos  = null;
            ObjectOutputStream oos  = null;

            try {
                fos = new FileOutputStream(logFile);
                oos = new ObjectOutputStream(fos);

                oos.writeInt(validations.size()); // Save size first
                for(ValidatedTrip trip : validations) {
                    Log.d("ValidatedTripStorage", "writing local validated trip");
                    oos.writeObject(trip);
                }
            }
            catch (Exception e) {

                Log.e("ValidatedTripStorage", "failed ", e);
            }
            finally {
                try {
                    if (oos != null)   oos.close();
                    if (fos != null)   fos.close();
                }
                catch (Exception e) { /* do nothing */ }
            }

        } catch (Exception e) {
            Log.d("ValidatedTripStorage", e.getMessage());
        }
    }

    public static void writeLocalValidationsToStorage(ArrayList<ValidatedTrip> validations){

        try {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/detectP2P_Folder");
            File logDirectory = new File(appDirectory + "/validations");
            File logFile = new File(logDirectory, "local-validations.txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            FileOutputStream fos  = null;
            ObjectOutputStream oos  = null;

            try {
                fos = new FileOutputStream(logFile);
                oos = new ObjectOutputStream(fos);

                oos.writeInt(validations.size()); // Save size first
                for(ValidatedTrip trip : validations) {
                    Log.d("ValidatedTripStorage", "writing local validated trip");
                    oos.writeObject(trip);
                }
            }
            catch (Exception e) {

                Log.e("ValidatedTripStorage", "failed ", e);
            }
            finally {
                try {
                    if (oos != null)   oos.close();
                    if (fos != null)   fos.close();
                }
                catch (Exception e) { /* do nothing */ }
            }

        } catch (Exception e) {
            Log.d("ValidatedTripStorage", e.getMessage());
        }
    }

    public static ArrayList<ValidatedTrip> readLocalValidations(){
        ArrayList<ValidatedTrip> list = new ArrayList<>();
        try {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/detectP2P_Folder");
            File logDirectory = new File(appDirectory + "/validations");
            File logFile = new File(logDirectory, "local-validations.txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            FileOutputStream fos  = null;
            ObjectOutputStream oos  = null;

            try {
                FileInputStream inStream = new FileInputStream(logFile);
                ObjectInputStream objectInStream = new ObjectInputStream(inStream);
                int count = objectInStream.readInt(); // Get the number of regions

                for (int c=0; c < count; c++)
                    list.add((ValidatedTrip) objectInStream.readObject());
                objectInStream.close();
            }
            catch (Exception e) {

                Log.e("ValidatedTripStorage", "failed ", e);
            }
            finally {
                try {
                    if (oos != null)   oos.close();
                    if (fos != null)   fos.close();
                }
                catch (Exception e) { /* do nothing */ }
            }

        } catch (Exception e) {
            Log.d("ValidatedTripStorage", e.getMessage());
        }
        return list;
    }

    public static ArrayList<ValidatedTrip> readAllValidations(){
        ArrayList<ValidatedTrip> list = new ArrayList<>();
        try {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/detectP2P_Folder");
            File logDirectory = new File(appDirectory + "/validations");
            File logFile = new File(logDirectory, "external-validations.txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            FileOutputStream fos  = null;
            ObjectOutputStream oos  = null;

            try {
                FileInputStream inStream = new FileInputStream(logFile);
                ObjectInputStream objectInStream = new ObjectInputStream(inStream);
                int count = objectInStream.readInt(); // Get the number of regions

                for (int c=0; c < count; c++)
                    list.add((ValidatedTrip) objectInStream.readObject());
                objectInStream.close();
            }
            catch (Exception e) {

                Log.e("ValidatedTripStorage", "failed ", e);
            }
            finally {
                try {
                    if (oos != null)   oos.close();
                    if (fos != null)   fos.close();
                }
                catch (Exception e) { /* do nothing */ }
            }

        } catch (Exception e) {
            Log.d("ValidatedTripStorage", e.getMessage());
        }
        return list;
    }

}
