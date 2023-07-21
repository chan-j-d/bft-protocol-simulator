package simulation.json;

public class ValidatorConfigJson {

    private int numNodes;
    private int numConsensus;
    private String validatorType;
    private double baseTimeLimit;
    private double nodeProcessingRate;

    public int getNumConsensus() {
        return numConsensus;
    }

    public double getNodeProcessingRate() {
        return nodeProcessingRate;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public double getBaseTimeLimit() {
        return baseTimeLimit;
    }

    public String getValidatorType() {
        return validatorType;
    }
}
