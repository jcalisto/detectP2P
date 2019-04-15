package inesc_id.pt.detectp2p.ModeClassification.dataML;

public class MLAlgorithmInput {

    static int currOs = 0;

    ProcessedPoints processedPoints;
    ProcessedAccelerations processedAccelerations;

    long startDate;

    int OSVersion = 0;


    double accelsBelowFilter;
    double avgFilteredAccels;

    public MLAlgorithmInput(double accelsBelowFilter, double avgFilteredAccels, ProcessedAccelerations processedAccelerations, ProcessedPoints processedPoints, long startSegmentDate) {
        this.accelsBelowFilter = accelsBelowFilter;
        this.avgFilteredAccels = avgFilteredAccels;
        this.processedAccelerations = processedAccelerations;
        this.processedPoints = processedPoints;
        this.startDate = startSegmentDate;
        this.OSVersion = MLAlgorithmInput.currOs;
    }

    public static void setCurOs(int os){
        MLAlgorithmInput.currOs = os;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public ProcessedPoints getProcessedPoints() {
        return processedPoints;
    }

    public void setProcessedPoints(ProcessedPoints processedPoints) {
        this.processedPoints = processedPoints;
    }

    public ProcessedAccelerations getProcessedAccelerations() {
        return processedAccelerations;
    }

    public void setProcessedAccelerations(ProcessedAccelerations processedAccelerations) {
        this.processedAccelerations = processedAccelerations;
    }

    public double getAccelsBelowFilter() {
        return accelsBelowFilter;
    }

    public void setAccelsBelowFilter(double accelsBelowFilter) {
        this.accelsBelowFilter = accelsBelowFilter;
    }

    public double getAvgFilteredAccels() {
        return avgFilteredAccels;
    }

    public void setAvgFilteredAccels(double avgFilteredAccels) {
        this.avgFilteredAccels = avgFilteredAccels;
    }

    public int getOSVersion() {
        return OSVersion;
    }



}
