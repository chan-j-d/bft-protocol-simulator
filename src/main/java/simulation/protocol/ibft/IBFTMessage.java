package simulation.protocol.ibft;

import simulation.network.entity.BFTMessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static simulation.util.StringUtil.MESSAGE_SEPARATOR;

/**
 * Message sent between IBFT validators.
 */
public class IBFTMessage extends BFTMessage {

    /**
     * Null value placeholder for message components that are not used.
     */
    public static final int NULL_VALUE = -1;

    private final int identifier;
    private final int recipient;
    private final IBFTMessageType messageType;
    private final int lambda;
    private final int round;
    private final int value;
    private final int preparedRound;
    private final int preparedValue;
    private final List<IBFTMessage> piggybackMessages;

    private IBFTMessage(int identifier, int recipient, IBFTMessageType messageType, int lambda,
                int round, int value, int preparedRound, int preparedValue) {
        this.identifier = identifier;
        this.recipient = recipient;
        this.messageType = messageType;
        this.lambda = lambda;
        this.round = round;
        this.value = value;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
        this.piggybackMessages = List.of();
    }

    private IBFTMessage(int identifier, int recipient, IBFTMessageType messageType, int lambda,
            int round, int value, int preparedRound, int preparedValue, List<IBFTMessage> piggybackMessages) {
        this.identifier = identifier;
        this.recipient = recipient;
        this.messageType = messageType;
        this.lambda = lambda;
        this.round = round;
        this.value = value;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
        this.piggybackMessages = Optional.ofNullable(piggybackMessages).map(lst -> List.copyOf(piggybackMessages))
                .orElse(List.of());
    }

    /**
     * Creates an IBFT message of the given {@code type} and specified {@code value}.
     */
    public static IBFTMessage createValueMessage(int identifier, int recipient, IBFTMessageType type,
            int lambda, int round, int value) {
        return new IBFTMessage(identifier, recipient, type, lambda, round, value, NULL_VALUE, NULL_VALUE);
    }

    /**
     * Creates an IBFT message of the given {@code type} and specified {@code value} with {@code piggybackMessages}.
     */
    public static IBFTMessage createValueMessage(int identifier, int recipient, IBFTMessageType type,
            int lambda, int round, int value, List<IBFTMessage> piggybackMessages) {
        return new IBFTMessage(identifier, recipient, type, lambda, round, value,
                NULL_VALUE, NULL_VALUE, piggybackMessages);
    }

    /**
     * Creates an IBFT message with {@code preparedRound} and {@code preparedValue} specified.
     */
    public static IBFTMessage createPreparedValuesMessage(int identifier, int recipient, IBFTMessageType messageType,
            int lambda, int round, int preparedRound, int preparedValue) {
        return new IBFTMessage(identifier, recipient, messageType, lambda, round, NULL_VALUE,
                preparedRound, preparedValue);
    }

    /**
     * Creates an IBFT message with {@code preparedRound} and {@code preparedValue} with {@code piggybackMessages}.
     */
    public static IBFTMessage createPreparedValuesMessage(int identifier, int recipient, IBFTMessageType messageType,
            int lambda, int round, int preparedRound, int preparedValue, List<IBFTMessage> piggybackMessages) {
        return new IBFTMessage(identifier, recipient, messageType, lambda, round, NULL_VALUE,
                preparedRound, preparedValue, piggybackMessages);
    }

    public int getIdentifier() {
        return identifier;
    }

    public IBFTMessageType getMessageType() {
        return messageType;
    }

    public int getLambda() {
        return lambda;
    }

    public int getRound() {
        return round;
    }

    public int getValue() {
        return value;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public int getPreparedValue() {
        return preparedValue;
    }

    public List<IBFTMessage> getPiggybackMessages() {
        return piggybackMessages;
    }

    @Override
    public String toString() {
        return Stream.of(identifier, messageType, lambda, round, value,
                        preparedRound, preparedValue, piggybackMessages.size())
                .map(Object::toString)
                .reduce((x, y) -> x + MESSAGE_SEPARATOR + y)
                .get();
    }

    @Override
    public String getType() {
        return getMessageType().toString();
    }

    @Override
    public int getRecipientId() {
        return recipient;
    }
}
