package simulation.statistics;

import java.util.Comparator;

public class ConsensusTimeComparator implements Comparator<ConsensusStatistics> {

    @Override
    public int compare(ConsensusStatistics consensusStatistics1, ConsensusStatistics consensusStatistics2) {
        double consensusTime1 = consensusStatistics1.getAverageConsensusTime();
        double consensusTime2 = consensusStatistics2.getAverageConsensusTime();
        if (Math.abs(consensusTime1 - consensusTime2) < 1e-8) {
            return 0;
        } else if (consensusTime1 > consensusTime2) {
            return 1;
        } else {
            return -1;
        }
    }
}
