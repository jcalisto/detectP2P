package inesc_id.pt.detectp2p.Taks;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import inesc_id.pt.detectp2p.Command.UpdateCommand;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

public class SendUpdateTask extends AsyncTask<String, String, String> {

    public final static int PORT = 10001;
    UpdateCommand command;
    private SimWifiP2pSocket mCliSocket = null;


    //TASK TO SEND NEW CLASSIFIER
    public SendUpdateTask(UpdateCommand command){
        this.command = command;
    }

    @Override
    protected String doInBackground(String[] params) {
        try {
            mCliSocket = new SimWifiP2pSocket(params[0], PORT);
            ObjectOutputStream oos = new ObjectOutputStream(mCliSocket.getOutputStream());
            oos.writeObject(command);
            Log.d("WIFI-SERVICE", "Sent update!");
            ObjectInputStream ois = new ObjectInputStream(mCliSocket.getInputStream());

            mCliSocket.close();
            mCliSocket = null;
            return "ACK";
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCliSocket = null;
        return null;
    }

}