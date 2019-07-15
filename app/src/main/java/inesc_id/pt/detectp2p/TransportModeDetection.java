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

        simulatePeerInformation();

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

    public void simulatePeerInformation(){
        HashMap<Integer, Double> testDict1 = new HashMap<>();

        testDict1.put(1, 0.0);
        testDict1.put(3, 0.0);
        testDict1.put(7, 0.00);
        testDict1.put(9, 0.45);
        testDict1.put(10, 0.23);
        testDict1.put(11, 0.35);
        updateStoredPeerInformation("test1", 9, testDict1);

        HashMap<Integer, Double> testDict2 = new HashMap<>();
        testDict2.put(1, 0.0);
        testDict2.put(3, 0.0);
        testDict2.put(7, 0.00);
        testDict2.put(9, 0.30);
        testDict2.put(10, 0.10);
        testDict2.put(11, 0.60);
        updateStoredPeerInformation("test2", 11, testDict2);

        int bicycle = 1;
        int still = 3;
        int walking = 7;
        int running = 8;
        int car = 9;
        int train = 10;
        int tram = 11;
        int bus = 15;
        int motorcycle = 20;
        int moped = 21;

    }


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


        int totalWeight = 0;
        HashMap<Integer, Double> totalProbabilityDict = new HashMap<>();

        //Iterate Peers
        for(String key : peerInfoByName.keySet()) {
            PeerInfo peerInfo = peerInfoByName.get(key);
            Log.d(TAG, "Iterate peer= " + peerInfo.getName());

            //Check if peer is still in close range
            if(System.currentTimeMillis() - peerInfo.getLastTimeStamp() < 40 * 1000) {
                Log.d(TAG, "--- Peer in close range");
                ModeInfo modeInfo = peerInfo.getModeInfoByTime().get(peerInfo.getTime());   //Get Last ModeInfo received
                HashMap<Integer, Double> peerProbasDict = modeInfo.getProbasDicts();
                int peerWeight = 1;
                if(key.equals("test2"))
                    peerWeight = 2;
                totalWeight += peerWeight;

                //Iterate the probability dictionary of the peer and add it to global dict
                Log.d(TAG, "--- Iterate probas dict");
                for(Integer modeKey : peerProbasDict.keySet()) {
                    Log.d(TAG, "----- Modekey=" + modeKey + ", value=" + peerProbasDict.get(modeKey));

                    sumValueToDictEntry(totalProbabilityDict, modeKey, peerWeight * peerProbasDict.get(modeKey));
                }

            }
        }

        for(Integer modeKey : totalProbabilityDict.keySet()){
            Double weightedProbability = totalProbabilityDict.get(modeKey) / totalWeight;
            totalProbabilityDict.put(modeKey, weightedProbability);
            Log.d(TAG, "Final probability for mode=" + modeKey + " is " + weightedProbability);
        }



    }

    private void sumValueToDictEntry(HashMap<Integer, Double> dict, Integer key, Double value){
        Double currentValue = 0.0;
        if(dict.containsKey(key)) {
            Log.d(TAG, "Contains Key =" + key + ", value=" + dict.get(key));
            currentValue = dict.get(key) + value;
        } else {

            Log.d(TAG, "New Key=" + key);
            currentValue = value;
        }
        dict.put(key, currentValue);
        Log.d(TAG, "New value for Key =" + key + ", value=" + dict.get(key));
    }







}
