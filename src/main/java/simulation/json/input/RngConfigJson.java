package simulation.json.input;

import java.util.List;

public class RngConfigJson {

    private String distributionType;
    private List<Double> parameters;

    public String getDistributionType() {
        return distributionType.toLowerCase();
    }

    public List<Double> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return distributionType + ": " + parameters;
    }
}
