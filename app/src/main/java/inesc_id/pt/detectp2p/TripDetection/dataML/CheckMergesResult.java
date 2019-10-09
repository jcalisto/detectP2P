package inesc_id.pt.detectp2p.TripDetection.dataML;

import java.util.ArrayList;

public class CheckMergesResult {

    ArrayList<Segment> potentialLegs;
    boolean wasMerged;

    public CheckMergesResult(ArrayList<Segment> potentialLegs, boolean wasMerged) {
        this.potentialLegs = potentialLegs;
        this.wasMerged = wasMerged;
    }

    public ArrayList<Segment> getPotentialLegs() {
        return potentialLegs;
    }

    public void setPotentialLegs(ArrayList<Segment> potentialLegs) {
        this.potentialLegs = potentialLegs;
    }

    public boolean isWasMerged() {
        return wasMerged;
    }

    public void setWasMerged(boolean wasMerged) {
        this.wasMerged = wasMerged;
    }
}
