package simulation.json.input;

public class ValidatorConfigJson {

    private int numNodes;
    private int numConsensus;
    private String consensusProtocol;
    private double baseTimeLimit;
    private RngConfigJson nodeProcessingDistribution;
    private FaultConfigJson faultSettings;

    public int getNumConsensus() {
        return numConsensus;
    }

    public RngConfigJson getNodeProcessingDistribution() {
        return nodeProcessingDistribution;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public double getBaseTimeLimit() {
        return baseTimeLimit;
    }

    public String getConsensusProtocol() {
        return consensusProtocol.toLowerCase();
    }

    public FaultConfigJson getFaultSettings() {
        return faultSettings;
    }
}
