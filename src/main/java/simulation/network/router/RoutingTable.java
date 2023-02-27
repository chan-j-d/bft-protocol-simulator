package simulation.network.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoutingTable<T> {

    private final Map<T, List<T>> nodeNextNodeMap;
    private final Map<T, Integer> nodeDistanceMap;
    private final List<T> neighbors;

    public RoutingTable(List<? extends T> endPoints, List<? extends T> neighbors) {
        nodeNextNodeMap = endPoints.stream()
                .collect(Collectors.toMap(nodeName -> nodeName, nodeName -> new ArrayList<>()));
        neighbors.forEach(neighbor -> nodeNextNodeMap.get(neighbor).add(neighbor));
        nodeDistanceMap = neighbors.stream()
                .collect(Collectors.toMap(neighbor -> neighbor, neighbor -> 1));
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
            } else if (getNodeDistance(nodeName) == other.getNodeDistance(nodeName) + 1) {
                newNodeNextNodeMap.get(nodeName).add(otherNodeName);
            } else {
                // do nothing as other path through other node is longer
            }
        }

        return new RoutingTable<>(newNodeNextNodeMap, newNodeDistanceMap, new ArrayList<>(neighbors));
    }
}
