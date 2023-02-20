package simulation.network.entity;

import simulation.util.Queueable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class NetworkNode<T> implements Queueable<Payload<T>> {

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

    public List<Payload<T>> processPayload(double time, Payload<T> payload) {
        this.currentTime = time;
        return List.of();
    }

    public boolean isOccupiedAtTime(double time) {
        return currentTime > time;
    }

    public abstract List<Payload<T>> initializationPayloads();

    public String getName() {
        return name;
    }

    public NetworkNode<T> getNextNodeFor(Payload<T> payload) {
        return destinationToNeighborMap.get(payload.getDestination());
    }
    public boolean isPayloadDestination(Payload<T> payload) {
        return payload.getDestination().equals(name);
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

    public List<Payload<T>> sendMessage(T message, List<? extends NetworkNode<T>> nodes) {
        List<Payload<T>> payloads = new ArrayList<>();
        for (NetworkNode<T> node : nodes) {
            payloads.add(new Payload<T>(message, node.getName()));
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
