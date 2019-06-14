package inesc_id.pt.detectp2p;

import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.ModeClassification.dataML.MLInputMetadata;
import inesc_id.pt.detectp2p.P2PNetwork.DataModels.ModeInfo;
import inesc_id.pt.detectp2p.P2PNetwork.DataModels.PeerInfo;

public class TransportModeDetection {

    public static TransportModeDetection instance;

    private static final String TAG = "TransportModeDetection";

    public static TransportModeDetection getInstance(){
        if (instance == null){ //if there is no instance available... create new one
            instance = new TransportModeDetection();
        }

        return instance;
    }

    public TransportModeDetection(){
        final Handler handler = new Handler();
        final int delay = 30 * 1000; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){
                //do something
                takeDecision();


                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    //Save peers by [deviceName, VirtualIP]
    private Map<String, String> peersByName = new HashMap<String, String>();

    //Peer info by name
    private Map<String, PeerInfo> peerInfoByName = new HashMap<String, PeerInfo>();

    //Local Classifier Info
    private ModeInfo classifierModeInfo;


    synchronized  public void updateStoredPeerInformation(String peerName, int modeKey, HashMap<Integer, Double> probabilityDict){
        PeerInfo peerInfo;

        if(peerInfoByName.containsKey(peerName)){
            peerInfo = peerInfoByName.get(peerName);
        }
        else {
            peerInfo = new PeerInfo(peerName);
            peerInfoByName.put(peerName, peerInfo);
        }

        ModeInfo modeInfo = new ModeInfo(probabilityDict);
        peerInfo.addModeInfo(modeInfo);
    }

    synchronized  public void setCurrentModeInfo(ModeInfo currentModeInfo){
        Log.d(TAG, "Updating current mode info");
        this.classifierModeInfo = currentModeInfo;
    }

    private void takeDecision() {
        //TODO
        Log.d(TAG, "Taking decision");

    }





}
