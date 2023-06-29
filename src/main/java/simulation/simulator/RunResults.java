package simulation.simulator;

import simulation.statistics.ConsensusStatistics;
import simulation.statistics.QueueStatistics;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Encapsulates the results of a BFT protocol simulation run.
 * Contains queue statistics of switches and validators and consensus statistics.
 */
public class RunResults {

    private final ConsensusStatistics fastestValidatorStatistics;
    private final ConsensusStatistics remainderValidatorStatistics;
    private final QueueStatistics fastestValidatorQueueStatistics;
    private final QueueStatistics remainderValidatorQueueStatistics;
    private final List<QueueStatistics> switchStatistics;

    public RunResults(ConsensusStatistics fastestValidatorStatistics, ConsensusStatistics remainderValidatorStatistics,
            QueueStatistics fastestValidatorQueueStatistics, QueueStatistics remainderValidatorQueueStatistics,
            List<QueueStatistics> switchStatistics) {
        this.fastestValidatorStatistics = fastestValidatorStatistics;
        this.remainderValidatorStatistics = remainderValidatorStatistics;
        this.fastestValidatorQueueStatistics = fastestValidatorQueueStatistics;
        this.remainderValidatorQueueStatistics = remainderValidatorQueueStatistics;
        this.switchStatistics = switchStatistics;
    }

    public ConsensusStatistics getFastestValidatorStatistics() {
        return fastestValidatorStatistics;
    }

    public ConsensusStatistics getRemainderValidatorStatistics() {
        return remainderValidatorStatistics;
    }

    public QueueStatistics getFastestValidatorQueueStatistics() {
        return fastestValidatorQueueStatistics;
    }

    public QueueStatistics getRemainderValidatorQueueStatistics() {
        return remainderValidatorQueueStatistics;
    }

    public List<QueueStatistics> getSwitchStatistics() {
        return switchStatistics;
    }

    /**
     * Returns a new {@code RunResults} that merges {@code this} and {@code other} together.
     */
    public RunResults mergeRunResults(RunResults other) {
        ConsensusStatistics newFastestConsensusStatistics =
                fastestValidatorStatistics.combineStatistics(other.getFastestValidatorStatistics());
        ConsensusStatistics newRemainderConsensusStatistics = remainderValidatorStatistics
                .combineStatistics(other.getRemainderValidatorStatistics());

        QueueStatistics newFastestQueueStatistics =
                fastestValidatorQueueStatistics.combineStatistics(other.getFastestValidatorQueueStatistics());
        QueueStatistics newRemainderQueueStatistics =
                remainderValidatorQueueStatistics.combineStatistics(other.getRemainderValidatorQueueStatistics());

        List<QueueStatistics> newSwitchStatistics =
                IntStream.iterate(0, i -> i < switchStatistics.size(), i -> i + 1)
                        .mapToObj(i -> switchStatistics.get(i).combineStatistics(other.getSwitchStatistics().get(i)))
                        .collect(Collectors.toList());
        return new RunResults(newFastestConsensusStatistics, newRemainderConsensusStatistics,
                newFastestQueueStatistics, newRemainderQueueStatistics, newSwitchStatistics);
    }

    @Override
    public String toString() {
        return String.format("Validator Stats:\n%s\nValidator Queue Stats:\n%s\nSwitch Stats%s",
                fastestValidatorStatistics,
                fastestValidatorQueueStatistics,
                switchStatistics);
    }
}
