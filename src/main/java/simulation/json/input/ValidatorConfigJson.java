package simulation.json.input;

public class ValidatorConfigJson {

    private int numNodes;
    private int numConsensus;
    private String consensusProtocol;
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

    public String getConsensusProtocol() {
        return consensusProtocol;
    }
}
