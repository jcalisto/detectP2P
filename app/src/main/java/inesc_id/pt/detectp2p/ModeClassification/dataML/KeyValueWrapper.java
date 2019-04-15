package inesc_id.pt.detectp2p.ModeClassification.dataML;

import java.io.Serializable;

public class KeyValueWrapper implements Serializable{

    Integer key;
    Double value;

    public KeyValueWrapper(Integer key, Double value) {
        this.key = key;
        this.value = value;
    }

    public KeyValueWrapper() {
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

}
