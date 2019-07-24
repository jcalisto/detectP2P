package inesc_id.pt.detectp2p.WifiDirectTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import inesc_id.pt.detectp2p.Command.Command;
import inesc_id.pt.detectp2p.Command.TestCommand;

public class TestTask extends AsyncTask<String, String, String> {

    private static String TAG = "TestTask";
    private Command command;

    public TestTask() {
        this.command = new TestCommand("Hello Peer :)");
    }

    @Override
    protected String doInBackground(String[] params) {
        String host = params[0];
        int port = 8080;

        Socket socket = new Socket();

        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 500);

            OutputStream outputStream = socket.getOutputStream();

            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(command);
            oos.flush();
            oos.close();
            outputStream.close();

        } catch (IOException e) {
            //catch logic
        }

        finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
        }
        return "SUCCESS";
    }

    /**
     * Start activity that can handle the JPEG image
     */
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }
}