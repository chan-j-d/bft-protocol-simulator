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

/**
 * Represents a switch in a computer network.
 *
 * @param <T> Message class carried by {@code Switch} and other nodes in the network.
 */
public class Switch<T> extends Node<T> {

    private final RandomNumberGenerator rng;
    private final Map<String, Node<T>> stringToNodeMap;
    private final List<Node<T>> endpoints;
    private List<Node<T>> directlyConnectedEndpoints;
    private List<Switch<T>> switchNeighbors;
    private RoutingTable<Node<T>> table;

    /**
     * @param name Name of switch.
     * @param allEndpoints Endpoints in the network.
     * @param directlyConnectedEndpoints Nodes directly connected to this switch.
     * @param rng Random number generator for service rate of switch.
     */
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

    /**
     * Sets the switch neighbors of this switch.
     * Used for calculating route tables for the switch.
     */
    public void setSwitchNeighbors(List<? extends Switch<T>> switchNeighbors) {
        this.switchNeighbors = new ArrayList<>(switchNeighbors);
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    /**
     * Sets directly connected endpoints of this switch.
     * Used for calculating route tables for the switch.
     */
    public void setDirectlyConnectedEndpoints(List<? extends EndpointNode<T>> endpoints) {
        this.directlyConnectedEndpoints = new ArrayList<>(endpoints);
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    /**
     * Updates the switch table of this switch with {@code newNeighbors}.
     */
    public void updateSwitchNeighbors(List<? extends Switch<T>> newNeighbors) {
        this.switchNeighbors.addAll(newNeighbors);
        this.table = new RoutingTable<>(endpoints, directlyConnectedEndpoints, switchNeighbors);
    }

    public RoutingTable<Node<T>> getRoutingTable() {
        return table;
    }

    /**
     * Updates the routing table of this switch using the routing tables of neighboring switches.
     * Used to establish a route from this switch to all endpoints in the network.
     */
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

    /**
     * Returns the next hop node for a payload moving to its destination.
     * A random shortest hop count routing protocol is used.
     */
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
