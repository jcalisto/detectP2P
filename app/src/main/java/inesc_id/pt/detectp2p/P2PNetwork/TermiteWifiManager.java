package inesc_id.pt.detectp2p.P2PNetwork;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import inesc_id.pt.detectp2p.Command.ClassificationUpdate;
import inesc_id.pt.detectp2p.Command.Command;
import inesc_id.pt.detectp2p.Command.UpdateCommand;
import inesc_id.pt.detectp2p.Command.CommandCliHandlerImpl;
import inesc_id.pt.detectp2p.P2PNetwork.DataModels.ModeInfo;
import inesc_id.pt.detectp2p.Response.CliResponse;
import inesc_id.pt.detectp2p.Taks.SendPredictionTask;
import inesc_id.pt.detectp2p.Taks.SendUpdateTask;
import inesc_id.pt.detectp2p.TransportModeDetection;
import inesc_id.pt.detectp2p.Utils.FileUtil;
import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;


public class TermiteWifiManager extends Service implements SimWifiP2pManager.PeerListListener, SimWifiP2pManager.GroupInfoListener{

    final static int PORT = 10001;

    private SimWifiP2pManager mManager = null;
    private SimWifiP2pManager.Channel mChannel = null;
    private Messenger mService = null;
    private boolean mBound = false;
    private SimWifiP2pSocketServer mSrvSocket = null;
    private SimWifiP2pSocket mCliSocket = null;

    private TermiteBroadcastReceiver receiver;
    private static TermiteWifiManager instance;

    private CommandCliHandlerImpl handler;

    private String myName;

    //Device Name -> Virtual IP
    private Map<String, String> peersByName = new HashMap<String, String>();

    public TermiteWifiManager(){
        instance = this;
    }

    public static TermiteWifiManager getInstance(){
        return instance;
    }


    private ModeInfo currentModeInfo;




    @Override
    public int onStartCommand(Intent t, int f, int sid){
        initializeWifiDirect();
        Intent intent = new Intent(getBaseContext(), SimWifiP2pService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
        handler = new CommandCliHandlerImpl();

        //Set unique device name
        //Used to keep track of each peer
        myName = UUID.randomUUID().toString();

        TransportModeDetection.getInstance();

        new IncommingCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        final Handler handler = new Handler();
        final int delay = 60 * 1000; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){
                if(currentModeInfo != null) {
                    Log.d("SendCurrentModeUpdate", "Start to send current mode update!");
                    sendCurrentDetectedMode(currentModeInfo);
                } else {
                    Log.d("SendCurrentModeUpdate", "Can't send update, no info available");
                }
                handler.postDelayed(this, delay);
            }
        }, delay);

        Log.d("WIFI-SERVICE", "INITIALIZED");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(mBound){
            Log.d("WIFI-SERVICE","INSIDE BOUND");
            unbindService(mConnection);
            mBound=false;
            unregisterReceiver(receiver);
        }
        instance=null;
        Log.d("WIFI-SERVICE", "Service Destroyed");
    }



    public void updatePeers(){
        Log.d("WIFI-SERVICE", "Request Peer Update");
        if(mBound)
            mManager.requestPeers(mChannel, TermiteWifiManager.this);
    }

    public void updateGroup(){
        Log.d("WIFI-SERVICE", "Request Group Update");
        if (mBound)
            mManager.requestGroupInfo(mChannel, TermiteWifiManager.this);
    }

    private void initializeWifiDirect() {
        SimWifiP2pSocketManager.Init(getBaseContext());

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        receiver = new TermiteBroadcastReceiver(TermiteWifiManager.this);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList deviceList, SimWifiP2pInfo simWifiP2pInfo) {
        peersByName.clear();

        myName = simWifiP2pInfo.getDeviceName();

        for (SimWifiP2pDevice device : deviceList.getDeviceList()) {
            peersByName.put(device.deviceName, device.getVirtIp());
            Log.d("WIFI-SERVICE", "onGroupInfoAvailable: added " + device.deviceName);
        }
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList deviceList) {
        peersByName.clear();

        for (SimWifiP2pDevice device : deviceList.getDeviceList()) {
            peersByName.put(device.deviceName, device.getVirtIp());
            Log.d("WIFI-SERVICE", "Peer List updated - added " + device.deviceName);
        }
    }


    public static boolean isRunning(){
        return instance != null;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        // callbacks for service binding, passed to bindService()

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            mManager = new SimWifiP2pManager(mService);
            mChannel = mManager.initialize(getApplication(), getMainLooper(),
                    null);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mManager = null;
            mChannel = null;
            mBound = false;
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void setCurrentModeInfo(ModeInfo currentModeInfo){
        Log.d("WifiService", "Updating current mode info");
        this.currentModeInfo = currentModeInfo;
    }


    ///////////////////// COMMUNICATION TASKS //////////////////
    public class IncommingCommTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.d("WifiService", "IncommingCommTask started (" + this.hashCode() + ").");

            try {
                mSrvSocket = new SimWifiP2pSocketServer(PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SimWifiP2pSocket sock = mSrvSocket.accept();
                    Log.d("WIFI-SERVICE", "Received Request");
                    try {
                        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                        Command cmd = null;
                        try {
                            cmd = (Command) ois.readObject();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        CliResponse cliResponse = null;
                        if(cmd != null && cmd instanceof UpdateCommand) {
                            UpdateCommand update = (UpdateCommand) cmd;
                            cliResponse = handler.handle(update);
                        }
                        Log.d("WIFI-SERVICE", "Received Communication");

                        ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                        oos.writeObject(cliResponse);
                    } catch (IOException e) {
                        Log.d("Error reading socket:", e.getMessage());
                    } finally {
                        sock.close();
                    }
                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    break;
                }
            }
            Log.d("WIFI-SERVICE", "Ended INCOMM TASK");
            return null;
        }
    }

    //////////////// COMMUNICATION METHODS /////////////////////
    //Method to send new classifier to peers
    public void sendUpdate(String update){

        UpdateCommand command = new UpdateCommand("classifier_v2", FileUtil.readClassifierToBytes());

        for (Map.Entry<String, String> entry : peersByName.entrySet()) {
            Log.d("WIFI-SERVICE", "Sending update to " + entry.getKey());
            new SendUpdateTask(command).execute(entry.getValue());
        }
    }


    //Method to send current detected mode to every peer
    public void sendCurrentDetectedMode(ModeInfo currentModeInfo){

        ClassificationUpdate classificationUpdate = new ClassificationUpdate(currentModeInfo.getDetectedMode(),
                                                        currentModeInfo.getProbasDicts(),
                                                        myName);

        for (Map.Entry<String, String> entry : peersByName.entrySet()) {
            Log.d("WIFI-SERVICE", "Sending update to " + entry.getKey());
            new SendPredictionTask(classificationUpdate).execute(entry.getValue());
        }


    }

}
