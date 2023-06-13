package simulation.network.router;

import simulation.network.entity.EndpointNode;
import simulation.network.entity.Node;
import simulation.network.entity.Payload;
import simulation.util.Pair;
import simulation.util.rng.RNGUtil;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Switch<T> extends Node<T> {

    private final RandomNumberGenerator rng;
    private final Map<String, Node<T>> stringToNodeMap;
    private final List<Node<T>> endpoints;
    private List<Node<T>> directlyConnectedEndpoints;
    private List<Switch<T>> switchNeighbors;
    private RoutingTable<Node<T>> table;

    public Switch(String name, List<? extends Node<T>> allEndpoints,
            List<? extends Node<T>> directlyConnectedEndpoints, RandomNumberGenerator rng) {
        super(name);
        this.endpoints = new ArrayList<>(allEndpoints);
        this.directlyConnectedEndpoints = new ArrayList<>(directlyConnectedEndpoints);
        this.stringToNodeMap = allEndpoints.stream()
                .collect(Collectors.toMap(Node::getName, endPoint -> endPoint));
        this.switchNeighbors = new ArrayList<>();
        this.rng = rng;
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    public void setSwitchNeighbors(List<? extends Switch<T>> switchNeighbors) {
        this.switchNeighbors = new ArrayList<>(switchNeighbors);
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    public void setDirectlyConnectedEndpoints(List<? extends EndpointNode<T>> endpoints) {
        this.directlyConnectedEndpoints = new ArrayList<>(endpoints);
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    public void updateSwitchNeighbors(List<? extends Switch<T>> newNeighbors) {
        this.switchNeighbors.addAll(newNeighbors);
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    public RoutingTable<Node<T>> getRoutingTable() {
        return table;
    }
    public boolean update() {
        boolean isUpdated = false;
        for (var neighbor : switchNeighbors) {
            RoutingTable<Node<T>> otherTable = neighbor.getRoutingTable();
            RoutingTable<Node<T>> newTable = table.addOtherRoutingTableInfo(neighbor, otherTable);
            isUpdated = isUpdated || !newTable.equals(table);
            table = newTable;
        }
        return isUpdated;
    }

    public Node<T> getNextNodeFor(Payload<T> payload) {
        String destinationString = payload.getDestination();
        Node<T> endpoint = stringToNodeMap.get(destinationString);
        if (directlyConnectedEndpoints.contains(endpoint)) {
            return endpoint;
        }

        List<Node<T>> nextHopNodeOptions = table.getNextHopNodeFor(endpoint);

        // tie-breaking mechanism - use a randomized decision
        int randomIndex = RNGUtil.getRandomInteger(0, nextHopNodeOptions.size());
        return nextHopNodeOptions.get(randomIndex);
    }

    @Override
    public boolean isStillRequiredToRun() {
        return true;
    }

    public List<Switch<T>> getSwitchNeighbors() {
        return switchNeighbors;
    }

    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        double duration = rng.generateRandomNumber();
        super.processPayload(time + duration, payload);
        return new Pair<>(duration, List.of(payload));
    }

    @Override
    public List<Payload<T>> initializationPayloads() {
        return List.of();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
