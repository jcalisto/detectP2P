package inesc_id.pt.detectp2p.P2PManager;

import android.net.wifi.p2p.WifiP2pInfo;
import android.support.annotation.Nullable;

import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import java.util.ArrayList;

public class MySalut extends Salut {

    public MySalut(SalutDataReceiver dataReceiver, SalutServiceData salutServiceData, SalutCallback deviceNotSupported) {
        super(dataReceiver, salutServiceData, deviceNotSupported);
    }

    @Override
    public void setOnDeviceUnregisteredCallback(SalutDeviceCallback callback) {
        super.setOnDeviceUnregisteredCallback(callback);
    }

    @Override
    public ArrayList<String> getReadableFoundNames() {
        return super.getReadableFoundNames();
    }

    @Override
    public ArrayList<String> getReadableRegisteredNames() {
        return super.getReadableRegisteredNames();
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        super.onConnectionInfoAvailable(info);
    }

    @Override
    protected void closeRegistrationSocket() {
        super.closeRegistrationSocket();
    }

    @Override
    protected void closeDataSocket() {
        super.closeDataSocket();
    }

    @Override
    protected void startListeningForData() {
        super.startListeningForData();
    }

    @Override
    public void registerWithHost(SalutDevice device, @Nullable SalutCallback onRegistered, @Nullable SalutCallback onRegistrationFail) {
        super.registerWithHost(device, onRegistered, onRegistrationFail);
    }

    @Override
    public void sendToAllDevices(Object data, @Nullable SalutCallback onFailure) {
        super.sendToAllDevices(data, onFailure);
    }

    @Override
    public void sendToHost(Object data, @Nullable SalutCallback onFailure) {
        super.sendToHost(data, onFailure);
    }

    @Override
    public void sendToDevice(SalutDevice device, Object data, @Nullable SalutCallback onFailure) {
        super.sendToDevice(device, data, onFailure);
    }

    @Override
    public void cancelConnecting() {
        super.cancelConnecting();
    }

    @Override
    protected void forceDisconnect() {
        super.forceDisconnect();
    }

    @Override
    protected void disconnectFromDevice() {
        super.disconnectFromDevice();
    }

    @Override
    public void createGroup(SalutCallback onSuccess, SalutCallback onFailure) {
        super.createGroup(onSuccess, onFailure);
    }

    @Override
    public void unregisterClient(@Nullable SalutCallback onSuccess, @Nullable SalutCallback onFailure, boolean disableWiFi) {
        super.unregisterClient(onSuccess, onFailure, disableWiFi);
    }

    @Override
    public void unregisterClient(boolean disableWiFi) {
        super.unregisterClient(disableWiFi);
    }

    @Override
    public void startNetworkService(SalutDeviceCallback onDeviceRegisteredWithHost) {
        super.startNetworkService(onDeviceRegisteredWithHost);
    }

    @Override
    public void startNetworkService(@Nullable SalutDeviceCallback onDeviceRegisteredWithHost, @Nullable SalutCallback onSuccess, @Nullable SalutCallback onFailure) {
        super.startNetworkService(onDeviceRegisteredWithHost, onSuccess, onFailure);
    }

    @Override
    public void discoverNetworkServices(SalutDeviceCallback onDeviceFound, boolean callContinously) {
        super.discoverNetworkServices(onDeviceFound, callContinously);
    }

    @Override
    public void discoverNetworkServices(SalutCallback onDeviceFound, boolean callContinously) {
        super.discoverNetworkServices(onDeviceFound, callContinously);
    }

    @Override
    public void discoverWithTimeout(SalutCallback onDevicesFound, SalutCallback onDevicesNotFound, int timeout) {
        super.discoverWithTimeout(onDevicesFound, onDevicesNotFound, timeout);
    }

    @Override
    public void stopNetworkService(boolean disableWiFi) {
        super.stopNetworkService(disableWiFi);
    }

    @Override
    public void stopServiceDiscovery(boolean shouldUnregister) {
        super.stopServiceDiscovery(shouldUnregister);
    }

    @Override
    public String serialize(Object o) {
        return null;
    }
}
