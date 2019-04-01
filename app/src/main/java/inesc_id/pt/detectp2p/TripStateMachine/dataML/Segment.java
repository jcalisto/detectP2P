package inesc_id.pt.detectp2p.TripStateMachine.dataML;

public class Segment {

    int mode;
    int length;
    double probSum;
    int firstIndex;

    public Segment(int mode, int length, double probSum, int firstIndex) {
        this.mode = mode;
        this.length = length;
        this.probSum = probSum;
        this.firstIndex = firstIndex;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public double getProbSum() {
        return probSum;
    }

    public void setProbSum(double probSum) {
        this.probSum = probSum;
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public void setFirstIndex(int firstIndex) {
        this.firstIndex = firstIndex;
    }

}
