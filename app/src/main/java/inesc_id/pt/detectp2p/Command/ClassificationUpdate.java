package inesc_id.pt.detectp2p.Command;

import android.util.Log;

import java.util.HashMap;

public class ClassificationUpdate implements Command {

    private static final long serialVersionUID = -8907331723807741905L;

    String senderID;
    int modeKey;
    HashMap<Integer, Double> probabilityDict;

    public ClassificationUpdate(int modeKey, HashMap<Integer, Double> probabilityDict, String senderID) {
        this.modeKey = modeKey;
        this.probabilityDict = probabilityDict;
        this.senderID = senderID;
    }

    public int getModeKey() {
        return modeKey;
    }

    public void setModeKey(int modeKey) {
        this.modeKey = modeKey;
    }

    public HashMap<Integer, Double> getProbabilityDict() {
        return probabilityDict;
    }

    public void setProbabilityDict(HashMap<Integer, Double> probabilityDict) {
        this.probabilityDict = probabilityDict;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }
}
