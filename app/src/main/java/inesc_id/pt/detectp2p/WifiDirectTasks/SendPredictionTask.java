package inesc_id.pt.detectp2p.WifiDirectTasks;

import android.os.AsyncTask;
import android.util.Log;

import org.jpmml.evaluator.Classification;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import inesc_id.pt.detectp2p.Command.ClassificationUpdate;
import inesc_id.pt.detectp2p.Command.UpdateCommand;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

public class SendPredictionTask extends AsyncTask<String, String, String> {

    private final static int PORT = 9000;
    private ClassificationUpdate command;

    private String host;


    //TASK TO SEND NEW CLASSIFIER
    public SendPredictionTask(String host, ClassificationUpdate command){
        Log.d("SendPredictionTask", "onCreate");
        this.command = command;
        this.host = host;
    }

    @Override
    protected String doInBackground(String[] params) {
        Log.d("SendPredictionTask", "Sending prediction to external peer!");
        Socket socket = new Socket();

        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, PORT)), 500);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(command);
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            socket.close();
            socket = null;
            return "ACK";
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = null;
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }

}
