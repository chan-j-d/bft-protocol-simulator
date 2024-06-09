package simulation.protocol;

import simulation.network.entity.BFTMessage;
import simulation.statistics.ConsensusStatistics;

import java.util.Collection;
import java.util.List;

public class NoProgram<T extends BFTMessage> implements ConsensusProgram<T> {

    @Override
    public List<T> processAndRegisterMessage(T message, double currentTime) {
        return List.of();
    }

    @Override
    public List<T> processMessage(T message) {
        return List.of();
    }

    @Override
    public List<T> initializationPayloads() {
        return List.of();
    }

    @Override
    public List<T> notifyTime(int timerCount) {
        return List.of();
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
