package simulation.network.entity.ibft;

import java.util.Map;

public enum IBFTMessageType {

    PREPREPARED("PREPREPARED"),
    PREPARED("PREPARED"),
    ROUND_CHANGE("ROUND_CHANGE"),
    COMMIT("COMMIT"),
    TIMER_EXPIRY("TIMER_EXPIRY");

    private static final Map<String, IBFTMessageType> STRING_IBFT_MESSAGE_TYPE_MAP = Map.of(
            "PREPREPARED", PREPREPARED,
            "PREPARED", PREPARED,
            "ROUND_CHANGE", ROUND_CHANGE,
            "COMMIT", COMMIT,
            "TIMER_EXPIRY", TIMER_EXPIRY
    );

    private final String representation;

    IBFTMessageType(String representation) {
        this.representation = representation;
    }

    public static IBFTMessageType getMessageTypeFromString(String stringType) {
        IBFTMessageType type = STRING_IBFT_MESSAGE_TYPE_MAP.get(stringType);
        if (type == null) {
            throw new RuntimeException("Not a valid string type!");
        }

        return type;
    }

    @Override
    public String toString() {
        return this.representation;
    }
}
