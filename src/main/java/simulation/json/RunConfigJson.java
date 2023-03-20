package simulation.json;

public class RunConfigJson {

    private int numRuns;
    private int numConsensus;
    private int startingSeed;
    private int seedMultiplier;
    private int numNodes;
    private double nodeProcessingRate;
    private double switchProcessingRate;
    private double baseTimeLimit;

    public int getNumRuns() {
        return numRuns;
    }

    public int getNumConsensus() {
        return numConsensus;
    }

    public int getStartingSeed() {
        return startingSeed;
    }

    public double getNodeProcessingRate() {
        return nodeProcessingRate;
    }

    public double getSwitchProcessingRate() {
        return switchProcessingRate;
    }

    public double getBaseTimeLimit() {
        return baseTimeLimit;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public int getSeedMultiplier() {
        return seedMultiplier;
    }
}
