package inesc_id.pt.detectp2p.ModeClassification;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.gson.Gson;

import org.dmg.pmml.FieldName;
import org.joda.time.DateTime;
import org.jpmml.android.EvaluatorUtil;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelField;
import org.jpmml.evaluator.OutputField;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import inesc_id.pt.detectp2p.ModeClassification.DataModels.ActivityDetected;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTrip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.MLAlgorithmInput;
import inesc_id.pt.detectp2p.ModeClassification.dataML.MLInputMetadata;
import inesc_id.pt.detectp2p.Utils.JSONUtils;

import static org.joda.time.DateTimeZone.UTC;

/**
 * Created by constantin on 7/31/18.
 */

public class Classifier implements Serializable {

    private final static String TAG = "Classifier";

    private static Classifier instance = null;
    private static AssetManager assetManager = null;

    private Evaluator evaluator;
    private static String evaluatorType;


    private Classifier() {
//        evaluatorType = "randomForest.pmml.ser";
        loadEvaluator(evaluatorType);
    }

    public static void initClassifier(Context context, String evaluatorFileName){
        assetManager = context.getAssets();
        evaluatorType = evaluatorFileName;
    }

    public static Classifier getInstance(){
        if (Classifier.instance == null){
            Classifier.instance = new Classifier();
        }
        return Classifier.instance;
    }


    private Evaluator createEvaluator(String name) throws Exception {

        InputStream is = assetManager.open(name);
        try{
            return EvaluatorUtil.createEvaluator(is);
        } catch (Exception e){
            Log.d(TAG, e.getMessage());
        }finally {
            is.close();
        }
        return null;
    }

    static private List<FieldName> getNames(List<? extends ModelField> modelFields){
        List<FieldName> names = new ArrayList<>(modelFields.size());

        for(ModelField modelField : modelFields){
            FieldName name = modelField.getName();

            names.add(name);
        }

        return names;
    }


    private void loadEvaluator(String evaluatorFileName){
        Log.d(TAG, "Importing evaluator " + evaluatorFileName +  "at " + new DateTime(UTC).getMillis());
        try {
            evaluator = createEvaluator(evaluatorFileName);
            evaluator.verify();
        } catch(Exception e){
            throw new RuntimeException(e);
        }

        Log.d(TAG, "Evaluator imported" +  "at " + new DateTime(UTC).getMillis());
    }

    public List<FieldName> getActiveFields(){
        return getNames(evaluator.getActiveFields());
    }

    public List<FieldName> getTargetFields(){
        return getNames(evaluator.getTargetFields());
    }

    public List<FieldName> getOutputFields(){
        return getNames(evaluator.getOutputFields());
    }


    public HashMap<Integer, Double> getEvaluatorResult(double[] values){

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        List<InputField> inputFields = evaluator.getInputFields();
        for(InputField inputField : inputFields){
            FieldName inputFieldName = inputField.getName();

            // The raw (ie. user-supplied) value could be any Java primitive value
            Object rawValue = getValueByInput(inputFieldName, values);

            // The raw value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
            FieldValue inputFieldValue = inputField.prepare(rawValue);

            arguments.put(inputFieldName, inputFieldValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);

        List<OutputField> outputFields = evaluator.getOutputFields();
        HashMap<Integer, Double> predicts = new HashMap<>();
        for(OutputField outputField : outputFields){
            FieldName outputFieldName = outputField.getName();
            Object outputFieldValue = results.get(outputFieldName);

            Integer mode = Integer.valueOf(outputField.getOutputField().getValue());
            Double prob = (Double) outputFieldValue;
            predicts.put(mode, prob);
            Log.d(TAG,  "Field: " + mode + "; Res: " + prob);
        }
        return predicts;
    }


    public MLInputMetadata evaluateSegment(MLAlgorithmInput mlAlgorithmInput){

        HashMap<Integer, Double> predicts = new HashMap<>();

        if((mlAlgorithmInput.getProcessedPoints().getAvgSpeed() <= keys.STILL_AVG_SPEED_FILTER)
                && (mlAlgorithmInput.getAccelsBelowFilter() >= keys.STILL_ACCELS_BELOW_FILTER)){

            List<OutputField> outputFields = evaluator.getOutputFields();
            for(OutputField outputField : outputFields){
                Integer mode = Integer.valueOf(outputField.getOutputField().getValue());
                Double prob = 0.0;
                predicts.put(mode, prob);
            }
            predicts.put(ActivityDetected.keys.still, 1.0);

        }else{
            predicts = getEvaluatorResult(mlAlgorithmInput);
            predicts.put(ActivityDetected.keys.still, 0.0);
        }

        MLInputMetadata result = new MLInputMetadata(mlAlgorithmInput, predicts);
        Log.d(TAG, "Segment Evaluated!!" + result.getBestMode().getKey());
        return result;
    }

    private HashMap<Integer, Double> getEvaluatorResult(MLAlgorithmInput input){

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        List<InputField> inputFields = evaluator.getInputFields();
        for(InputField inputField : inputFields){
            FieldName inputFieldName = inputField.getName();

            // The raw (ie. user-supplied) value could be any Java primitive value
            Object rawValue = getValueByInput(inputFieldName, input);

            // The raw value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
            FieldValue inputFieldValue = inputField.prepare(rawValue);

            arguments.put(inputFieldName, inputFieldValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);

        List<OutputField> outputFields = evaluator.getOutputFields();
        HashMap<Integer, Double> predicts = new HashMap<>();
        for(OutputField outputField : outputFields){
            FieldName outputFieldName = outputField.getName();
            Object outputFieldValue = results.get(outputFieldName);

            Integer mode = Integer.valueOf(outputField.getOutputField().getValue());
            Double prob = (Double) outputFieldValue;
            predicts.put(mode, prob);
            Log.d(TAG, "Field: " + mode + "; Res: " + prob);
        }
        return predicts;
    }

    private Object getValueByInput(FieldName inputFieldName, MLAlgorithmInput input) {
        // Log.d("---InputFiled:", inputFieldName.getValue());
        switch (inputFieldName.getValue()) {
            case "estimatedSpeed":
                return input.getProcessedPoints().getEstimatedSpeed();
            case "OS":
                return input.getOSVersion();
            case "accelsBelowFilter":
                return input.getAccelsBelowFilter();
            case "accelBetw_03_06":
                return input.getProcessedAccelerations().getBetween_03_06();
            case "accelBetw_06_1":
                return input.getProcessedAccelerations().getBetween_06_1();
            case "accelBetw_1_3":
                return input.getProcessedAccelerations().getBetween_1_3();
            case "accelBetw_3_6":
                return input.getProcessedAccelerations().getBetween_3_6();
            case "accelAbove_6":
                return input.getProcessedAccelerations().getAbove_6();
            case "avgFilteredAccel":
                return input.getAvgFilteredAccels();
            case "avgAccel":
                return input.getProcessedAccelerations().getAvgAccel();
            case "maxAccel":
                return input.getProcessedAccelerations().getMaxAccel();
            case "minAccel":
                return input.getProcessedAccelerations().getMinAccel();
            case "stdDevAccel":
                return input.getProcessedAccelerations().getStdDevAccel();
            case "avgSpeed":
                return input.getProcessedPoints().getAvgSpeed();
            case "maxSpeed":
                return input.getProcessedPoints().getMaxSpeed();
            case "minSpeed":
                return input.getProcessedPoints().getMinSpeed();
            case "stdDevSpeed":
                return input.getProcessedPoints().getStdDevSpeed();
            case "avgAcc":
                return input.getProcessedPoints().getAvgAcc();
            case "maxAcc":
                return input.getProcessedPoints().getMaxAcc();
            case "minAcc":
                return input.getProcessedPoints().getMinAcc();
            case "stdDevAcc":
                return input.getProcessedPoints().getStdDevAcc();
            case "gpsTimeMean":
                return input.getProcessedPoints().getGpsTimeMean();
            case "distance":
                return input.getProcessedPoints().getDistance();
        }
        throw new RuntimeException("Unknown input field: " + inputFieldName.getValue());
    }



    // Tests only:
    private Object getValueByInput(FieldName inputFieldName, double[] values) {
        // Log.d("---InputFiled:", inputFieldName.getValue());
        switch (inputFieldName.getValue()) {
            case "estimatedSpeed":
                return values[0];
            case "OS":
                return values[1];
            case "accelsBelowFilter":
                return values[2];
            case "avgFilteredAccel":
                return values[3];
            case "avgAccel":
                return values[4];
            case "maxAccel":
                return values[5];
            case "minAccel":
                return values[6];
            case "stdDevAccel":
                return values[7];
            case "avgSpeed":
                return values[8];
            case "maxSpeed":
                return values[9];
            case "minSpeed":
                return values[10];
            case "stdDevSpeed":
                return values[11];
            case "avgAcc":
                return values[12];
            case "maxAcc":
                return values[13];
            case "minAcc":
                return values[13];
            case "stdDevAcc":
                return values[13];
            case "gpsTimeMean":
                return values[13];
            case "distance":
                return values[13];
        }
        throw new RuntimeException("Unknown input field: " + inputFieldName.getValue());
    }

    public interface keys{

        double STILL_AVG_SPEED_FILTER = 2.5;
        double STILL_ACCELS_BELOW_FILTER = 0.8;

    }
}
