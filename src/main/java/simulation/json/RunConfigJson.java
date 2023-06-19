package simulation.json;

import java.util.List;

/**
 * Encapsulates the setting configuration for a simulation run.
 */
public class RunConfigJson {

    private int numRuns;
    private int numConsensus;
    private int startingSeed;
    private int seedMultiplier;
    private int numNodes;
    private double nodeProcessingRate;
    private double switchProcessingRate;
    private double baseTimeLimit;
    private String networkType;
    private String validatorType;
    private List<Integer> networkParameters;

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

    public String getNetworkType() {
        return networkType;
    }

    public String getValidatorType() {
        return validatorType;
    }

    public List<Integer> getNetworkParameters() {
        return networkParameters;
    }
}
