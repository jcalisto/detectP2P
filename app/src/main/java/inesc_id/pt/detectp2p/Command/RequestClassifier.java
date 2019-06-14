package inesc_id.pt.detectp2p.Command;

import inesc_id.pt.detectp2p.Response.CliResponse;

public class RequestClassifier implements Command {

    /**
     *
     */
    private static final long serialVersionUID = -5075092256917036574L;

    private String currentClassifier;
    private String location;

   public RequestClassifier(String classifier, String location){
       this.currentClassifier = classifier;
       this.location = location;
   }
}
