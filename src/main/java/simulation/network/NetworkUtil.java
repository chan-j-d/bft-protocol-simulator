package simulation.network;

import simulation.network.entity.NetworkNode;

import java.util.List;

public class NetworkUtil {

    public static void updateDestinationTables(List<? extends NetworkNode> nodes) {
        boolean isUpdated;
        do {
            isUpdated = false;
            for (NetworkNode node : nodes) {
                for (NetworkNode neighborNode : node.getNeighbors()) {
                    boolean isNodeUpdated = neighborNode.mergeDestinationTable(node);
                    isUpdated = isUpdated || isNodeUpdated;
                }
            }
        } while (isUpdated);
    }
}
