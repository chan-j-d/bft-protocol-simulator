package simulation.simulator;

import simulation.statistics.ConsensusStatistics;

/**
 * Interface for a validator node's results.
 */
public interface ValidatorResults extends QueueResults {

    ConsensusStatistics getConsensusStatistics(int programNumber);
    int getNumConsensusPrograms();
}
