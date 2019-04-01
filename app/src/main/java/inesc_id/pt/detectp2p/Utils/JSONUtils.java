package inesc_id.pt.detectp2p.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import inesc_id.pt.detectp2p.TripStateMachine.dataML.FullTripPart;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.Trip;

/**
 * Created by Duarte on 14/03/2018.
 */

public class JSONUtils {

    private Gson gson;

    private Gson gsonSurvey;

    private static JSONUtils instance;

    public static JSONUtils getInstance(){
        if(instance == null){
            instance = new JSONUtils();
            return instance;
        }else{
            return instance;
        }
    }

    private JSONUtils(){
        //for trip data
        GsonBuilder builder = new GsonBuilder();
        //builder.registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter());
        RuntimeTypeAdapterFactory<FullTripPart> adapter = RuntimeTypeAdapterFactory
                .of(FullTripPart.class)
                .registerSubtype(Trip.class);


        builder.registerTypeAdapterFactory(adapter);

        builder.setPrettyPrinting().excludeFieldsWithoutExposeAnnotation();
        builder.serializeSpecialFloatingPointValues();
    }



    public Gson getGson(){
        return gson;
    }

    public Gson getSurveyGSON(){

        return gsonSurvey;

    }

}
