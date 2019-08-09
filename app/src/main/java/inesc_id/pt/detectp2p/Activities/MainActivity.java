package inesc_id.pt.detectp2p.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.ModeClassification.ActivityRecognitionService;
import inesc_id.pt.detectp2p.Adapters.LegValidationAdapter;
import inesc_id.pt.detectp2p.Adapters.TripDigestListAdapter;
import inesc_id.pt.detectp2p.ModeClassification.Classifier;
import inesc_id.pt.detectp2p.P2PNetwork.MySalut;
import inesc_id.pt.detectp2p.P2PNetwork.SalutManager;
import inesc_id.pt.detectp2p.P2PNetwork.TermiteBroadcastReceiver;
import inesc_id.pt.detectp2p.P2PNetwork.TermiteWifiManager;
import inesc_id.pt.detectp2p.P2PNetwork.WifiDirectBroadcastReceiver;
import inesc_id.pt.detectp2p.P2PNetwork.WifiDirectManager;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.ModeClassification.PersistentTripStorage;
import inesc_id.pt.detectp2p.ModeClassification.TripStateMachine;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTrip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripDigest;
import inesc_id.pt.detectp2p.Taks.RequestToServerTask;
import inesc_id.pt.detectp2p.TransportModeDetection;
import inesc_id.pt.detectp2p.Utils.FileUtil;

public class MainActivity extends AppCompatActivity implements SalutDataCallback {

    // CHOOSE WIFI DIRECT OR TERMITE MODE
    public static int MODE_TERMITE = 0;
    public static int MODE_WIFIDIRECT = 1;
    int mode = MODE_WIFIDIRECT;

    // WIFI DIRECT MANAGER
    WifiDirectManager wifiDirectManager;
    SalutManager salutManager;

    //Termite Objects
    ///////////////////////////
    private TermiteBroadcastReceiver termiteBroadcastReceiver;
    Intent myService;
    Intent wifiService;
    ///////////////////////////

    // UI VIEWS
    /////////////////////////////////
    private Button btTest;
    private Button btStartStop;
    private Button btLog;
    private Button btConnectPeers;
    private Button btTestRead;
    private Button btRequestServer;
    private ListView tripList;
    /////////////////////////////////

    // DATA
    ///////////////////////////
    private PersistentTripStorage persistentTripStorage;
    private ArrayList<FullTripDigest> tripDigestList = new ArrayList<>();
    private TripDigestListAdapter tripDigestAdapter;
    ///////////////////////////

    public MainActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //ACTIVITY DETECTION SERVICE INITIALIZATION
        myService = new Intent(getApplicationContext(), ActivityRecognitionService.class);
        startService(myService);

        salutManager = SalutManager.startSalutInstance(this);

        //INIT WIFI DIRECT OBJECTS
        //wifiDirectManager = WifiDirectManager.startInstance(this);

        if(mode == MODE_TERMITE && !TermiteWifiManager.isRunning()) {
            //startWifiService();
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

        btRequestServer = findViewById(R.id.btRequestServer);
        btRequestServer.setOnClickListener(buttonListener);

        TransportModeDetection.getInstance();
    }


    @Override
    public void onResume() {
        super.onResume();
        //wifiDirectManager.registerWifiReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        //wifiDirectManager.unregisterWifiReceiver();
        salutManager.stopService();
    }

    @Override
    public void onDataReceived(Object data) {
        Log.d("MainActivity", "Got DATA");
    }

    public void startWifiService(){
        new Thread() {
            public void run() {
                Log.d("WIFI-SERVICE", "STARTING INTENT");
                wifiService = new Intent(getApplicationContext(), TermiteWifiManager.class);
                startService(wifiService);
            }
        }.start();
    }

    private View.OnClickListener buttonListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            //TermiteWifiManager.getInstance().sendUpdate("HELLO PEER ");

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
                    //TermiteWifiManager.getInstance().sendUpdate("OLA");
                    salutManager.sendMessage();
                    break;
                case R.id.btTest:
                    FileUtil.copyAssets(getApplicationContext());
                    break;
                case R.id.btTestRead:
                    Classifier c = FileUtil.readClassifier(getApplicationContext(), "classifier1");
                    Log.d("FileUtil", "HASH=" + c.hashCode());
                    break;
                case R.id.btRequestServer:
                    //new RequestToServerTask().execute("9090");
                    salutManager.discoverAndRegisterToPeers();
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

        LegValidationAdapter adapter = new LegValidationAdapter(fullTrip, this);

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
