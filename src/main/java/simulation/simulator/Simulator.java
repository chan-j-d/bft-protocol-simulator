package simulation.simulator;

import java.util.Optional;

/***
 * Simulator interface that runs a BFT protocol simulation.
 */
public interface Simulator {

    /**
     * Simulates one event and returns an optional String to print.
     */
    Optional<String> simulate();

    /**
     * Returns a String representation of a view of the nodes in the simulation.
     */
    String getSnapshotOfNodes();

    /**
     * Returns true if the simulation is over.
     */
    boolean isSimulationOver();

    /**
     * Returns the results of the BFT protocol simulation run.
     */
    RunResults getRunResults();
}
