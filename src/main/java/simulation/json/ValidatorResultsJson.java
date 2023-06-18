package simulation.json;

import simulation.statistics.ConsensusStatistics;
import simulation.statistics.QueueStatistics;

import java.util.LinkedHashMap;
import java.util.Map;

public class ValidatorResultsJson {

    private Map<String, Double> stateTimeMap;
    private double t_total;
    private double L;
    private double lambda;
    private double W;

    public ValidatorResultsJson(ConsensusStatistics consensusStatistics, QueueStatistics queueStatistics) {
        t_total = consensusStatistics.getAverageConsensusTime();
        L = queueStatistics.getAverageNumMessagesInQueue();
        lambda = queueStatistics.getMessageArrivalRate();
        W = queueStatistics.getAverageMessageWaitingTime();
        stateTimeMap = new LinkedHashMap<>();
        for (String state : consensusStatistics.getStates()) {
            stateTimeMap.put(state, consensusStatistics.getTimeInState(state));
        }
    }

    @Override
    public String toString() {
        return String.format("%s\n%s", stateTimeMap, t_total);
    }
}
