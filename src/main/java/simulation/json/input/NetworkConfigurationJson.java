package simulation.json.input;

import java.util.List;

public class NetworkConfigurationJson {

    private SwitchConfigJson switchSettings;
    private String networkType;
    private List<Integer> networkParameters;

    public SwitchConfigJson getSwitchSettings() {
        return switchSettings;
    }

    public String getNetworkType() {
        return networkType.toLowerCase();
    }

    public List<Integer> getNetworkParameters() {
        return networkParameters;
    }
}
