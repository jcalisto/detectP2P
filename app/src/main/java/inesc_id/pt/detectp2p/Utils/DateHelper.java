package inesc_id.pt.detectp2p.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by admin on 1/29/18.
 */

public class DateHelper {

    public static String getDateFromTSString(long ts){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a");
        return simpleDateFormat.format(ts);
    }

    public static String getHMSfromMS(long millis) {

        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

    }

    public static String getHoursMinutesFromTSString(long ts){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(ts);
    }

    public static String getYearMonthDayFromTSString(long ts){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(ts);
    }

    public static String getMinutesAndSecondsFromSeconds(double time){

        double timeTemp = time;

        int i=0;
        while(timeTemp>60){

            timeTemp -= 60;

            i++;
        }

        return i + ":" + (int) timeTemp;

    }

    public static Date getDateFromRRTimeAndDate(String departureDateRouteRank, String departureTimeRouteRank){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            return simpleDateFormat.parse(departureDateRouteRank+" "+departureTimeRouteRank);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public static boolean isSameDay(long ts1, long ts2) {

        Date date1 = new Date(ts1);
        Date date2 = new Date(ts2);

        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(date1);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(date2);
        boolean sameYear = calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
        boolean sameMonth = calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH);
        boolean sameDay = calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
        return (sameDay && sameMonth && sameYear);
    }

}
