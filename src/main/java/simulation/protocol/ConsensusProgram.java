package simulation.protocol;

import simulation.network.entity.BFTMessage;
import simulation.network.entity.Payload;
import simulation.statistics.ConsensusStatistics;

import java.util.Collection;
import java.util.List;

public interface ConsensusProgram<T extends BFTMessage> {

    List<Payload<T>> processMessage(T message);
    int getConsensusCount();
    int getNumConsecutiveFailure();
    String getState();
    Collection<String> getStates();
    List<Payload<T>> initializationPayloads();
    List<Payload<T>> notifyTime(int timerCount);
    ConsensusStatistics getStatistics();
    void registerMessageProcessed(double timeTaken, String state);
}
