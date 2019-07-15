package inesc_id.pt.detectp2p.P2PNetwork.DataModels;

import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.P2PNetwork.DataModels.ModeInfo;

public class PeerInfo {

    private int time;
    private long lastTimeStamp;
    private String name;
    private Map<Integer, ModeInfo> modeInfoByTime = new HashMap<>();

    public PeerInfo(String name){
        time = 0;
        this.name = name;
    }


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


    public long getLastTimeStamp() {
        return lastTimeStamp;
    }

    public void setLastTimeStamp(long lastTimeStamp) {
        this.lastTimeStamp = lastTimeStamp;
    }

    public void  addModeInfo(ModeInfo modeInfo){
        increaseTimer();
        lastTimeStamp = System.currentTimeMillis();
        if(modeInfoByTime.containsKey(time)){
            addModeInfo(modeInfo);
        }
        else{
            modeInfoByTime.put(time, modeInfo);
        }
    }

    public void increaseTimer(){
        time += 1;
    }
}
