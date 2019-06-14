package inesc_id.pt.detectp2p.Command;


import android.util.Log;

import inesc_id.pt.detectp2p.Response.CliResponse;

public class UpdateCommand implements Command {

    private static final long serialVersionUID = -8907331723807741905L;


    private String modelId;
    private byte[] modelBytes;

    public UpdateCommand(String modelId, byte[] bytes) {
        Log.d("UpdateCommand", "Creating update command, bytes size=" + bytes.length);
        this.modelId = modelId;
        this.modelBytes = bytes;

    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String update) {
        this.modelId = update;
    }

    public byte[] getModelBytes() {
        return modelBytes;
    }

    public void setModelBytes(byte[] modelBytes) {
        this.modelBytes = modelBytes;
    }

}