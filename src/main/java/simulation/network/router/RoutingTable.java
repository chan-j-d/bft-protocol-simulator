package simulation.network.router;

import simulation.network.entity.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routing table for switches to map routing paths to different nodes.
 * @param <T> Network node subclass, usually an endpoint node.
 */
public class RoutingTable<T extends Node<?>> {

    private final Map<T, List<T>> nodeNextNodeMap;
    private final Map<T, Integer> nodeDistanceMap;
    private final List<T> neighbors;

    /**
     * Initializes a routing table with the default state of only being able to reach all neighboring nodes in 1 hop.
     *
     * @param endPoints All endpoint nodes to be reachable by the routing table.
     * @param connectedEndpoints Connected endpoints that are directly reachable.
     * @param neighbors Neighboring nodes to facilitate routing which are 1 hop away.
     */
    public RoutingTable(List<? extends T> endPoints, List<? extends T> connectedEndpoints,
            List<? extends T> neighbors) {
        nodeNextNodeMap = endPoints.stream()
                .collect(Collectors.toMap(nodeName -> nodeName, nodeName -> new ArrayList<>()));
        nodeDistanceMap = connectedEndpoints.stream()
                .collect(Collectors.toMap(point -> point, point -> 1));
        connectedEndpoints.forEach(endpoint -> nodeNextNodeMap.get(endpoint).add(endpoint));
        this.neighbors = new ArrayList<>(neighbors);
    }

    private RoutingTable(Map<T, List<T>> nodeNextNodeMap, Map<T, Integer> nodeDistanceMap,
            List<T> neighbors) {
        this.nodeDistanceMap = nodeDistanceMap;
        this.nodeNextNodeMap = nodeNextNodeMap;
        this.neighbors = neighbors;
    }

    public boolean isNodeRecorded(T nodeName) {
        return nodeDistanceMap.containsKey(nodeName);
    }

    public int getNodeDistance(T nodeName) {
        return nodeDistanceMap.get(nodeName);
    }

    public List<T> getNextHopNodeFor(T endpoint) {
        return nodeNextNodeMap.get(endpoint);
    }

    /**
     * Combines the information of two routing tables to reach nodes 1 hop further.
     */
    public RoutingTable<T> addOtherRoutingTableInfo(T otherNodeName, RoutingTable<T> other) {
        Collection<T> nodeNames = nodeNextNodeMap.keySet();
        Map<T, List<T>> newNodeNextNodeMap = new HashMap<>(nodeNextNodeMap);
        Map<T, Integer> newNodeDistanceMap = new HashMap<>(nodeDistanceMap);

        for (T nodeName : nodeNames) {
            if (!other.isNodeRecorded(nodeName)) {
                continue;
            }

            if (!isNodeRecorded(nodeName) || getNodeDistance(nodeName) > other.getNodeDistance(nodeName) + 1) {
                newNodeDistanceMap.put(nodeName, other.getNodeDistance(nodeName) + 1);
                newNodeNextNodeMap.put(nodeName, new ArrayList<>(List.of(otherNodeName)));
            } else if (getNodeDistance(nodeName) == other.getNodeDistance(nodeName) + 1 &&
                    !newNodeNextNodeMap.get(nodeName).contains(otherNodeName)) {
                newNodeNextNodeMap.get(nodeName).add(otherNodeName);
            } else {
                // do nothing as other path through other node is longer
            }
        }

        return new RoutingTable<>(newNodeNextNodeMap, newNodeDistanceMap, new ArrayList<>(neighbors));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoutingTable)) {
            return false;
        }

        RoutingTable<?> otherTable = (RoutingTable<?>) o;
        return nodeNextNodeMap.equals(otherTable.nodeNextNodeMap)
                && nodeDistanceMap.equals(otherTable.nodeDistanceMap)
                && neighbors.equals(otherTable.neighbors);
    }

    @Override
    public String toString() {
        return String.format("%s\n%s\n%s", neighbors, nodeDistanceMap, nodeNextNodeMap);
    }
}
