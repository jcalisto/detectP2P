package inesc_id.pt.detectp2p.Command;

import android.util.Log;

import inesc_id.pt.detectp2p.Response.CliResponse;
import inesc_id.pt.detectp2p.TransportModeDetection;
import inesc_id.pt.detectp2p.Utils.FileUtil;

public class CommandCliHandlerImpl implements CommandClientHandler{


    @Override
    public CliResponse handle(UpdateCommand c) {
        Log.d("CommandHandler", "Opening UpdateCommand");

        //FileUtil.writeClassifier("classifier_v3.pmml", c.getModelBytes());

        Log.d("CommandHandler", "UpdateCommand processed, received bytes: " + c.getModelBytes().length);

        return null;
    }

    @Override
    public CliResponse handle(ClassificationUpdate c) {
        Log.d("CommandHandler", "Opening Classification Update Prediction");

        TransportModeDetection.getInstance().updateStoredPeerInformation(c.senderID, c.modeKey, c.probabilityDict);

        Log.d("CommandHandler", "Dispatched update from peer to inner structure");

        return null;
    }
}
