package simulation.protocol;

import simulation.network.entity.BFTMessage;
import simulation.statistics.ConsensusStatistics;

import java.util.Collection;
import java.util.List;

/**
 * Encapsulates a program implementing a consensus protocol.
 *
 * @param <T> Message type sent between consensus nodes.
 */
public interface ConsensusProgram<T extends BFTMessage> {

    /**
     * Registers the message received and processes it and returns the resulting messages for processing.
     *
     * Includes side effects such as recording statistics.
     */
    List<T> processAndRegisterMessage(T message, double currentTime);

    /**
     * Returns the list of resulting messages as a result of processing {@code message}.
     */
    List<T> processMessage(T message);

    /**
     * Returns the list of payloads during initialization of the node.
     */
    List<T> initializationPayloads();

    /**
     * Returns the list of payloads as a result of a timer notification.
     */
    List<T> notifyTime(int timerCount);

    int getConsensusCount();
    int getNumConsecutiveFailure();
    String getState();
    Collection<String> getStates();

    ConsensusStatistics getStatistics();
}
