package inesc_id.pt.detectp2p.P2PManager.Bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.ArrayList;

public class BluetoothManager {

    Activity activity;
    Context context;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> deviceList;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    private static String TAG = "BluetoothManager";

    public BluetoothManager(Activity activity, Context context){
        this.activity = activity;
        this.context = context;
    }

   public void addDevice(BluetoothDevice b){
        deviceList.add(b);
   }

}
