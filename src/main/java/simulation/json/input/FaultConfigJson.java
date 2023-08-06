package simulation.json.input;

import java.util.List;

public class FaultConfigJson {

    private int numFaults;
    private String faultType;
    private List<Integer> faultParameters;

    public int getNumFaults() {
        return numFaults;
    }

    public String getFaultType() {
        return faultType.toLowerCase();
    }

    public List<Integer> getFaultParameters() {
        return faultParameters;
    }
}
