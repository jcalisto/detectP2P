package inesc_id.pt.detectp2p.P2PNetwork.DataModels;

import java.util.HashMap;
import java.util.List;

import inesc_id.pt.detectp2p.ModeClassification.dataML.KeyValueWrapper;

public class ModeInfo {

    private HashMap<Integer, Double> probasDicts;
    private List<KeyValueWrapper> probasOrdered;
    private long realTimestamp;

    private int detectedMode;

    public ModeInfo(HashMap<Integer, Double> probasDicts){
        this.probasDicts = probasDicts;
    }

    public HashMap<Integer, Double> getProbasDicts() {
        return probasDicts;
    }

    public void setProbasDicts(HashMap<Integer, Double> probasDicts) {
        this.probasDicts = probasDicts;
    }

    public List<KeyValueWrapper> getProbasOrdered() {
        return probasOrdered;
    }

    public void setProbasOrdered(List<KeyValueWrapper> probasOrdered) {
        this.probasOrdered = probasOrdered;
    }

    public int getDetectedMode() {
        return detectedMode;
    }

    public void setDetectedMode(int detectedMode) {
        this.detectedMode = detectedMode;
    }

    public long getRealTimestamp() {
        return realTimestamp;
    }

    public void setRealTimestamp(long realTimestamp) {
        this.realTimestamp = realTimestamp;
    }

    public static ModeInfo getTestModeInfo(){

        ModeInfo mode;
        HashMap<Integer, Double> testDict1 = new HashMap<>();

        testDict1.put(1, 0.0);
        testDict1.put(3, 0.0);
        testDict1.put(7, 0.00);
        testDict1.put(9, 0.45);
        testDict1.put(10, 0.23);
        testDict1.put(11, 0.35);

        mode = new ModeInfo(testDict1);
        mode.detectedMode = 9;
        return mode;
    }
}
