package inesc_id.pt.detectp2p.Command;

import android.util.Log;

import inesc_id.pt.detectp2p.Response.CliResponse;

public class CommandCliHandlerImpl implements CommandClientHandler{


    @Override
    public CliResponse handle(CliUpdateCommand c) {
        //TODO
        Log.d("UPDATE HANDLER", " RECEIVED: " + c.getUpdate());
        return null;
    }
}
