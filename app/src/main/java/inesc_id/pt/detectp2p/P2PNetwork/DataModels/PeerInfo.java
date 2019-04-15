package inesc_id.pt.detectp2p.P2PNetwork.DataModels;

import java.util.Map;

import inesc_id.pt.detectp2p.P2PNetwork.DataModels.ModeInfo;

public class PeerInfo {

    private int time;
    private String name;
    private Map<Integer, ModeInfo> modeInfoByTime;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, ModeInfo> getModeInfoByTime() {
        return modeInfoByTime;
    }

    public void setModeInfoByTime(Map<Integer, ModeInfo> modeInfoByTime) {
        this.modeInfoByTime = modeInfoByTime;
    }
}