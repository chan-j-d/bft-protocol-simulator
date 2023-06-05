package simulation.simulator;

import java.util.Optional;

public interface Simulator {

    Optional<String> simulate();
    String getSnapshotOfNodes();
    boolean isSimulationOver();
    RunResults getRunResults();
}
