package inesc_id.pt.detectp2p.P2PNetwork;


import android.content.Context;
import android.util.Log;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import java.util.HashMap;
import java.util.Random;

public class SalutManager implements SalutDataCallback {

    static SalutManager instance;
    SalutDataReceiver dataReceiver;
    SalutServiceData serviceData;
    
    HashMap<String, SalutDevice> salutDeviceHostMap; 

    private static String TAG = "SalutManager";

    private String id;
    private Salut network;

    private SalutManager(Context context){


        id = "DetectP2P_" + (new Random().nextInt(20000-1) + 1);
        Log.d(TAG, "ID = " + id + ", realId=" + id.substring(10));

        dataReceiver = new SalutDataReceiver(context, this);
        serviceData = new SalutServiceData("detectP2P", 9090, id);

        salutDeviceHostMap = new HashMap<>();



        network = new MySalut(dataReceiver, serviceData, new SalutCallback() {
            @Override
            public void call() {
                Log.e("MainActivity", "Sorry, but this device does not support WiFi Direct.");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        network.startNetworkService(new SalutDeviceCallback() {
            @Override
            public void call(SalutDevice device) {
                Log.d(TAG, "---- startNetworkService callback::::: " + device.readableName + " has connected!");

            }
        }, new SalutCallback() {
            @Override
            public void call() {
                Log.d(TAG, "Start Service - SUCCESS");
                discoverAndRegisterToPeers();
            }
        }, new SalutCallback() {
            @Override
            public void call() {
                Log.d(TAG, "Start Service - FAILURE");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }





    }

    public void discoverAndRegisterToPeers() {
        Log.d(TAG, "new discover and register routine");

        network.discoverNetworkServices(new SalutDeviceCallback() {
            @Override
            public void call(SalutDevice device) {
                Log.d(TAG, "--- discoverNetworkServices: D_NAME=" + device.serviceName + ", D_SERVICE=" + device.serviceName +
                        ", D_Real_Name=" + device.readableName);
                if(!salutDeviceHostMap.containsKey(device.deviceName) && !chooseHost(device.readableName)) {
                    Log.d(TAG, "--- Peer is chosen to host: " + device.readableName);

                        salutDeviceHostMap.put(device.deviceName, device);
                        Log.d(TAG, "--- Registering to host: " + device.readableName);
                        network.registerWithHost(device, new SalutCallback() {
                            @Override
                            public void call() {
                                Log.d(TAG, "---- We're now registered.");
                            }
                        }, new SalutCallback() {
                            @Override
                            public void call() {
                                Log.d(TAG, "---- We failed to register.");
                            }
                        });

                }
                discoverAndRegisterToPeers();

            }
        }, true);


    }

    public synchronized static SalutManager startSalutInstance(Context context) {
        if(instance == null){
            instance = new SalutManager(context);
        }
        return instance;

    }

    public static SalutManager getInstance(){
        return instance;
    }

    @Override
    public void onDataReceived(Object data) {
        Log.d(TAG, "On Data Received");
    }

    public void sendMessage(){
        Message myMessage = new Message();
        myMessage.description = "HELLO PEER!";

        Log.d(TAG, "Printing network stats:");
        Log.d(TAG, "found devices:" + network.foundDevices.size() );
        Log.d(TAG, "registered devices:" + network.registeredClients.size() );

        network.sendToAllDevices(myMessage, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! The data failed to send.");
            }
        });

    }

    public void stopService(){
        Log.d(TAG, "Stopping service from main activity");
        try {
            network.stopServiceDiscovery(true);
            network.stopNetworkService(false);
            network.unregisterClient(false);
        } catch (Exception e){
            Log.d(TAG, e.getMessage());
        }

    }

    public int getPeerCount(){
        return network.registeredClients.size();
    }

    //Choose host based on id. Highest number is host
    //Returns true if this is host
    private boolean chooseHost(String peerId){

        int peerRealId =  Integer.parseInt(peerId.substring(10));
        int myRealId = Integer.parseInt(id.substring(10));

        if (peerRealId > myRealId) {
            return false;
        }
        return true;
    }

}

@JsonObject
class Message{

    /*
     * Annotate a field that you want sent with the @JsonField marker.
     */
    @JsonField
    public String description;

    /*
     * Note that since this field isn't annotated as a
     * @JsonField, LoganSquare will ignore it when parsing
     * and serializing this class.
     */
    public int nonJsonField;
}
