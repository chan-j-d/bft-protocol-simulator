package simulation.network.entity.ibft;

/**
 * Enum for different states a IBFT validator can take.
 */
public enum IBFTState {

    NEW_ROUND("NEW_ROUND"),
    PREPREPARED("PRE_PREPARED"),
    PREPARED("PREPARED"),
    ROUND_CHANGE("ROUND_CHANGE");

    private final String name;

    IBFTState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
