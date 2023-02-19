package simulation.network.entity.ibft;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class IBFTMessage {

    /**
     * Null value placeholder for message components that are not used.
     */
    public static final int NULL_VALUE = -1;

    private static final String SEPARATOR = ":";

    private int identifier;
    private IBFTMessageType messageType;
    private int lambda;
    private int round;
    private int value;
    private int preparedRound;
    private int preparedValue;
    private List<IBFTMessage> piggybackMessages;

    private IBFTMessage(int identifier, IBFTMessageType messageType, int lambda,
                int round, int value, int preparedRound, int preparedValue) {
        this.identifier = identifier;
        this.messageType = messageType;
        this.lambda = lambda;
        this.round = round;
        this.value = value;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
        this.piggybackMessages = List.of();
    }

    private IBFTMessage(int identifier, IBFTMessageType messageType, int lambda,
            int round, int value, int preparedRound, int preparedValue, List<IBFTMessage> piggybackMessages) {
        this.identifier = identifier;
        this.messageType = messageType;
        this.lambda = lambda;
        this.round = round;
        this.value = value;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
        this.piggybackMessages = List.copyOf(piggybackMessages);
    }

    public static IBFTMessage createIBFTMessageFromString(String message) {
        String[] details = message.split(SEPARATOR);
        return new IBFTMessage(
                Integer.parseInt(details[0]),
                IBFTMessageType.getMessageTypeFromString(details[1]),
                Integer.parseInt(details[2]),
                Integer.parseInt(details[3]),
                Integer.parseInt(details[4]),
                Integer.parseInt(details[5]),
                Integer.parseInt(details[6])
        );
    }
    public static IBFTMessage createValueMessage(int identifier, IBFTMessageType messageType,
            int lambda, int round, int value) {
        return new IBFTMessage(identifier, messageType, lambda, round, value, NULL_VALUE, NULL_VALUE);
    }

    public static IBFTMessage createPreparedValuesMessage(int identifier, IBFTMessageType messageType,
            int lambda, int round, int preparedRound, int preparedValue) {
        return new IBFTMessage(identifier, messageType, lambda, round, NULL_VALUE, preparedRound, preparedValue);
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
                .reduce((x, y) -> x + SEPARATOR + y)
                .get();
    }
}
