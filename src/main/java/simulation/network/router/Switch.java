package simulation.network.router;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.TestGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Switch<T> extends Node<T> {

    //TODO update means to configure timing. Currently set to 0 sec
    private static final RandomNumberGenerator RNG = new TestGenerator(0);

    private List<Switch<T>> neighbors;
    private final Map<String, Node<T>> stringToNodeMap;
    private final List<Node<T>> endpoints;
    private RoutingTable<Node<T>> table;

    public Switch(String name, List<Node<T>> endpoints) {
        super(name);
        this.endpoints = new ArrayList<>(endpoints);
        this.stringToNodeMap = endpoints.stream()
                .collect(Collectors.toMap(Node::getName, endPoint -> endPoint));
    }

    public void setNeighbors(List<Switch<T>> neighbors) {
        this.neighbors = new ArrayList<>(neighbors);
        this.table = new RoutingTable<>(endpoints, neighbors);
    }

    public RoutingTable<Node<T>> getRoutingTable() {
        return table;
    }
    public void update() {
        for (var neighbor : neighbors) {
            RoutingTable<Node<T>> otherTable = neighbor.getRoutingTable();
            table = table.addOtherRoutingTableInfo(neighbor, otherTable);
        }
    }

    public Node<T> getNextNodeFor(Payload<T> payload) {
        String destinationString = payload.getDestination();
        Node<T> endpoint = stringToNodeMap.get(destinationString);
        if (endpoints.contains(endpoint)) {
            return endpoint;
        }

        List<Node<T>> nextHopNodeOptions = table.getNextHopNodeFor(endpoint);

        // tie-breaking mechanism - use a randomized decision
        int randomIndex = new Random().nextInt(nextHopNodeOptions.size());

        return nextHopNodeOptions.get(randomIndex);
    }

    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        return new Pair<>(RNG.generateRandomNumber(), List.of(payload));
    }

    @Override
    public List<Payload<T>> initializationPayloads() {
        return List.of();
    }

    @Override
    public boolean isDone() {
        return false;
    }
}