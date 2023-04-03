package simulation.json;

import simulation.network.entity.ibft.IBFTStatistics;
import simulation.statistics.QueueStatistics;

public class IBFTResultsJson {

    private double t_newRound;
    private double t_prePrepared;
    private double t_prepared;
    private double t_roundChange;
    private double t_total;
    private double L;
    private double lambda;
    private double W;

    public IBFTResultsJson(IBFTStatistics ibftStatistics, QueueStatistics queueStatistics) {
        t_total = ibftStatistics.getAverageConsensusTime();
        t_newRound = ibftStatistics.getNewRoundTime();
        t_prePrepared = ibftStatistics.getPrePreparedTime();
        t_roundChange = ibftStatistics.getRoundChangeTime();
        t_prepared = ibftStatistics.getPreparedTime();
        L = queueStatistics.getAverageNumMessagesInQueue();
        lambda = queueStatistics.getMessageArrivalRate();
        W = queueStatistics.getAverageMessageWaitingTime();
    }

    @Override
    public String toString() {
        return String.format("t_newRound: %.3f\nt_prePrepared: %.3f\n" +
                "t_prepared: %.3f\nt_total: %.3f", t_newRound, t_prePrepared, t_prepared, t_total);
    }
}
