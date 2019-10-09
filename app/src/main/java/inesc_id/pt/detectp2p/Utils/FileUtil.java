package inesc_id.pt.detectp2p.Utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import inesc_id.pt.detectp2p.TripDetection.Classifier;

public class FileUtil {

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }

    public static void startWriteToLog(){
        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/detectP2P_Folder" );
            File logDirectory = new File( appDirectory + "/log" );
            String date = new SimpleDateFormat("dd:MMM_HH:mm").format(new Date());
            Log.d("Main Activity", "Date: " + date);
            File logFile = new File( logDirectory, "logcat" + date + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
    }

    public static byte[] readClassifierToBytes(){
        File appDirectory = new File( Environment.getExternalStorageDirectory() + "/detectP2P_Folder" );
        File logDirectory = new File( appDirectory + "/ML_Models" );
        File file = new File( logDirectory, "classifier_v2.pmml.ser" );

        byte[] classifier = null;
        try {
            classifier = FileUtils.readFileToByteArray(file);

        } catch(Exception e) { Log.e("FileUtil", e.getMessage()); }

        return classifier;
    }

    public static Classifier readClassifier(Context context, String fileName){

        File appDirectory = new File( Environment.getExternalStorageDirectory() + "/detectP2P_Folder" );
        File logDirectory = new File( appDirectory + "/ML_Models" );
        File file = new File( logDirectory, "classifier1" );

        FileInputStream fis;
        ObjectInputStream is;
        Classifier newClassifier = null;
        try {
            fis = new FileInputStream(file);
            is = new ObjectInputStream(fis);
            newClassifier = (Classifier) is.readObject();
            is.close();
            fis.close();
        } catch(Exception e) { Log.e("Classifier", e.getMessage()); }

        return newClassifier;

    }

    public static void writeClassifier(String fileName, byte[] classifierBytes){
        File appDirectory = new File( Environment.getExternalStorageDirectory() + "/detectP2P_Folder" );
        File logDirectory = new File( appDirectory + "/ML_Models" );
        File file = new File( logDirectory, fileName);

        // create app folder
        if ( !appDirectory.exists() ) {
            appDirectory.mkdir();
        }

        // create log folder
        if ( !logDirectory.exists() ) {
            logDirectory.mkdir();
        }


        try {
            FileOutputStream fOutputStream = new FileOutputStream(file, false);
            FileUtils.writeByteArrayToFile(file, classifierBytes);

        } catch (Exception e) {
            Log.e("FileUtil", e.getMessage());
        }
    }

    public static void copyAssets(Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        try {
            in = assetManager.open("randomForest.pmml.ser");
            in.reset();

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/detectP2P_Folder" );
            File logDirectory = new File( appDirectory + "/ML_Models" );
            File file = new File( logDirectory, "classifier_v2.pmml.ser" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            copyFile(in, file);

        } catch(IOException e) {
            Log.e("FileUtil", "Failed to copy asset file");
        }
    }

    private static void copyFile(InputStream in, File dest) {

        try {
            int length = 0;

            InputStream inputStream = in;
            FileOutputStream fOutputStream = new FileOutputStream(dest);
            //note the following line
            byte[] buffer = new byte[65536];
            while ((length = inputStream.read(buffer)) > 0) {
                fOutputStream.write(buffer, 0, length);
            }
            fOutputStream.flush();
            fOutputStream.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e("FileUtil", e.getMessage());
        }
    }
}
