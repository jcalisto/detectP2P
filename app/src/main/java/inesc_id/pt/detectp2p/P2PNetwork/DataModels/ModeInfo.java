package inesc_id.pt.detectp2p.P2PNetwork.DataModels;

import java.util.HashMap;
import java.util.List;

import inesc_id.pt.detectp2p.ModeClassification.dataML.KeyValueWrapper;

public class ModeInfo {

    private HashMap<Integer, Double> probasDicts;
    private List<KeyValueWrapper> probasOrdered;

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
}
