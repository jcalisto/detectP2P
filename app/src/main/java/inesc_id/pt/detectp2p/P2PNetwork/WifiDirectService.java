package inesc_id.pt.detectp2p.P2PNetwork;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.Command.CliCommand;
import inesc_id.pt.detectp2p.Command.CliUpdateCommand;
import inesc_id.pt.detectp2p.Command.CommandCliHandlerImpl;
import inesc_id.pt.detectp2p.Response.CliResponse;
import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;


public class WifiDirectService extends Service implements SimWifiP2pManager.PeerListListener, SimWifiP2pManager.GroupInfoListener{

    final static int PORT = 10001;

    private SimWifiP2pManager mManager = null;
    private SimWifiP2pManager.Channel mChannel = null;
    private Messenger mService = null;
    private boolean mBound = false;
    private SimWifiP2pSocketServer mSrvSocket = null;
    private SimWifiP2pSocket mCliSocket = null;

    private P2pBroadcastReceiver receiver;
    private Map<String, String> peersByName = new HashMap<String, String>();
    private static WifiDirectService instance;

    private CommandCliHandlerImpl handler;

    private String myName;

    public WifiDirectService(){
        instance = this;
    }

    public static WifiDirectService getInstance(){
        return instance;
    }



    @Override
    public int onStartCommand(Intent t, int f, int sid){
        initializeWifiDirect();
        Intent intent = new Intent(getBaseContext(), SimWifiP2pService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
        handler = new CommandCliHandlerImpl();

        new IncommingCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            mManager.requestPeers(mChannel, WifiDirectService.this);
    }

    public void updateGroup(){
        Log.d("WIFI-SERVICE", "Request Group Update");
        if (mBound)
            mManager.requestGroupInfo(mChannel, WifiDirectService.this);
    }

    private void initializeWifiDirect() {
        SimWifiP2pSocketManager.Init(getBaseContext());

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        receiver = new P2pBroadcastReceiver(WifiDirectService.this);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList deviceList, SimWifiP2pInfo simWifiP2pInfo) {

        peersByName.clear();

        myName = simWifiP2pInfo.getDeviceName();

        for (SimWifiP2pDevice device : deviceList.getDeviceList()) {
            if (device.deviceName.equals(myName)) {
                continue;
            }
            peersByName.put(device.deviceName, device.getVirtIp());
            Log.d("WIFI-SERVICE", "onGroupInfoAvailable: added " + device.deviceName);
        }
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList deviceList) {
        //TODO something
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
                        CliCommand cmd = null;
                        try {
                            cmd = (CliCommand) ois.readObject();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        CliResponse cliResponse= cmd.handle(handler);
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


    public class SendUpdateTask extends AsyncTask<String, String, String> {
        private String update;

        public SendUpdateTask(String update){
            this.update = update;
        }

        @Override
        protected String doInBackground(String[] params) {
            try {
                mCliSocket = new SimWifiP2pSocket(params[0], PORT);
                CliUpdateCommand command = new CliUpdateCommand(update);
                ObjectOutputStream oos = new ObjectOutputStream(mCliSocket.getOutputStream());
                oos.writeObject(command);
                Log.d("WIFI-SERVICE", "Sent update!");
                ObjectInputStream ois = new ObjectInputStream(mCliSocket.getInputStream());

                mCliSocket.close();
                mCliSocket = null;
                return "ACK";
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCliSocket = null;
            return null;
        }

    }


    //////////////// COMMUNICATION METHODS /////////////////////
    public void sendUpdate(String update){
        for (Map.Entry<String, String> entry : peersByName.entrySet()) {
            Log.d("WIFI-SERVICE", "Sending update to " + entry.getKey());
            new SendUpdateTask(update).execute(entry.getValue());
        }
    }
}
