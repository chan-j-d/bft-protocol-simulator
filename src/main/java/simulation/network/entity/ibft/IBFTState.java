package simulation.network.entity.ibft;

public enum IBFTState {

    NEW_ROUND("NEW_ROUND"),
    PRE_PREPARED("PRE_PREPARED"),
    PREPARED("PREPARED"),
    COMMITTED("COMMITTED"),
    FINAL_COMMITTED("FINAL_COMMITTED"),
    ROUND_CHANGE("ROUND_CHANGE");

    private final String representation;

    IBFTState(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return representation;
    }
}
