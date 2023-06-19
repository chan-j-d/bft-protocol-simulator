package simulation.network.entity;

import simulation.simulator.QueueResults;
import simulation.statistics.QueueStatistics;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class Node<T> implements QueueResults {

    private final String name;
    private final LinkedList<Payload<T>> queue;
    private final LinkedList<Double> messageArrivalTimes;
    private final QueueStatistics queueStatistics;
    private double currentTime;
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
    public Payload<T> createPayloads(T message, Node<T> node) {
        return new Payload<>(message, node.getName());
    }

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

    public void addToQueue(double time, Payload<T> payload) {
        double timeElapsed = time - previousQueueChangedTime;
        previousQueueChangedTime = time;
        queueStatistics.addMessageArrivedTime(timeElapsed);
        messageArrivalTimes.add(time);
        queue.add(payload);
    }

    public Payload<T> popFromQueue(double time) {
        return queue.pop();
    }

    @Override
    public QueueStatistics getQueueStatistics() {
        return queueStatistics;
    }
}
