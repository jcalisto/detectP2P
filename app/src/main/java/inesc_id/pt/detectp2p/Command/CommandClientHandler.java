package inesc_id.pt.detectp2p.Command;

import inesc_id.pt.detectp2p.Response.CliResponse;

public interface CommandClientHandler {
    public CliResponse handle(UpdateCommand c);

    CliResponse handle(ClassificationUpdate c);
}
