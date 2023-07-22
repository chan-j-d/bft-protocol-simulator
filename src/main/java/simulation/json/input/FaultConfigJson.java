package simulation.json.input;

import java.util.List;

public class FaultConfigJson {

    private int numNodes;
    private String faultType;
    private List<Integer> faultParameters;

    public int getNumNodes() {
        return numNodes;
    }

    public String getFaultType() {
        return faultType;
    }

    public List<Integer> getFaultParameters() {
        return faultParameters;
    }
}
