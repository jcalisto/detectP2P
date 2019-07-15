package inesc_id.pt.detectp2p.P2PNetwork;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.Activities.MainActivity;

import static android.os.Looper.getMainLooper;

public class WifiDirectManager {

    public static WifiDirectManager instance;
    public static String TAG = "WifiDirectManager";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_detectP2Pdemo";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int SERVER_PORT = 8080;

    //Wifi Direct Objects
    ///////////////////////////
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver wifiDirectReceiver;
    IntentFilter intentFilter;
    ///////////////////////////

    MainActivity mainActivity;

    // STRUCTS TO HOLD PEER SERVICE INFO
    WifiP2pDeviceList currentPeerList;
    final HashMap<String, String> buddies = new HashMap<String, String>();
    ArrayList<WifiP2pDevice> servicePeers = new ArrayList<>();
    ////////////////////////////////////

    private WifiP2pDnsSdServiceRequest serviceRequest;

    public static WifiDirectManager startInstance(MainActivity activity){
        if(instance == null) {
            instance = new WifiDirectManager(activity);
        }
        return instance;
    }

    public static WifiDirectManager getInstance(){
        return instance;
    }


    private WifiDirectManager(MainActivity activity){
        mainActivity = activity;
        manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, getMainLooper(), null);
        wifiDirectReceiver = new WifiDirectBroadcastReceiver(manager, channel, activity);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        startRegistrationAndDiscovery();
    }

    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "DetectP2PDevice" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Added Local Service");
            }
            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Added Local Service");
            }
        });
        discoverService();
    }

    private void discoverService() {
        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
            @Override
            /* Callback includes:
             * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
             * record: TXT record dta as a map of key/value pairs.
             * device: The device running the advertised service.
             */

            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
                buddies.put(device.deviceAddress, (String) record.get("buddyname"));
            }
        };

        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                if(instanceName.equals(SERVICE_INSTANCE)) {
                    resourceType.deviceName = buddies
                            .containsKey(resourceType.deviceAddress) ? buddies
                            .get(resourceType.deviceAddress) : resourceType.deviceName;
                    servicePeers.add(resourceType);
                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                }


            }
        };

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        manager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Added service discovery request");
                    }
                    @Override
                    public void onFailure(int arg0) {
                        Log.d(TAG, "Failed to add service discovery request");
                    }
                });

        manager.discoverServices(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
            }
            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery failed");
            }
        });
    }

    public void registerWifiReceiver() {
        mainActivity.registerReceiver(wifiDirectReceiver, intentFilter);
    }

    public void unregisterWifiReceiver() {
        mainActivity.unregisterReceiver(wifiDirectReceiver);
    }

    public void discoverPeers(){
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discover peers - SUCCESS");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Discover peers - FAILURE");
            }
        });
    }

    public WifiP2pDeviceList getCurrentPeerList() {
        return currentPeerList;
    }

    public void setCurrentPeerList(WifiP2pDeviceList currentPeerList) {
        this.currentPeerList = currentPeerList;
    }

    public void connectToPeers(){
        for(WifiP2pDevice device : currentPeerList.getDeviceList()){
            connectToDevice(device);
        }
    }

    public void connectToDevice(final WifiP2pDevice device){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Connected to new device " + device.deviceName);
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
            }
        });
    }
}
