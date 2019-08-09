package inesc_id.pt.detectp2p.Activities;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import inesc_id.pt.detectp2p.ModeClassification.dataML.Trip;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.Utils.TransportInfo;
import inesc_id.pt.detectp2p.ValidatedData.ValidatedDataManager;
import inesc_id.pt.detectp2p.ValidatedData.ValidatedTrip;

public class EvaluatedTripActivity extends AppCompatActivity {

    Trip currentTrip;
    TextView tvTripId;
    TextView tvClassifierValue;
    TextView tvRealValue;
    TextView tvClassifierProbs;
    TextView tvBelowFilter;
    TextView tv03_06;
    TextView tv06_1;
    TextView tv1_3;
    TextView tv3_6;
    TextView tv6;
    TextView tvPeerCount;
    TextView tvPeerResults;
    TextView tvMatrixValues;
    TextView tvFinalResults;
    ImageView checkbox;
    Spinner spinner;

    private boolean initiatingSpinner = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluated_trip);

        currentTrip = (Trip) getIntent().getSerializableExtra("TRIP");

        tvTripId = findViewById(R.id.tvTripId);
        tvClassifierValue = findViewById(R.id.tvClassifierValue);
        tvRealValue = findViewById(R.id.tvRealValue);
        tvClassifierProbs = findViewById(R.id.tvClassifierProbs);
        tvBelowFilter = findViewById(R.id.tvBelowFilter);
        tv03_06 = findViewById(R.id.tv03_06);
        tv06_1 = findViewById(R.id.tv06_1);
        tv1_3 = findViewById(R.id.tv1_3);
        tv3_6 = findViewById(R.id.tv3_6);
        tv6 = findViewById(R.id.tv6);
        tvPeerCount = findViewById(R.id.tvPeerCount);
        tvPeerResults = findViewById(R.id.tvPeerResults);
        tvMatrixValues = findViewById(R.id.tvMatrixValues);
        tvFinalResults = findViewById(R.id.tvFinalResults);
        checkbox = findViewById(R.id.checkbox);
        spinner = findViewById(R.id.spinner2);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.modes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(initiatingSpinner || i==0) {
                    initiatingSpinner = false;
                    return;
                }
                currentTrip.setCorrectedModeOfTransport(TransportInfo.getModeCodeFromString(TransportInfo.codes.correctedModalities[i]));
                Log.d("LegValidationAdapter", "Trip validated mode set to: " + currentTrip.getCorrectedModeOfTransport());
                checkbox.setColorFilter(Color.GREEN);
                ValidatedDataManager.getInstance().addLocalValidation(currentTrip, TransportInfo.getModeCodeFromString(TransportInfo.codes.correctedModalities[i]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        String tripId = ValidatedDataManager.getInstance().generateTripId(currentTrip);
        ValidatedTrip validatedTrip = ValidatedDataManager.getInstance().getValidatedTrip(tripId);

        tvTripId.setText(ValidatedDataManager.getInstance().generateTripId(currentTrip));
        tvClassifierValue.setText(TransportInfo.codes.modalities[currentTrip.getSugestedModeOfTransport()]);
        tvRealValue.setText("");

        if(validatedTrip != null){
            checkbox.setColorFilter(Color.GREEN);
            tvRealValue.setText(TransportInfo.codes.modalities[validatedTrip.realMode]);
            tvBelowFilter.setText("AccelsBelowFilter: " + validatedTrip.accelsBelowFilter);
            tv03_06.setText("AccelsBetw03-06: " + validatedTrip.accelBetw_03_06);
            tv06_1.setText("AccelsBetw06-1: " + validatedTrip.accelBetw_06_1);
            tv1_3.setText("AccelsBetw1-3: " + validatedTrip.accelBetw_1_3);
            tv3_6.setText("AccelsBetw3-6: " + validatedTrip.accelBetw_3_6);
            tv6.setText("AccelsAbove6: " + validatedTrip.accelAbove_6);
        }

        //TODO GET PEER INFO

        // CLASSIFIER INFO
        String probabilities = "";
        if(currentTrip.getProbasDict() != null){
            for(Integer i : currentTrip.getProbasDict().keySet()){
                probabilities += "Mode:" + TransportInfo.codes.modalities[i] + " = " + currentTrip.getProbasDict().get(i) + "\n";
            }
        }
        tvClassifierProbs.setText(probabilities);





    }
}
