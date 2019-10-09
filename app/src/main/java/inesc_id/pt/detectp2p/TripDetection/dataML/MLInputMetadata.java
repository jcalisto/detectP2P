package inesc_id.pt.detectp2p.TripDetection.dataML;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import inesc_id.pt.detectp2p.TripDetection.SortMapByValue;


public class MLInputMetadata implements Serializable{

    MLAlgorithmInput mlAlgorithmInput;

    HashMap<Integer, Double> probasDicts;
    List<KeyValueWrapper> probasOrdered;

    public MLInputMetadata(MLAlgorithmInput mlAlgorithmInput, HashMap<Integer, Double> probasDicts){
        this.mlAlgorithmInput = mlAlgorithmInput;
        this.probasDicts = probasDicts;
        LinkedHashMap<Integer, Double> orderedRes = (LinkedHashMap<Integer, Double>) SortMapByValue.sortByValueDesc(probasDicts);
        List<Map.Entry<Integer, Double>> temp = new ArrayList<>(orderedRes.entrySet());

        probasOrdered = new ArrayList<>();

        for (Map.Entry<Integer, Double> entry : temp){
            probasOrdered.add(new KeyValueWrapper(entry.getKey(), entry.getValue()));
        }


    }

    public MLInputMetadata(){}

    public Double getProbabilityByMode(int mode){
        return  probasDicts.get(mode);
    }

    public MLAlgorithmInput getMlAlgorithmInput() {
        return mlAlgorithmInput;
    }

    public void setMlAlgorithmInput(MLAlgorithmInput mlAlgorithmInput) {
        this.mlAlgorithmInput = mlAlgorithmInput;
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

    public KeyValueWrapper getBestMode(){
        return probasOrdered.get(0);
    }

}
