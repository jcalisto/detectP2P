package inesc_id.pt.detectp2p.TripStateMachine.dataML;

import java.util.ArrayList;

public class OngoingTripWrapper {

    ArrayList<FullTripPart> identifiedLegs;
    int currentState;
    ArrayList<LocationDataContainer> currentLocations;

    public OngoingTripWrapper(ArrayList<FullTripPart> identifiedLegs, int currentState, ArrayList<LocationDataContainer> currentLocations) {
        this.identifiedLegs = identifiedLegs;
        this.currentState = currentState;
        this.currentLocations = currentLocations;
    }

    public ArrayList<FullTripPart> getIdentifiedLegs() {
        return identifiedLegs;
    }

    public void setIdentifiedLegs(ArrayList<FullTripPart> identifiedLegs) {
        this.identifiedLegs = identifiedLegs;
    }

    public int getCurrentState() {
        return currentState;
    }

    public void setCurrentState(int currentState) {
        this.currentState = currentState;
    }

    public ArrayList<LocationDataContainer> getCurrentLocations() {
        return currentLocations;
    }

    public void setCurrentLocations(ArrayList<LocationDataContainer> currentLocations) {
        this.currentLocations = currentLocations;
    }



}
