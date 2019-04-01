package inesc_id.pt.detectp2p.Command;


import inesc_id.pt.detectp2p.Response.CliResponse;

public class CliUpdateCommand implements CliCommand {

    private static final long serialVersionUID = -8907331723807741905L;

    private String update;

    public CliUpdateCommand(String update) {
        this.update = update;
    }

    public String getUpdate() {
        return update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    @Override
    public CliResponse handle(CommandClientHandler ch) {
        return ch.handle(this);
    }
}