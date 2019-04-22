package inesc_id.pt.detectp2p.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import inesc_id.pt.detectp2p.ModeClassification.Classifier;
import inesc_id.pt.detectp2p.ModeClassification.RawDataPreProcessing;

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

    public static void writeClassifier(Context context){
        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/detectP2P_Folder" );
            File logDirectory = new File( appDirectory + "/ML_Models" );

            File file = new File( logDirectory, "classifier1" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }
            Log.d("FileUtil", "Writing classifier with hash=" + c.hashCode());

            // clear the previous logcat and then write the new one to the file
            try {
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject("x");
                os.close();
                fos.close();

            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
    }
}
