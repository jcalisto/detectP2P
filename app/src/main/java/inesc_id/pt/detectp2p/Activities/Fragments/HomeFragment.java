package inesc_id.pt.detectp2p.Activities.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripDetection.TripStateMachine;
import inesc_id.pt.detectp2p.Utils.FileUtil;

public class HomeFragment extends Fragment {

    Button btStartStopTrip;
    Button logToFile;
    Handler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        btStartStopTrip = (Button) view.findViewById(R.id.startStopTrip);
        logToFile = (Button) view.findViewById(R.id.btLogToFile);

        btStartStopTrip.setOnClickListener(buttonListener);
        logToFile.setOnClickListener(buttonListener);

        handler = new Handler();

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable(){
            public void run(){
                //do something
                updateState();


                handler.postDelayed(this, 30000);
            }
        }, 15000);
    }

    private View.OnClickListener buttonListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            switch (view.getId()){
                case R.id.startStopTrip:
                   if(TripStateMachine.getInstance(getActivity(), false, true).currentState == TripStateMachine.state.still){
                        TripStateMachine.getInstance(getActivity(), false, true).forceStartTrip();
                        btStartStopTrip.setText("Stop Trip");
                    }else{
                        TripStateMachine.getInstance(getActivity(), false, true).forceFinishTrip(false);
                        btStartStopTrip.setText("Start Trip");
                    }
                    break;

                case R.id.btLogToFile:
                    FileUtil.startWriteToLog();
                    break;
            }
        }
    };

    private void updateState(){
        TripStateMachine.state state = TripStateMachine.getInstance(getActivity(), false, true).currentState;
        try {
            switch (state.getStateInt()) {
                case 0:
                    btStartStopTrip.setText("Start Trip");
                    break;
                case 1:
                    btStartStopTrip.setText("Stop Trip");
                    break;
                case 2:
                    btStartStopTrip.setText("Stop Trip");
                    break;
            }
        } catch(Exception e){
            Log.e("HomeFragment", "Error: " + e.getMessage());
        }
    }

}
