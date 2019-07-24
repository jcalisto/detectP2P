package inesc_id.pt.detectp2p.Command;

import android.util.Log;

public class TestCommand implements Command {

    private static final long serialVersionUID = -8907331723807741905L;

    private String message;

    public TestCommand(String message) {
        Log.d("TestCommand", "Creating TEST command");
        this.message = message;

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
