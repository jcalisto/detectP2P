package inesc_id.pt.detectp2p.Taks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import inesc_id.pt.detectp2p.Activities.MainActivity;
import inesc_id.pt.detectp2p.Command.Command;
import inesc_id.pt.detectp2p.Command.RequestClassifier;
import inesc_id.pt.detectp2p.Command.UpdateCommand;

public class RequestToServerTask extends AsyncTask<String, Void, String> {

    private MainActivity mainActivity;

    public RequestToServerTask() {
        super();
    }


    @Override
    protected String doInBackground(String[] params) {
        Socket server = null;
        String reply = "";

        Command glc = new RequestClassifier("classifier_v2.pmml.ser", "lisboa");

        try {
            server = new Socket("194.210.229.209", 9090);
            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
            oos.writeObject(glc);
            ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
            Command response = (Command) ois.readObject();

            if (response != null & response instanceof UpdateCommand){
                Log.d("RequestTask","Received update command");

                UpdateCommand update = (UpdateCommand) response;
                Log.d("RequestTask","Updated classifier has ID=" + update.getModelId());
                oos.close();
                ois.close();
            }
        }
        catch (Exception e) {
            Log.d("RequestTask", "Task failed..." + e.getMessage());
            e.printStackTrace();
        } finally {
            if (server != null) {
                try { server.close(); }
                catch (Exception e) { }
            }
        }
        return reply;
    }

    @Override
    protected void onPostExecute(String o) {

    }
}
