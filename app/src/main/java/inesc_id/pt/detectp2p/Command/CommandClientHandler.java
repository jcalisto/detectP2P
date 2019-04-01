package inesc_id.pt.detectp2p.Command;

import inesc_id.pt.detectp2p.Response.CliResponse;

public interface CommandClientHandler {
    public CliResponse handle(CliUpdateCommand c);
}
