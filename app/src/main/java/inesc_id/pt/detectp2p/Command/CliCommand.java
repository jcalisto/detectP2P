package inesc_id.pt.detectp2p.Command;

import java.io.Serializable;

import inesc_id.pt.detectp2p.Response.CliResponse;

/**
 * Created by Joao on 18/05/2018.
 */

public interface CliCommand extends Serializable{
    CliResponse handle(CommandClientHandler ch);
}
