package simulation.network.structure;

import simulation.network.NetworkUtil;
import simulation.network.entity.NetworkNode;

import java.util.ArrayList;
import java.util.List;

public class NetworkStructure {
    public static <T> List<NetworkNode<T>> arrangeCliqueStructure(List<? extends NetworkNode<T>> nodes) {
        for (NetworkNode<T> node : nodes) {
            node.clearNeighbors();
            List<NetworkNode<T>> copy = new ArrayList<>(nodes);
            copy.remove(node);
            node.addNeighbors(copy);

            copy.forEach(neighbor -> node.registerDestination(neighbor, neighbor));
        }
        NetworkUtil.updateDestinationTables(nodes);
        return new ArrayList<>(nodes);
    }
}
