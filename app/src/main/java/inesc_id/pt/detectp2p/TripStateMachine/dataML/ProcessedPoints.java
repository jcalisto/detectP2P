package inesc_id.pt.detectp2p.TripStateMachine.dataML;


public class ProcessedPoints {

    private double avgSpeed;
    private double maxSpeed;

    private double minSpeed;
    private double stdDevSpeed;

    private double avgAcc;
    private double maxAcc;
    private double minAcc;
    private double stdDevAcc;

    public int getEstimatedSpeed() {
        return estimatedSpeed;
    }

    public void setEstimatedSpeed(int estimatedSpeed) {
        this.estimatedSpeed = estimatedSpeed;
    }

    int estimatedSpeed = 0;

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
    }

    public double getStdDevSpeed() {
        return stdDevSpeed;
    }

    public void setStdDevSpeed(double stdDevSpeed) {
        this.stdDevSpeed = stdDevSpeed;
    }

    public double getAvgAcc() {
        return avgAcc;
    }

    public void setAvgAcc(double avgAcc) {
        this.avgAcc = avgAcc;
    }

    public double getMaxAcc() {
        return maxAcc;
    }

    public void setMaxAcc(double maxAcc) {
        this.maxAcc = maxAcc;
    }

    public double getMinAcc() {
        return minAcc;
    }

    public void setMinAcc(double minAcc) {
        this.minAcc = minAcc;
    }

    public double getStdDevAcc() {
        return stdDevAcc;
    }

    public void setStdDevAcc(double stdDevAcc) {
        this.stdDevAcc = stdDevAcc;
    }

    public double getGpsTimeMean() {
        return gpsTimeMean;
    }

    public void setGpsTimeMean(double gpsTimeMean) {
        this.gpsTimeMean = gpsTimeMean;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }


    public ProcessedPoints() {
    }

    private double gpsTimeMean;

    private double distance;


    public ProcessedPoints(double avgSpeed, double maxSpeed, double minSpeed, double stdDevSpeed, double avgAcc, double maxAcc, double minAcc, double stdDevAcc, double gpsTimeMean, double distance) {
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.stdDevSpeed = stdDevSpeed;
        this.avgAcc = avgAcc;
        this.maxAcc = maxAcc;
        this.minAcc = minAcc;
        this.stdDevAcc = stdDevAcc;
        this.gpsTimeMean = gpsTimeMean;
        this.distance = distance;
    }



}
