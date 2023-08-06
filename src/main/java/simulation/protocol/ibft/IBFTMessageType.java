package simulation.protocol.ibft;

import java.util.List;
import java.util.Map;

/**
 * Enum for different message types the IBFT validator requires.
 */
public enum IBFTMessageType {

    PREPREPARED("PREPREPARED"),
    PREPARED("PREPARED"),
    ROUND_CHANGE("ROUND_CHANGE"),
    COMMIT("COMMIT"),
    SYNC("SYNC"),
    TIMER_EXPIRY("TIMER_EXPIRY");

    private static final Map<String, IBFTMessageType> STRING_IBFT_MESSAGE_TYPE_MAP = Map.of(
            "PREPREPARED", PREPREPARED,
            "PREPARED", PREPARED,
            "ROUND_CHANGE", ROUND_CHANGE,
            "COMMIT", COMMIT,
            "TIMER_EXPIRY", TIMER_EXPIRY
    );

    public static final List<IBFTMessageType> MESSAGE_TYPES = List.of(
            PREPREPARED, PREPARED, ROUND_CHANGE, COMMIT, TIMER_EXPIRY);

    private final String representation;

    IBFTMessageType(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return this.representation;
    }
}
