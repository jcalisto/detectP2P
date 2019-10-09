package inesc_id.pt.detectp2p.P2PManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.Activities.MainActivity;
import inesc_id.pt.detectp2p.Command.ClassificationUpdate;
import inesc_id.pt.detectp2p.P2PManager.DataModels.ModeInfo;
import inesc_id.pt.detectp2p.WifiDirectTasks.SendPredictionTask;
import inesc_id.pt.detectp2p.TransportModeDeterminer;

import static android.os.Looper.getMainLooper;

public class WifiDirectManager implements ConnectionInfoListener {

    public static WifiDirectManager instance;
    public static String TAG = "WifiDirectManager";

    String myName;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_detectP2Pdemo";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int SERVER_PORT = 8080;


    //TODO INCREASE INTERVAL
    public static final int SERVICE_DISCOVERY_INTERVAL = 40 * 1000;
    public static final int SEND_UPDATE_INTERVAL = 20 * 1000;

    //Wifi Direct Objects
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver wifiDirectReceiver;
    private IntentFilter intentFilter;
    private WifiDirectHandler wifiDirectHandler;


    private ArrayList<String> connectedPeersAdresses = new ArrayList<>();
    ////////////////////////////////////////////////////////////////////////////////////////////////

    MainActivity mainActivity;

    // STRUCTS TO HOLD PEER SERVICE INFO
    private WifiP2pDeviceList currentPeerList;
    private final HashMap<String, String> buddies = new HashMap<String, String>();
    private ArrayList<WifiP2pDevice> servicePeers = new ArrayList<>();
    ////////////////////////////////////

    private final Handler serviceBroadcastingHandler = new Handler();
    private final Handler serviceDiscoveryHandler = new Handler();
    private final Handler classificationBroadcastHandler = new Handler();


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
        wifiDirectHandler = new WifiDirectHandler(manager, channel);
        wifiDirectHandler.execute();



        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        myName = Settings.Secure.getString(activity.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        startRegistrationAndDiscovery();

        classificationBroadcastHandler.postDelayed(
                classificationBroadcastRunnable,
                SEND_UPDATE_INTERVAL);


    }

    public WifiP2pDeviceList getCurrentPeerList() {
        return currentPeerList;
    }

    public void setCurrentPeerList(WifiP2pDeviceList currentPeerList) {
        this.currentPeerList = currentPeerList;

        for(WifiP2pDevice device : currentPeerList.getDeviceList()) {
            Log.d(TAG, "Start connection to device " + device.deviceName);
            connectToDevice(device);
        }
    }

    public void removeConnectedPeer(String address){
        if(connectedPeersAdresses.contains(address))
            connectedPeersAdresses.remove(address);
    }

    public void broadcastCurrentMode(){
        ModeInfo currentMode = TransportModeDeterminer.getInstance().getClassifierModeInfo();

        if(currentMode == null) currentMode = ModeInfo.getTestModeInfo();
        for(String peerAddress : connectedPeersAdresses) {
            Log.d(TAG, "Broadcasting mode to " + peerAddress);
            new SendPredictionTask(peerAddress, new ClassificationUpdate(currentMode.getDetectedMode(),
                    currentMode.getProbasDicts(), myName)).execute();
        }
    }


    //CONNECTION LISTENER
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        Log.d(TAG, "onConnectionInfoAvailable: starting");
        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "onConnectionInfoAvailable: This device is group owner");
        } else if (info.groupFormed) {
            //This is client
            Log.d(TAG, "onConnectionInfoAvailable: This device is client");
            connectedPeersAdresses.add(info.groupOwnerAddress.getHostAddress());
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////    METHODS TO BROADCAST AND DISCOVER SERVICES ///////////////////////////////

    // WifiP2PDevices available in currentPeerList

    private void startRegistrationAndDiscovery() {
        Log.d(TAG, "Start registration and discovery");
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "DetectP2PDevice" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        final WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);


                manager.addLocalService(channel, service,
                        new ActionListener() {

                            @Override
                            public void onSuccess() {
                                // service broadcasting started
                                Log.d(TAG, "onSuccess: ADD LOCAL SERVICE");
                                serviceBroadcastingHandler.postDelayed(serviceBroadcastingRunnable, SERVICE_DISCOVERY_INTERVAL);
                            }

                            @Override
                            public void onFailure(int error) {
                                Log.d(TAG, "onFailure: ADD LOCAL SERVICES + " + error);
                            }
                        });


        discoverService();
    }

    private void discoverService() {

        Log.d(TAG, "Prepare service discovery");
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

                Log.d(TAG, "Found instance = " + instanceName);
                if(instanceName.equals(SERVICE_INSTANCE)) {
                    resourceType.deviceName = buddies
                            .containsKey(resourceType.deviceAddress) ? buddies
                            .get(resourceType.deviceAddress) : resourceType.deviceName;
                    servicePeers.add(resourceType);
                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                    Log.d(TAG, "BUDDIES COUNT: " + buddies.size());
                    connectToDevice(resourceType);
                }


            }
        };

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        startServiceDiscovery();
    }

    public void startServiceDiscovery(){

        Log.d(TAG, "Start service discovery");
        manager.removeServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        Log.d(TAG, "onSuccess: removed service request");
                        manager.addServiceRequest(channel, serviceRequest,
                                new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "onSuccess: added Service request");
                                        manager.discoverServices(channel,
                                                new WifiP2pManager.ActionListener() {

                                                    @Override
                                                    public void onSuccess() {
                                                        Log.d(TAG, "onSuccess: Service discovery started");
                                                        //service discovery started

                                                        serviceDiscoveryHandler.postDelayed(
                                                                mServiceDiscoveringRunnable,
                                                                SERVICE_DISCOVERY_INTERVAL);
                                                    }

                                                    @Override
                                                    public void onFailure(int error) {
                                                        Log.d(TAG, "onFailure: Service discovery");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailure(int error) {
                                        // react to failure of adding service request
                                    }
                                });
                    }

                    @Override
                    public void onFailure(int reason) {
                        // react to failure of removing service request
                    }
                });
    }

    public void registerWifiReceiver() {
        mainActivity.registerReceiver(wifiDirectReceiver, intentFilter);
    }

    public void unregisterWifiReceiver() {
        mainActivity.unregisterReceiver(wifiDirectReceiver);
    }



    public void connectToDevice(final WifiP2pDevice device){
        if(connectedPeersAdresses.contains(device.deviceAddress)) {
            Log.d(TAG, "Device " + device.deviceName + " already connected");
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Connected to new device " + device.deviceName);
                connectedPeersAdresses.add(device.deviceAddress);
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                       RUNNABLES
    private Runnable serviceBroadcastingRunnable = new Runnable() {
        @Override
        public void run() {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Discover Peers Success");
                }

                @Override
                public void onFailure(int error) {
                    Log.d(TAG, "Discover Peers Failed!");
                }
            });
            serviceBroadcastingHandler
                    .postDelayed(serviceBroadcastingRunnable, SERVICE_DISCOVERY_INTERVAL);
        }
    };


    private Runnable mServiceDiscoveringRunnable = new Runnable() {
        @Override
        public void run() {
            startServiceDiscovery();
        }
    };

    private Runnable classificationBroadcastRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Start to broadcast current mode");

           broadcastCurrentMode();

            classificationBroadcastHandler.postDelayed(
                    classificationBroadcastRunnable,
                    SEND_UPDATE_INTERVAL);
        }
    };


    ////////////////////////////////////////////////////////////////////////////////////////////////
}
