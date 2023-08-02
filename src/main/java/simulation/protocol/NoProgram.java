package simulation.protocol;

import simulation.network.entity.BFTMessage;
import simulation.network.entity.Payload;
import simulation.statistics.ConsensusStatistics;

import java.util.Collection;
import java.util.List;

public class NoProgram<T extends BFTMessage> implements ConsensusProgram<T> {

    @Override
    public List<Payload<T>> processMessage(T message) {
        return List.of();
    }

    @Override
    public List<Payload<T>> initializationPayloads() {
        return List.of();
    }

    @Override
    public List<Payload<T>> notifyTime(int timerCount) {
        return List.of();
    }

    @Override
    public void registerMessageProcessed(T message, double timeTaken) {
        return;
    }

    @Override
    public int getConsensusCount() {
        return 0;
    }

    @Override
    public int getNumConsecutiveFailure() {
        return 0;
    }

    @Override
    public String getState() {
        return "";
    }

    @Override
    public Collection<String> getStates() {
        return List.of();
    }

    @Override
    public ConsensusStatistics getStatistics() {
        return new ConsensusStatistics(List.of());
    }
}
