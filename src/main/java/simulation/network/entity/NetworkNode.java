package simulation.network.entity;

import simulation.util.Pair;
import simulation.util.Queueable;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.TestGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class NetworkNode<T> implements Queueable<Payload<T>> {

    //TODO update means to configure timing. Currently set to 0 sec
    private static final RandomNumberGenerator RNG = new TestGenerator(0);
    private List<NetworkNode<T>> neighbors;
    private Map<String, NetworkNode<T>> destinationToNeighborMap;
    private String name;
    private LinkedList<Payload<T>> queue;
    private double currentTime;

    public NetworkNode(String name) {
        this.name = name;
        this.neighbors = new ArrayList<>();
        this.destinationToNeighborMap = new HashMap<>();
        this.queue = new LinkedList<>();
        this.currentTime = 0;
    }

    public NetworkNode(String name, List<? extends NetworkNode<T>> neighbors) {
        this.name = name;
        this.neighbors = new ArrayList<>(neighbors);
        this.destinationToNeighborMap = new HashMap<>();
        this.queue = new LinkedList<>();
    }

    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        this.currentTime = time;
        return new Pair<>(0.0, List.of());
    }

    public boolean isOccupiedAtTime(double time) {
        return currentTime > time;
    }

    public abstract List<Payload<T>> initializationPayloads();
    public abstract boolean isDone();

    public String getName() {
        return name;
    }

    public NetworkNode<T> getNextNodeFor(Payload<T> payload) {
        return destinationToNeighborMap.get(payload.getDestination());
    }
    public Pair<Double, Boolean> isPayloadDestination(Payload<T> payload) {
        return new Pair<>(RNG.generateRandomNumber(), payload.getDestination().equals(name));
    }

    public void registerDestination(String destination, NetworkNode<T> neighbor) {
        destinationToNeighborMap.put(destination, neighbor);
    }

    public void registerDestination(NetworkNode<T> destination, NetworkNode<T> neighbor) {
        destinationToNeighborMap.put(destination.getName(), neighbor);
    }

    public boolean mergeDestinationTable(NetworkNode<T> node) {
        boolean updated = false;
        for (Map.Entry<String, NetworkNode<T>> entry : node.getDestinationToNeighborMap().entrySet()) {
            Object result = destinationToNeighborMap.putIfAbsent(entry.getKey(), entry.getValue());
            updated = updated || result == null;
        }
        return updated;
    }

    public Map<String, NetworkNode<T>> getDestinationToNeighborMap() {
        return destinationToNeighborMap;
    }

    public void addNeighbor(NetworkNode<T> neighbor) {
        neighbors.add(neighbor);
        destinationToNeighborMap.put(neighbor.getName(), neighbor);
    }

    public void addNeighbors(List<? extends NetworkNode<T>> neighbors) {
        neighbors.forEach(this::addNeighbor);
    }

    public List<NetworkNode<T>> getNeighbors() {
        return neighbors;
    }

    public void clearNeighbors() {
        neighbors.clear();
    }

    public Payload<T> sendMessage(T message, NetworkNode<T> node) {
        return new Payload<>(message, node.getName());
    }
    public List<Payload<T>> sendMessage(T message, Collection<? extends NetworkNode<T>> nodes) {
        List<Payload<T>> payloads = new ArrayList<>();
        for (NetworkNode<T> node : nodes) {
            payloads.add(new Payload<>(message, node.getName()));
        }
        return payloads;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NetworkNode)) {
            return false;
        }

        NetworkNode<?> otherNode = (NetworkNode<?>) o;
        return otherNode.name.equals(this.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void addToQueue(Payload<T> payload) {
        queue.add(payload);
    }

    @Override
    public Payload<T> popFromQueue() {
        return queue.pop();
    }
}
