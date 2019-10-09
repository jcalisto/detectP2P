package inesc_id.pt.detectp2p.Activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.peak.salut.Callbacks.SalutDataCallback;
import com.ramimartin.multibluetooth.activity.BluetoothActivity;
import com.ramimartin.multibluetooth.bluetooth.manager.BluetoothManager;


import java.util.ArrayList;
import java.util.Random;

import inesc_id.pt.detectp2p.Adapters.PeerListAdapter;
import inesc_id.pt.detectp2p.TransportModeDeterminer;
import inesc_id.pt.detectp2p.TripDetection.ActivityRecognitionService;
import inesc_id.pt.detectp2p.Adapters.LegValidationAdapter;
import inesc_id.pt.detectp2p.Adapters.TripDigestListAdapter;
import inesc_id.pt.detectp2p.TripDetection.Classifier;
import inesc_id.pt.detectp2p.P2PManager.Bluetooth.BluetoothPeer;
import inesc_id.pt.detectp2p.P2PManager.SalutManager;
import inesc_id.pt.detectp2p.P2PManager.TermiteBroadcastReceiver;
import inesc_id.pt.detectp2p.P2PManager.TermiteWifiManager;
import inesc_id.pt.detectp2p.P2PManager.WifiDirectManager;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripDetection.PersistentTripStorage;
import inesc_id.pt.detectp2p.TripDetection.TripStateMachine;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTrip;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTripDigest;
import inesc_id.pt.detectp2p.Utils.FileUtil;

public class MainActivity extends BluetoothActivity implements SalutDataCallback {

    // CHOOSE WIFI DIRECT OR TERMITE MODE
    public static int MODE_TERMITE = 0;
    public static int MODE_WIFIDIRECT = 1;
    int mode = MODE_WIFIDIRECT;

    int myID = 0;


    // WIFI DIRECT MANAGER
    WifiDirectManager wifiDirectManager;
    SalutManager salutManager;


    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

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

    private ArrayList<BluetoothPeer> peerList = new ArrayList<>();
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

        //salutManager = SalutManager.startSalutInstance(this);

        //BLUETOOTH
        Random r = new Random();
        myID = r.nextInt(2000 - 1) + 1;


        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC);
        selectServerMode();
        BluetoothAdapter.getDefaultAdapter().setName("detectP2P_" + myID);


        scanAllBluetoothDevice();


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



        TransportModeDeterminer.getInstance();
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
        //salutManager.stopService();
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

        LegValidationAdapter adapter = new LegValidationAdapter(fullTrip, this, this);

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

    public void showPeerListPopup() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.peer_listing, null);

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();

        final ListView peerListView = mView.findViewById(R.id.legList);
        final Button doneBt = mView.findViewById(R.id.doneBt);

        final Button updateBt = mView.findViewById(R.id.updateBt);

        final PeerListAdapter adapter = new PeerListAdapter(peerList, this);

        peerListView.setAdapter(adapter);

        doneBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        updateBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanAllBluetoothDevice();
                BluetoothAdapter.getDefaultAdapter().startDiscovery();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    public void newPeer(BluetoothDevice device){
        for(BluetoothPeer peer : peerList){
            if(peer.device.getAddress() == device.getAddress()){
                return;
            }
        }
        peerList.add(new BluetoothPeer(device, "NOT CONNECTED", System.currentTimeMillis()));
    }


    ///////////////////////////////////////// BLUETOOTH //////////////////////////////////////////

    @Override
    public String setUUIDappIdentifier() {
        return "f520cf2c-6487-11e7-907b";
    }

    @Override
    public int myNbrClientMax() {
        return 7;
    }


    @Override
    public void onBluetoothDeviceFound(BluetoothDevice bluetoothDevice) {
        String otherName = bluetoothDevice.getName();
        Log.d("BluetoothManager", "device found with Name=" + otherName + ",   Addr:" + bluetoothDevice.getAddress());

        newPeer(bluetoothDevice);

        if(otherName == null || !otherName.contains("detectP2P")) return;
        int otherID = Integer.parseInt(otherName.split("_")[1]);
        Log.d("BluetoothManager", "device found with ID=" + otherID);

        if(otherID > myID){ //Other device is server
            createClient(bluetoothDevice.getAddress());
        }
        else {              //This device is server
            //selectServerMode();
        }

    }

    @Override
    public void onClientConnectionSuccess() {
        Log.d("BluetoothManager", "Connected to device");
    }

    @Override
    public void onClientConnectionFail() {
        Log.d("BluetoothManager", "Connection failed");
    }

    @Override
    public void onServeurConnectionSuccess() {
        Log.d("BluetoothManager", "Connected to server");
    }

    @Override
    public void onServeurConnectionFail() {
        Log.d("BluetoothManager", "Failed to connect to server");
    }

    @Override
    public void onBluetoothStartDiscovery() {
        Log.d("BluetoothManager", "START DISCOVERY");
    }

    @Override
    public void onBluetoothMsgStringReceived(String s) {

    }

    @Override
    public void onBluetoothMsgObjectReceived(Object o) {

    }

    @Override
    public void onBluetoothMsgBytesReceived(byte[] bytes) {

    }

    @Override
    public void onBluetoothNotAviable() {
        Log.d("BluetoothManager", "NOT AVAILABLE");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                // TODO stuff if you need
            }
        }
    }
}
