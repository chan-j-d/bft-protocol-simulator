package simulation.network.entity;

import simulation.util.Queueable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class NetworkNode implements Queueable<Payload> {

    private List<NetworkNode> neighbors;
    private Map<String, NetworkNode> destinationToNeighborMap;
    private String name;
    private LinkedList<Payload> queue;

    public NetworkNode(String name) {
        this.name = name;
        this.neighbors = new ArrayList<>();
        this.destinationToNeighborMap = new HashMap<>();
        this.queue = new LinkedList<>();
    }

    public NetworkNode(String name, List<NetworkNode> neighbors) {
        this.name = name;
        this.neighbors = new ArrayList<>(neighbors);
        this.destinationToNeighborMap = new HashMap<>();
        this.queue = new LinkedList<>();
    }

    public abstract List<Payload> processPayload(double time, Payload payload);
    public abstract List<Payload> initializationPayloads();

    /**
     * Returns the next time to notify the node for any timed events.
     *
     * @return next notification time or -1 if no need for notification.
     */
    public abstract double getNextNotificationTime();
    /**
     * Returns a list of payloads after notifying the node at the requested time.
     *
     * @param time to be notified, determined by a previous {@code getNextNotificationTime} call.
     * @return List of payloads to be sent at the given time.
     */
    public abstract List<Payload> notifyTime(double time);

    public String getName() {
        return name;
    }

    public NetworkNode getNextNodeFor(Payload payload) {
        return destinationToNeighborMap.get(payload.getDestination());
    }
    public boolean isPayloadDestination(Payload payload) {
        return payload.getDestination().equals(name);
    }

    public void registerDestination(String destination, NetworkNode neighbor) {
        destinationToNeighborMap.put(destination, neighbor);
    }

    public void registerDestination(NetworkNode destination, NetworkNode neighbor) {
        destinationToNeighborMap.put(destination.getName(), neighbor);
    }

    public boolean mergeDestinationTable(NetworkNode node) {
        boolean updated = false;
        for (Map.Entry<String, NetworkNode> entry : node.getDestinationToNeighborMap().entrySet()) {
            Object result = destinationToNeighborMap.putIfAbsent(entry.getKey(), entry.getValue());
            updated = updated || result == null;
        }
        return updated;
    }

    public Map<String, NetworkNode> getDestinationToNeighborMap() {
        return destinationToNeighborMap;
    }

    public void addNeighbor(NetworkNode neighbor) {
        neighbors.add(neighbor);
        destinationToNeighborMap.put(neighbor.getName(), neighbor);
    }

    public void addNeighbors(List<? extends NetworkNode> neighbors) {
        neighbors.forEach(this::addNeighbor);
    }

    public List<NetworkNode> getNeighbors() {
        return neighbors;
    }

    public void clearNeighbors() {
        neighbors.clear();
    }

    public List<Payload> broadcastMessage(String message, List<? extends NetworkNode> nodes) {
        List<Payload> payloads = new ArrayList<>();
        for (NetworkNode node : nodes) {
            payloads.add(new Payload(message, node.getName()));
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

        NetworkNode otherNode = (NetworkNode) o;
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
    public void addToQueue(Payload payload) {
        queue.add(payload);
    }

    @Override
    public Payload popFromQueue() {
        return queue.pop();
    }
}
