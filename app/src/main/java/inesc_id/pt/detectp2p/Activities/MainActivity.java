package inesc_id.pt.detectp2p.Activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import inesc_id.pt.detectp2p.ModeClassification.ActivityRecognitionService;
import inesc_id.pt.detectp2p.Adapters.LegValidationAdapter;
import inesc_id.pt.detectp2p.Adapters.TripDigestListAdapter;
import inesc_id.pt.detectp2p.ModeClassification.Classifier;
import inesc_id.pt.detectp2p.P2PNetwork.P2pBroadcastReceiver;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.ModeClassification.PersistentTripStorage;
import inesc_id.pt.detectp2p.ModeClassification.TripStateMachine;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTrip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripDigest;
import inesc_id.pt.detectp2p.P2PNetwork.WifiDirectService;
import inesc_id.pt.detectp2p.Utils.FileUtil;

public class MainActivity extends AppCompatActivity {
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private Channel mChannel;
    private P2pBroadcastReceiver receiver;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    private PersistentTripStorage persistentTripStorage;

    private ArrayList<FullTripDigest> tripDigestList = new ArrayList<>();

    private TripDigestListAdapter tripDigestAdapter;

    Intent myService;
    Intent wifiService;

    //Views
    private Button btTest;
    private Button btStartStop;
    private Button btLog;
    private Button btConnectPeers;
    private Button btTestRead;

    private ListView tripList;

    public MainActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //SERVICE INITIALIZATION
        myService = new Intent(getApplicationContext(), ActivityRecognitionService.class);
        startService(myService);

        if (!WifiDirectService.isRunning()) {
            startWifiService();
        }
        persistentTripStorage = new PersistentTripStorage(getApplicationContext());

        tripDigestList = persistentTripStorage.getAllFullTripDigestsObjects();

        Log.d("Main Activity", "Trip Count = " + tripDigestList.size());

        ListView tripList = findViewById(R.id.tripsList);

        tripDigestAdapter = new TripDigestListAdapter(getApplicationContext(), tripDigestList);

        tripList.setAdapter(tripDigestAdapter);

        tripList.setOnItemClickListener(itemClickListener);

        btStartStop = findViewById(R.id.btStartStop);

        btStartStop.setOnClickListener(buttonListener);

        btLog = findViewById(R.id.btLog);
        btLog.setOnClickListener(buttonListener);

        btConnectPeers = findViewById(R.id.btConnectPeers);
        btConnectPeers.setOnClickListener(buttonListener);

        btTest = findViewById(R.id.btTest);
        btTest.setOnClickListener(buttonListener);

        btTestRead = findViewById(R.id.btTestRead);
        btTestRead.setOnClickListener(buttonListener);





    }


    @Override
    public void onResume() {
        super.onResume();
        /*if (!WifiDirectService.isRunning()) {
            startWifiService();
        }*/
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void startWifiService(){
        new Thread() {
            public void run() {
                Log.d("WIFI-SERVICE", "STARTING INTENT");
                wifiService = new Intent(getApplicationContext(), WifiDirectService.class);
                startService(wifiService);
            }
        }.start();
    }

    private View.OnClickListener buttonListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            //WifiDirectService.getInstance().sendUpdate("HELLO PEER ");

            switch (view.getId()){
                case R.id.btStartStop:
                    Log.d("Main Activity", "Force Trip Start/Stop with current state=" + TripStateMachine.getInstance(getApplicationContext(), false, true).currentState);
                    if(TripStateMachine.getInstance(getApplicationContext(), false, true).currentState == TripStateMachine.state.still){
                        TripStateMachine.getInstance(getApplicationContext(), false, true).forceStartTrip();
                        btStartStop.setText("STOP TRIP");
                    }else{
                        TripStateMachine.getInstance(getApplicationContext(), false, true).forceFinishTrip(false);
                        btStartStop.setText("START TRIP");
                    }
                    break;
                case R.id.btRedraw:
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                    break;
                case R.id.btLog:
                    FileUtil.startWriteToLog();
                    break;
                case R.id.btConnectPeers:
                    WifiDirectService.getInstance().sendUpdate("OLA");
                    break;
                case R.id.btTest:
                    FileUtil.writeClassifier(getApplicationContext());
                    break;
                case R.id.btTestRead:
                    Classifier c = FileUtil.readClassifier(getApplicationContext(), "classifier1");
                    Log.d("FileUtil", "HASH=" + c.hashCode());
                    break;

            }
        }
    };

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FullTripDigest fullTripDigest = (FullTripDigest) adapterView.getItemAtPosition(i);

            FullTrip fullTrip = persistentTripStorage.getFullTripByDate(fullTripDigest.getTripID());
            Log.d("LegAdapter", "Getting full trip with ID=" + fullTripDigest.getTripID());
            if(fullTrip==null)
                Log.d("LegAdapter", "FULL TRIP NULL");
            showLegClassificationPopup(fullTrip);
        }
    };

    public void showLegClassificationPopup(FullTrip fullTrip) {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.trip_classified_popup, null);

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();

        final ListView legList = mView.findViewById(R.id.legList);
        final Button doneBt = mView.findViewById(R.id.doneBt);

        LegValidationAdapter adapter = new LegValidationAdapter(fullTrip, getApplicationContext());

        legList.setAdapter(adapter);

        doneBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

}
