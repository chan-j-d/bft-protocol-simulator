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
    private final Map<String, Double> fastestStateTimeMap;
    private final Map<String, Double> remainderStateTimeMap;

    private final Map<String, Double> fastestMessageCountMap;
    private final Map<String, Double> remainderMessageCountMap;

    private final Map<Integer, Map<String, Double>> fastestRoundStateTimeMap;
    private final Map<Integer, Map<String, Double>> remainderRoundStateTimeMap;

    private final double t_total_fastest;
    private final double t_total_remainder;

    private final double L_fastest;
    private final double L_remainder;

    private final double W_fastest;
    private final double W_remainder;

    private final double lambda_fastest;
    private final double lambda_remainder;

    public ValidatorResultsJson(ConsensusStatistics fastestConsensusStats,
            ConsensusStatistics remainderConsensusStats,
            QueueStatistics fastestQueueStats, QueueStatistics remainderQueueStats) {
        t_total_fastest = fastestConsensusStats.getAverageConsensusTime();
        L_fastest = fastestQueueStats.getAverageNumMessagesInQueue();
        W_fastest = fastestQueueStats.getAverageMessageWaitingTime();
        lambda_fastest = fastestQueueStats.getMessageArrivalRate();

        fastestStateTimeMap = fastestConsensusStats.getNormalizedStateTimeMap();
        fastestRoundStateTimeMap = fastestConsensusStats.getNormalizedRoundStateTimeMap();
        fastestMessageCountMap = fastestConsensusStats.getNormalizedMessageCountMap();

        t_total_remainder = remainderConsensusStats.getAverageConsensusTime();
        L_remainder = remainderQueueStats.getAverageNumMessagesInQueue();
        W_remainder = remainderQueueStats.getAverageMessageWaitingTime();
        lambda_remainder = remainderQueueStats.getMessageArrivalRate();

        remainderStateTimeMap = remainderConsensusStats.getNormalizedStateTimeMap();
        remainderRoundStateTimeMap = remainderConsensusStats.getNormalizedRoundStateTimeMap();
        remainderMessageCountMap = remainderConsensusStats.getNormalizedMessageCountMap();
    }

    @Override
    public String toString() {
        return String.format("%s\n%s", fastestStateTimeMap, t_total_fastest);
    }
}
