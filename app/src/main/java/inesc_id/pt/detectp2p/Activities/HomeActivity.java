package inesc_id.pt.detectp2p.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ramimartin.multibluetooth.activity.BluetoothActivity;
import com.ramimartin.multibluetooth.bluetooth.manager.BluetoothManager;

import java.util.ArrayList;
import java.util.Random;

import inesc_id.pt.detectp2p.Activities.Fragments.HomeFragment;
import inesc_id.pt.detectp2p.Activities.Fragments.LegResult;
import inesc_id.pt.detectp2p.Activities.Fragments.LegsFragment;
import inesc_id.pt.detectp2p.Activities.Fragments.TripsFragment;
import inesc_id.pt.detectp2p.P2PManager.Bluetooth.BluetoothPeer;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripDetection.ActivityRecognitionService;
import inesc_id.pt.detectp2p.TripDetection.PersistentTripStorage;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTrip;
import inesc_id.pt.detectp2p.TripDetection.dataML.Trip;

public class HomeActivity extends BluetoothActivity {

    // CHOOSE WIFI DIRECT OR TERMITE MODE
    public static int MODE_TERMITE = 0;
    public static int MODE_WIFIDIRECT = 1;
    int mode = MODE_WIFIDIRECT;

    int myID = 0;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    Intent myService;

    // UI VIEWS
    /////////////////////////////////

    private Button btTripList;
    private Button btPeerList;
    private Button btValidationList;
    private ImageView btHome;
    private TextView tvDetectionMode;
    private TextView tvCurrentState;
    private FrameLayout frameLayout;

    /////////////////////////////////

    // DATA
    ///////////////////////////
    private PersistentTripStorage persistentTripStorage;

    private ArrayList<BluetoothPeer> peerList = new ArrayList<>();
    ///////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_fragments);


        //ACTIVITY DETECTION SERVICE INITIALIZATION
        myService = new Intent(getApplicationContext(), ActivityRecognitionService.class);
        startService(myService);

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

        persistentTripStorage = new PersistentTripStorage(getApplicationContext());

        //SET VIEWS
       btTripList = findViewById(R.id.btTripList);
       btPeerList = findViewById(R.id.btPeerList);
       btValidationList = findViewById(R.id.btValidationList);
       btHome = findViewById(R.id.btHome);
       tvDetectionMode = findViewById(R.id.tvDetectionMode);
       tvCurrentState = findViewById(R.id.tvCurrentState);
       frameLayout = findViewById(R.id.frameLayout);

       btTripList.setOnClickListener(buttonListener);
       btPeerList.setOnClickListener(buttonListener);
       btValidationList.setOnClickListener(buttonListener);
       btHome.setOnClickListener(buttonListener);


        Fragment newFragment = new HomeFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, newFragment);
        transaction.addToBackStack("HOME");

        transaction.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btTripList:
                    Fragment newFragment = new TripsFragment();
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.frameLayout, newFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                    break;
                case R.id.btHome:
                    getFragmentManager().popBackStack("HOME", 0);
                case R.id.btPeerList:

                    break;
                case R.id.btValidationList:

                    break;
            }
        }
    };

    public void openTrip(FullTrip trip){
        Fragment newFragment = new LegsFragment();
        ((LegsFragment) newFragment).setTrip(trip);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void openLeg(Trip leg){
        Fragment newFragment = new LegResult();
        ((LegResult) newFragment).setLeg(leg);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    ///////////////////////////////////////// BLUETOOTH //////////////////////////////////////////

    public void newPeer(BluetoothDevice device){
        for(BluetoothPeer peer : peerList){
            if(peer.device.getAddress() == device.getAddress()){
                return;
            }
        }
        peerList.add(new BluetoothPeer(device, "NOT CONNECTED", System.currentTimeMillis()));
    }

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
