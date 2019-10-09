package inesc_id.pt.detectp2p.P2PManager.Bluetooth;

import android.bluetooth.BluetoothDevice;

public class BluetoothPeer {

    public BluetoothDevice device;
    public String state = "NOT CONNECTED";
    public long timestamp = 0;

    public BluetoothPeer(BluetoothDevice device, String state, long timestamp){
        this.device = device;
        this.state = state;
        this.timestamp = timestamp;
    }
    public BluetoothPeer(BluetoothDevice device){
        this.device = device;
        this.state = "NOT CONNECTED";
        this.timestamp = 0;
    }
}
