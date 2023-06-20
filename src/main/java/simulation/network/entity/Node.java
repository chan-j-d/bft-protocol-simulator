package simulation.network.entity;

import simulation.simulator.QueueResults;
import simulation.statistics.QueueStatistics;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates a node in a network.
 *
 * @param <T> Message class carried by {@code Node}.
 */
public abstract class Node<T> implements QueueResults {

    private final String name;
    /**
     * Queue of payloads at the node to be processed.
     */
    private final LinkedList<Payload<T>> queue;
    /**
     * Arrival times of messages.
     * Use for tracking and calculating of queue statistics.
     */
    private final LinkedList<Double> messageArrivalTimes;
    /**
     * Tracking of queue statistics in the node.
     */
    private final QueueStatistics queueStatistics;
    private double currentTime;
    /**
     * Helper time variable used for tracking average number of messages in queue.
     */
    private double previousQueueChangedTime;

    public Node(String name) {
        this.name = name;
        this.queue = new LinkedList<>();
        this.currentTime = 0;
        this.queueStatistics = new QueueStatistics();
        this.messageArrivalTimes = new LinkedList<>();
        this.previousQueueChangedTime = 0;
    }

    public abstract List<Payload<T>> initializationPayloads();
    public abstract Node<T> getNextNodeFor(Payload<T> payload);
    public abstract boolean isStillRequiredToRun();
    public String getName() {
        return name;
    }

    /**
     * Returns the time taken to process payload and the list of resulting payloads from processing it.
     * Updates the queue statistics due to a message being removed from queue.
     */
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        setCurrentTime(time);
        double timeElapsed = time - previousQueueChangedTime;
        previousQueueChangedTime = time;
        queueStatistics.addMessageProcessedTime(timeElapsed, time - messageArrivalTimes.pop());
        return new Pair<>(0.0, List.of());
    }

    public boolean isOccupiedAtTime(double time) {
        return currentTime > time;
    }

    public void setCurrentTime(double time) {
        this.currentTime = time;
    }
    public Payload<T> createPayload(T message, Node<T> node) {
        return new Payload<>(message, node.getName());
    }

    /**
     * Returns a list of payloads for the {@code message} to all nodes specified in the collection of {@code nodes}.
     */
    public List<Payload<T>> createPayloads(T message, Collection<? extends Node<? extends T>> nodes) {
        List<Payload<T>> payloads = new ArrayList<>();
        for (Node<? extends T> node : nodes) {
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
        if (!(o instanceof Node)) {
            return false;
        }

        Node<?> otherNode = (Node<?>) o;
        return otherNode.name.equals(this.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Adds {@code payload} to the queue for this {@code node} at {@code time}.
     */
    public void addToQueue(double time, Payload<T> payload) {
        double timeElapsed = time - previousQueueChangedTime;
        previousQueueChangedTime = time;
        queueStatistics.addMessageArrivedTime(timeElapsed);
        messageArrivalTimes.add(time);
        queue.add(payload);
    }

    /**
     * Pops first payload in queue.
     * Queue statistics are not updated here as it is calculated only after the payload is processed.
     * As such, it is assumed that the {@code processPayload} some time after this method.
     */
    public Payload<T> popFromQueue() {
        return queue.pop();
    }

    @Override
    public QueueStatistics getQueueStatistics() {
        return queueStatistics;
    }
}
