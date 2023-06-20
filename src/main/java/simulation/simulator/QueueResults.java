package simulation.simulator;

import simulation.statistics.QueueStatistics;

/**
 * Interface for a queue-able object that tracks its own statistics.
 */
public interface QueueResults {

    QueueStatistics getQueueStatistics();
}
