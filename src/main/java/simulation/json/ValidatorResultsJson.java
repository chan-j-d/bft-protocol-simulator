package simulation.json;

import simulation.statistics.ConsensusStatistics;
import simulation.statistics.QueueStatistics;

import java.util.Map;

/**
 * Encapsulates the results of a validator in an unspecified BFT protocol.
 */
public class ValidatorResultsJson {

    /**
     * Map of time spent in each state, as specified in the {@code ConsensusStatistics} provided to it.
     */
    private final Map<String, Double> stateTimeMap;
    private final Map<String, Double> messageCountMap;
    private final Map<Integer, Map<String, Double>> roundStateTimeMap;
    private final double t_total;
    private final double L;
    private final double lambda;
    private final double W;

    public ValidatorResultsJson(ConsensusStatistics consensusStatistics, QueueStatistics queueStatistics) {
        t_total = consensusStatistics.getAverageConsensusTime();
        L = queueStatistics.getAverageNumMessagesInQueue();
        lambda = queueStatistics.getMessageArrivalRate();
        W = queueStatistics.getAverageMessageWaitingTime();
        stateTimeMap = consensusStatistics.getNormalizedStateTimeMap();
        roundStateTimeMap = consensusStatistics.getNormalizedRoundStateTimeMap();
        messageCountMap = consensusStatistics.getNormalizedMessageCountMap();
    }

    @Override
    public String toString() {
        return String.format("%s\n%s", stateTimeMap, t_total);
    }
}
