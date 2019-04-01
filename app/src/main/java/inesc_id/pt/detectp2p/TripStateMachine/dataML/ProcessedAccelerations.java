package inesc_id.pt.detectp2p.TripStateMachine.dataML;

public class ProcessedAccelerations {

    private double avgAccel;
    private double maxAccel;

    private double minAccel;
    private double stdDevAccel;

    private double between_03_06;
    private double between_06_1;
    private double between_1_3;
    private double between_3_6;
    private double above_6;

    public ProcessedAccelerations() {
    }

    public ProcessedAccelerations(double avgAccel, double maxAccel, double minAccel, double stdDevAccel) {
        this.avgAccel = avgAccel;
        this.maxAccel = maxAccel;
        this.minAccel = minAccel;
        this.stdDevAccel = stdDevAccel;
    }

    public double getAvgAccel() {
        return avgAccel;
    }

    public void setAvgAccel(double avgAccel) {
        this.avgAccel = avgAccel;
    }

    public double getMaxAccel() {
        return maxAccel;
    }

    public void setMaxAccel(double maxAccel) {
        this.maxAccel = maxAccel;
    }

    public double getMinAccel() {
        return minAccel;
    }

    public void setMinAccel(double minAccel) {
        this.minAccel = minAccel;
    }

    public double getStdDevAccel() {
        return stdDevAccel;
    }

    public void setStdDevAccel(double stdDevAccel) {
        this.stdDevAccel = stdDevAccel;
    }

    public double getBetween_03_06() {
        return between_03_06;
    }

    public void setBetween_03_06(double between_03_06) {
        this.between_03_06 = between_03_06;
    }

    public double getBetween_06_1() {
        return between_06_1;
    }

    public void setBetween_06_1(double between_06_1) {
        this.between_06_1 = between_06_1;
    }

    public double getBetween_1_3() {
        return between_1_3;
    }

    public void setBetween_1_3(double between_1_3) {
        this.between_1_3 = between_1_3;
    }

    public double getBetween_3_6() {
        return between_3_6;
    }

    public void setBetween_3_6(double between_3_6) {
        this.between_3_6 = between_3_6;
    }

    public double getAbove_6() {
        return above_6;
    }

    public void setAbove_6(double above_6) {
        this.above_6 = above_6;
    }


}
