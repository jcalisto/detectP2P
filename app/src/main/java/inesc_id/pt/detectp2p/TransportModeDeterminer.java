package inesc_id.pt.detectp2p;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.P2PManager.DataModels.ModeInfo;
import inesc_id.pt.detectp2p.P2PManager.DataModels.PeerInfo;
import inesc_id.pt.detectp2p.TripValidationManager.ValidatedDataManager;

public class TransportModeDeterminer {

    public static TransportModeDeterminer instance;

    private static final String TAG = "TransportModeDeterminer";

    private int currentState = possibleStates.STATIONARY;

    public static TransportModeDeterminer getInstance(){
        if (instance == null){ //if there is no instance available... create new one
            instance = new TransportModeDeterminer();
        }

        return instance;
    }

    public TransportModeDeterminer(){
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

    public ModeInfo getClassifierModeInfo() {
        return classifierModeInfo;
    }

    //Take decision based on peer results
    private void takeDecision() {
        //TODO
        Log.d(TAG, "Taking decision");

        //HashMap<Integer, Double> peerModesProbabilities = getDecisionFromPeerModes();

        getDecisionFromValidatedData();

    }

    private void getDecisionFromValidatedData(){
        Float[][] matrix = ValidatedDataManager.getInstance().getLocalValidationMatrix();

        Log.d(TAG, "PRINTING VALIDATION MATRIX");
        Log.d(TAG, "CAR_CLASSIFIER:_____________R_BIKE:" + matrix[9][1] + "___R_WALKING:" + matrix[9][7] + "___R_CAR:" +
                matrix[9][9] + "___R_BUS:" + matrix[9][15] + "___R_TRAIN:" + matrix[9][10]);
        Log.d(TAG, "WALKING_CLASSIFIER:_________R_BIKE:" + matrix[7][1] + "___R_WALKING:" + matrix[7][7] + "___R_CAR:" +
                matrix[7][9] + "___R_BUS:" + matrix[7][15] + "___R_TRAIN:" + matrix[7][10]);
        Log.d(TAG, "BICYCLE_CLASSIFIER:_________R_BIKE:" + matrix[1][1] + "___R_WALKING:" + matrix[1][7] + "___R_CAR:" +
                matrix[1][9] + "___R_BUS:" + matrix[1][15] + "___R_TRAIN:" + matrix[1][10]);
        Log.d(TAG, "BUS_CLASSIFIER:_____________R_BIKE:" + matrix[15][1] + "___R_WALKING:" + matrix[15][7] + "___R_CAR:" +
                matrix[15][9] + "___R_BUS:" + matrix[15][15] + "___R_TRAIN:" + matrix[15][10]);
        Log.d(TAG, "TRAIN_CLASSIFIER:___________R_BIKE:" + matrix[10][1] + "___R_WALKING:" + matrix[10][7] + "___R_CAR:" +
                matrix[10][9] + "___R_BUS:" + matrix[10][15] + "___R_TRAIN:" + matrix[10][10]);


    }

    private HashMap<Integer, Double> getDecisionFromPeerModes(){
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

        return totalProbabilityDict;
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


    public HashMap<Integer, Double> getModeInfoInTimeInterval(long start, long end){
        HashMap<Integer, Double> averageProbasDictFromAllPeers = new HashMap<>();
        int userfullPeerCount = 0; //Peers used for Probas statistic

        for(PeerInfo peer : peerInfoByName.values()){
          ArrayList<ModeInfo> modeInfoList = new ArrayList<>();

            //Discard peers with less than 5 time units iterations
            /*if(!peer.getModeInfoByTime().containsKey(5)){
                continue;
            }*/

            //Get Every Mode Info Within the time interval From Peer
            for(int peerTimeCounter : peer.getModeInfoByTime().keySet()){
                ModeInfo tempMode = peer.getModeInfoByTime().get(peerTimeCounter);
                //if(start <= tempMode.getRealTimestamp() && end>=tempMode.getRealTimestamp()) {
                    modeInfoList.add(tempMode);
                //}
            }

            HashMap<Integer, Double> averageProbasDict = new HashMap<>();

            //Sum every mode probability from mode info list
            for(ModeInfo mode : modeInfoList){
                for (Map.Entry<Integer, Double> entry : mode.getProbasDicts().entrySet()) {
                    Integer key = entry.getKey();
                    Double current = averageProbasDict.get(key);
                    averageProbasDict.put(key, current == null ? entry.getValue() : entry.getValue() + current);
                }
            }

            //Divide every probability by the amount of modes to obtain the average.
            for(Integer key : averageProbasDict.keySet()){
                averageProbasDict.put(key, averageProbasDict.get(key) / modeInfoList.size());
            }

            if(averageProbasDict.size() > 0){
                userfullPeerCount++;
            }
            //Update total probas dict with current peer probas dict
            for(Map.Entry<Integer, Double> entry : averageProbasDict.entrySet()){
                Integer key = entry.getKey();
                Double current = averageProbasDictFromAllPeers.get(key);
                averageProbasDictFromAllPeers.put(key, current == null ? entry.getValue() : entry.getValue() + current);
            }

        }

        for(Integer key : averageProbasDictFromAllPeers.keySet()){
            averageProbasDictFromAllPeers.put(key, averageProbasDictFromAllPeers.get(key) / userfullPeerCount);
        }

        return averageProbasDictFromAllPeers;
    }

    public int getCurrentState(){
        return currentState;
    }

    public Map<String, PeerInfo> getPeerInfoByName() {
        return peerInfoByName;
    }

    public ArrayList<ModeInfo> getLastPredictions(){
        ArrayList<ModeInfo> predictions = new ArrayList<>();

        for(String key : peerInfoByName.keySet()){
            PeerInfo peer = peerInfoByName.get(key);

            if(peer.getTime() < 3){
                continue;
            }

            for(Integer modeKey : peer.getModeInfoByTime().keySet()){
                ModeInfo modeInfo = peer.getModeInfoByTime().get(modeKey);
                if(System.currentTimeMillis() - modeInfo.getRealTimestamp() <= 90000){
                    predictions.add(modeInfo);
                }
            }

        }
        return predictions;
    }

    public void updateState(int newState){
        currentState = newState;
    }

    public interface possibleStates {
        int TRAVELLING = 0;
        int STATIONARY = 1;
        int WAITING = 2;
    }




}
