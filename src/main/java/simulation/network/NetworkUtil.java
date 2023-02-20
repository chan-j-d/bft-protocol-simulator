package simulation.network;

import simulation.network.entity.NetworkNode;

import java.util.List;

public class NetworkUtil {

    public static <T> void updateDestinationTables(List<? extends NetworkNode<T>> nodes) {
        boolean isUpdated;
        do {
            isUpdated = false;
            for (NetworkNode<T> node : nodes) {
                for (NetworkNode<T> neighborNode : node.getNeighbors()) {
                    boolean isNodeUpdated = neighborNode.mergeDestinationTable(node);
                    isUpdated = isUpdated || isNodeUpdated;
                }
            }
        } while (isUpdated);
    }
}
