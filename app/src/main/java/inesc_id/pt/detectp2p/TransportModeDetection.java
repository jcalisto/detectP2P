package inesc_id.pt.detectp2p;

import java.util.HashMap;
import java.util.Map;

import inesc_id.pt.detectp2p.DataModels.PeerInfo;

public class TransportModeDetection {

    //Save peers by [deviceName, VirtualIP]
    private Map<String, String> peersByName = new HashMap<String, String>();

    //Peer info by name
    private Map<String, PeerInfo> peerInfoByName = new HashMap<String, PeerInfo>();



}
