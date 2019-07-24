package inesc_id.pt.detectp2p.P2PNetwork;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.Command.ClassificationUpdate;
import inesc_id.pt.detectp2p.Command.Command;
import inesc_id.pt.detectp2p.Command.CommandCliHandlerImpl;
import inesc_id.pt.detectp2p.Command.TestCommand;
import inesc_id.pt.detectp2p.Command.UpdateCommand;
import inesc_id.pt.detectp2p.Response.CliResponse;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class WifiDirectHandler extends AsyncTask {

    public static String TAG = "WifiDirectHandler";

    WifiP2pManager manager;
    Channel channel;
    private CommandCliHandlerImpl handler;

    public WifiDirectHandler(WifiP2pManager manager, Channel channel) {
        this.manager = manager;
        this.channel = channel;
        handler = new CommandCliHandlerImpl();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {

            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            Log.d(TAG, "Starting socket on port 8080, waiting for connections");
            ServerSocket serverSocket = new ServerSocket(9000);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = serverSocket.accept();
                    try {
                        Log.d("WIFI-SERVICE", "Received Communication");

                        InputStream inputstream = client.getInputStream();

                        ObjectInputStream ois = new ObjectInputStream(inputstream);
                        Command cmd = null;
                        try {
                            cmd = (Command) ois.readObject();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                        CliResponse cliResponse = null;
                        if (cmd instanceof TestCommand) {
                            TestCommand update = (TestCommand) cmd;
                            cliResponse = handler.handle(update);
                        } else if (cmd instanceof ClassificationUpdate) {
                            ClassificationUpdate update = (ClassificationUpdate) cmd;
                            cliResponse = handler.handle(update);
                        }

                        ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                        oos.writeObject(cliResponse);

                    } catch (IOException e) {
                        Log.d("Error reading socket:", e.getMessage());
                    } finally {
                        client.close();
                    }
                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    break;
                }
            }

            serverSocket.close();
            return "SUCCESS";
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            return "ERROR";
        }
    }


    private static String getP2pDeviceStatus(int deviceStatus) {
        Log.d(TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }


}