package simulation.json.input;

public class SwitchConfigJson {

    private RngConfigJson switchProcessingDistribution;
    private double messageChannelSuccessRate;

    public RngConfigJson getSwitchProcessingDistribution() {
        return switchProcessingDistribution;
    }

    public double getMessageChannelSuccessRate() {
        return messageChannelSuccessRate;
    }
}
