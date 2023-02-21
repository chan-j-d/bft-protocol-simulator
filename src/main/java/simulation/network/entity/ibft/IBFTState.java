package simulation.network.entity.ibft;

import java.util.List;

public enum IBFTState {

    NEW_ROUND("NEW_ROUND"),
    PREPREPARED("PRE_PREPARED"),
    PREPARED("PREPARED"),
    ROUND_CHANGE("ROUND_CHANGE");

    private final String name;

    public static final List<IBFTState> STATES = List.of(NEW_ROUND, PREPREPARED, PREPARED, ROUND_CHANGE);

    IBFTState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
