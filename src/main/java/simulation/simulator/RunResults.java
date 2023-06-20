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

    private final ConsensusStatistics validatorStatistics;
    private final QueueStatistics validatorQueueStatistics;
    private final List<QueueStatistics> switchStatistics;

    public RunResults(ConsensusStatistics validatorStatistics, QueueStatistics validatorQueueStatistics,
            List<QueueStatistics> switchStatistics) {
        this.validatorStatistics = validatorStatistics;
        this.validatorQueueStatistics = validatorQueueStatistics;
        this.switchStatistics = switchStatistics;
    }

    public ConsensusStatistics getValidatorStatistics() {
        return validatorStatistics;
    }

    public QueueStatistics getValidatorQueueStatistics() {
        return validatorQueueStatistics;
    }

    public List<QueueStatistics> getSwitchStatistics() {
        return switchStatistics;
    }

    /**
     * Returns a new {@code RunResults} that merges {@code this} and {@code other} together.
     */
    public RunResults mergeRunResults(RunResults other) {
        ConsensusStatistics newConsensusStatistics =
                validatorStatistics.combineStatistics(other.getValidatorStatistics());

        QueueStatistics newQueueStatistics =
                validatorQueueStatistics.combineStatistics(other.getValidatorQueueStatistics());

        List<QueueStatistics> newSwitchStatistics =
                IntStream.iterate(0, i -> i < switchStatistics.size(), i -> i + 1)
                        .mapToObj(i -> switchStatistics.get(i).combineStatistics(other.getSwitchStatistics().get(i)))
                        .collect(Collectors.toList());
        return new RunResults(newConsensusStatistics, newQueueStatistics, newSwitchStatistics);
    }

    @Override
    public String toString() {
        return String.format("Validator Stats:\n%s\nValidator Queue Stats:\n%s\nSwitch Stats%s",
                validatorStatistics,
                validatorQueueStatistics,
                switchStatistics);
    }
}
