package simulation.network;

import simulation.network.entity.Node;

import java.util.List;

public class NetworkUtil {

    public static <T> void updateDestinationTables(List<? extends Node<T>> nodes) {
        boolean isUpdated;
        do {
            isUpdated = false;
            for (Node<T> node : nodes) {
                for (Node<T> neighborNode : node.getNeighbors()) {
                    boolean isNodeUpdated = neighborNode.mergeDestinationTable(node);
                    isUpdated = isUpdated || isNodeUpdated;
                }
            }
        } while (isUpdated);
    }
}
