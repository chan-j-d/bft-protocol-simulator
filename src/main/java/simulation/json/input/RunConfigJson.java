package simulation.json.input;

/**
 * Encapsulates the setting configuration for a simulation run.
 */
public class RunConfigJson {

    private int numRuns;
    private int startingSeed;
    private int seedMultiplier;
    private ValidatorConfigJson validatorSettings;
    private NetworkConfigurationJson networkSettings;

    public int getNumRuns() {
        return numRuns;
    }

    public int getStartingSeed() {
        return startingSeed;
    }

    public int getSeedMultiplier() {
        return seedMultiplier;
    }

    public ValidatorConfigJson getValidatorSettings() {
        return validatorSettings;
    }

    public NetworkConfigurationJson getNetworkSettings() {
        return networkSettings;
    }
}
