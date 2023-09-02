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
     * Returns the list of resulting payloads as a result of processing {@code message}.
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

    /**
     * Registers the effects of {@code timeTaken}
     */
    void registerMessageProcessed(T message, double timeTaken);

    int getConsensusCount();
    int getNumConsecutiveFailure();
    String getState();
    Collection<String> getStates();

    ConsensusStatistics getStatistics();
}
