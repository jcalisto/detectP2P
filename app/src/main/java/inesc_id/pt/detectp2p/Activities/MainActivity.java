package inesc_id.pt.detectp2p.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import inesc_id.pt.detectp2p.ActivityRecognitionService;
import inesc_id.pt.detectp2p.Adapters.TripDigestListAdapter;
import inesc_id.pt.detectp2p.P2pBroadcastReceiver;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripStateMachine.PersistentTripStorage;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.FullTripDigest;
import inesc_id.pt.detectp2p.WifiDirectService;

public class MainActivity extends Activity {
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private Channel mChannel;
    private P2pBroadcastReceiver receiver;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    private PersistentTripStorage persistentTripStorage;

    private ArrayList<FullTripDigest> tripDigestList = new ArrayList<>();

    private TripDigestListAdapter tripDigestAdapter;

    Intent myService;

    //Views
    private Button button;
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

        ListView tripList = findViewById(R.id.tripsList);

        tripDigestAdapter = new TripDigestListAdapter(getApplicationContext(), tripDigestList);

        tripList.setAdapter(tripDigestAdapter);


        button = findViewById(R.id.button);
        button.setOnClickListener(buttonListener);



    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void startWifiService(){
        new Thread() {
            public void run() {
                Log.d("WIFI-SERVICE", "STARTING INTENT");
                Intent i = new Intent(getApplicationContext(), WifiDirectService.class);
                startService(i);
            }
        }.start();
    }

    private View.OnClickListener buttonListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            WifiDirectService.getInstance().sendUpdate("HELLO PEER ");
        }
    };
}
