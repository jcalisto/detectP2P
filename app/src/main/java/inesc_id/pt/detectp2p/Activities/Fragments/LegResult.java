package inesc_id.pt.detectp2p.Activities.Fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.Validate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TransportModeDeterminer;
import inesc_id.pt.detectp2p.TripDetection.dataML.Trip;
import inesc_id.pt.detectp2p.TripValidationManager.ValidatedDataManager;
import inesc_id.pt.detectp2p.TripValidationManager.ValidatedTrip;
import inesc_id.pt.detectp2p.Utils.TransportInfo;

public class LegResult extends Fragment {

    Trip leg;
    TextView tvTripId;
    TextView tvClassifierValue;
    TextView tvRealValue;
    TextView tvClassifierProbs;
    TextView tvPeerCount;
    TextView tvPeerResults;
    TextView tvMatrixValues;
    TextView tvFinalResults;
    TextView tvAdjustedValues;
    ImageView checkbox;
    Spinner spinner;

    private boolean initiatingSpinner = true;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_evaluated_trip, container, false);

        ValidatedDataManager validatedDataManager = ValidatedDataManager.getInstance();

        tvTripId = view.findViewById(R.id.tvTripId);
        tvClassifierValue = view.findViewById(R.id.tvClassifierValue);
        tvRealValue = view.findViewById(R.id.tvRealValue);
        tvClassifierProbs = view.findViewById(R.id.tvClassifierProbs);
        tvPeerCount = view.findViewById(R.id.tvPeerCount);
        tvPeerResults = view.findViewById(R.id.tvPeerResults);
        tvMatrixValues = view.findViewById(R.id.tvPathScore);
        tvFinalResults = view.findViewById(R.id.tvFinalResults);
        tvAdjustedValues = view.findViewById(R.id.tvAdjustedValues);
        checkbox = view.findViewById(R.id.checkbox);
        spinner = view.findViewById(R.id.spinner2);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.modes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        String tripId = validatedDataManager.generateTripId(leg);
        ValidatedTrip validatedTrip = validatedDataManager.getValidatedTrip(tripId);

        tvTripId.setText("Trip ID: " + tripId);
        tvClassifierValue.setText(TransportInfo.codes.modalities[leg.getSugestedModeOfTransport()]);
        tvRealValue.setText("");

        if(validatedTrip != null){
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setColorFilter(Color.GREEN);
            tvRealValue.setText(TransportInfo.codes.modalities[validatedTrip.realMode]);
            tvRealValue.setTextColor(Color.GREEN);
        }

        // GET PEER INFO
        String peerProbabilities = "";
        HashMap<Integer, Double> peerPredictions = TransportModeDeterminer.getInstance().getModeInfoInTimeInterval(leg.getInitTimestamp(), leg.getEndTimestamp());
        if(peerPredictions != null && peerPredictions.size() > 0){
            for(Integer i : peerPredictions.keySet()){
                peerProbabilities += TransportInfo.codes.modalities[i] + " = " + peerPredictions.get(i) + "\n";
            }
        }
        tvPeerResults.setText(peerProbabilities);

        // CLASSIFIER INFO
        String probabilities = "";
        if(leg.getProbasDict() != null){
            for(Integer i : leg.getProbasDict().keySet()){
                probabilities += "" + TransportInfo.codes.modalities[i] + " = " + leg.getProbasDict().get(i) + "\n";
            }
        }
        tvClassifierProbs.setText(probabilities);

        //ADJUST CLASSIFIER PREDICTION WITH VALIDATIONS
        Float[][] localValidationMatrix = validatedDataManager.getLocalValidationMatrix();
        HashMap<Integer, Double> adjustedProbas = leg.getProbasDict();
        int highestMode1 = 0;
        int highestMode2 = 0;
        int highestMode3 = 0;
        double highestMode1Prob = 0;
        double highestMode2Prob = 0;
        double highestMode3Prob = 0;
        double tempProb = 0;

        for(Integer mode : adjustedProbas.keySet()){
            tempProb = adjustedProbas.get(mode);
            if(tempProb > highestMode1Prob){
                highestMode3 = highestMode2;
                highestMode2 = highestMode1;
                highestMode3Prob = highestMode2Prob;
                highestMode2Prob = highestMode1Prob;
                highestMode1 = mode;
                highestMode1Prob = tempProb;
            } else if(tempProb > highestMode2Prob) {
                highestMode3 = highestMode2;
                highestMode3Prob = highestMode2Prob;
                highestMode2 = mode;
                highestMode2Prob = tempProb;
            } else if(tempProb > highestMode3Prob) {
                highestMode3 = mode;
                highestMode3Prob = tempProb;
            }
        }
        Log.d("AdjustedProbas", "Highest Mode 1 : " + highestMode1);
        Log.d("AdjustedProbas", "Highest Mode 2 : " + highestMode2);
        Log.d("AdjustedProbas", "Highest Mode 3 : " + highestMode3);

        double mode2insteadOfMode1Prob = localValidationMatrix[highestMode1][highestMode2] / localValidationMatrix[highestMode1][0];
        double mode3insteadOfMode1Prob = localValidationMatrix[highestMode1][highestMode3] / localValidationMatrix[highestMode1][0];
        Log.d("AdjustedProbas", "mode2InsteadOf1: " + mode2insteadOfMode1Prob);
        Log.d("AdjustedProbas", "mode3InsteadOf1: " + mode3insteadOfMode1Prob);


        if(highestMode2 != highestMode1) {
            adjustedProbas.put(highestMode1, highestMode1Prob - mode2insteadOfMode1Prob / 2);
            adjustedProbas.put(highestMode2, highestMode2Prob+ mode2insteadOfMode1Prob / 2);
        }
        if(highestMode3 != highestMode2) {
            adjustedProbas.put(highestMode1, adjustedProbas.get(highestMode1) - mode3insteadOfMode1Prob / 2);
            adjustedProbas.put(highestMode3, highestMode3Prob + mode3insteadOfMode1Prob / 2);
        }
        String adjustedProbabilities = "";
        for(Integer i : adjustedProbas.keySet()){

            adjustedProbabilities += "" + TransportInfo.codes.modalities[i] + " = " + adjustedProbas.get(i) + "\n";
        }
        tvAdjustedValues.setText(adjustedProbabilities);



        // PATH SCORE
        ArrayList<ValidatedTrip> userValidations = validatedDataManager.getUserValidations();

        int commonCoordsCount = 0;
        for(ValidatedTrip vTrip : userValidations){
            commonCoordsCount = validatedDataManager.compareTrips(leg, vTrip);
        }



        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(initiatingSpinner || i==0) {
                    initiatingSpinner = false;
                    return;
                }
                leg.setCorrectedModeOfTransport(TransportInfo.getModeCodeFromString(TransportInfo.codes.correctedModalities[i]));
                Log.d("LegValidationAdapter", "Trip validated mode set to: " + leg.getCorrectedModeOfTransport());
                checkbox.setColorFilter(Color.GREEN);
                tvRealValue.setText(TransportInfo.codes.modalities[leg.getCorrectedModeOfTransport()]);
                tvRealValue.setTextColor(Color.GREEN);
                ValidatedDataManager.getInstance().addLocalValidation(leg, TransportInfo.getModeCodeFromString(TransportInfo.codes.correctedModalities[i]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void setLeg(Trip leg){
        this.leg = leg;
    }
}
