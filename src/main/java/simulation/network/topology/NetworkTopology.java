package simulation.network.topology;

import simulation.network.NetworkUtil;
import simulation.network.entity.Node;

import java.util.ArrayList;
import java.util.List;

public class NetworkTopology {
    public static <T> List<Node<T>> arrangeCliqueStructure(List<? extends Node<T>> nodes) {
        for (Node<T> node : nodes) {
            node.clearNeighbors();
            List<Node<T>> copy = new ArrayList<>(nodes);
            copy.remove(node);
            node.addNeighbors(copy);

            copy.forEach(neighbor -> node.registerDestination(neighbor, neighbor));
        }
        NetworkUtil.updateDestinationTables(nodes);
        return new ArrayList<>(nodes);
    }
}
