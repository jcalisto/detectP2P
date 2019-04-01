package inesc_id.pt.detectp2p.Utils;

import android.os.Build;

/**
 * Created by admin on 7/11/17.
 */

public class MiscUtils {

    public MiscUtils(){}

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static String getOSVersion(){

        /*Field[] fields = Build.VERSION_CODES.class.getFields();


        return fields[Build.VERSION.SDK_INT + 1].getName();*/

        return Build.VERSION.SDK_INT+"";

    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean checkPasswordValidity(String password) {

        if((password.length() < 6) || (password.length() > 16)){
            return false;
        }else{
            return true;
        }

    }

}
