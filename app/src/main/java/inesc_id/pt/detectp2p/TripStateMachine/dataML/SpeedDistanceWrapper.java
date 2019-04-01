package inesc_id.pt.detectp2p.TripStateMachine.dataML;

public class SpeedDistanceWrapper {

    float avgSpeed;
    float maxSpeed;
    long distance;

    public SpeedDistanceWrapper(float avgSpeed, float maxSpeed, long distance) {
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.distance = distance;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public long getDistance() {
        return distance;
    }

    public void setDistance(long distance) {
        this.distance = distance;
    }

}
