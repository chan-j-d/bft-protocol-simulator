package simulation.simulator;

import simulation.statistics.ConsensusStatistics;

public interface ValidatorResults extends QueueResults {

    ConsensusStatistics getConsensusStatistics();
}
