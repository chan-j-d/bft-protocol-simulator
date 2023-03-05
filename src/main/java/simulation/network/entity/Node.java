package simulation.network.entity;

import simulation.statistics.QueueStatistics;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class Node<T> {

    private final String name;
    private final LinkedList<Payload<T>> queue;
    private final LinkedList<Double> messageArrivalTimes;
    private final QueueStatistics queueStatistics;
    private double currentTime;

    public Node(String name) {
        this.name = name;
        this.queue = new LinkedList<>();
        this.currentTime = 0;
        this.queueStatistics = new QueueStatistics();
        this.messageArrivalTimes = new LinkedList<>();
    }

    public abstract List<Payload<T>> initializationPayloads();
    public abstract boolean isDone();
    public abstract Node<T> getNextNodeFor(Payload<T> payload);

    public String getName() {
        return name;
    }
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        this.currentTime = time;
        return new Pair<>(0.0, List.of());
    }

    public boolean isOccupiedAtTime(double time) {
        return currentTime > time;
    }

    public void setCurrentTime(double time) {
        this.currentTime = time;
    }
    public double getCurrentTime() {
        return currentTime;
    }
    public Payload<T> sendMessage(T message, Node<T> node) {
        return new Payload<>(message, node.getName());
    }

    public List<Payload<T>> sendMessage(T message, Collection<? extends Node<T>> nodes) {
        List<Payload<T>> payloads = new ArrayList<>();
        for (Node<T> node : nodes) {
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
        double timeElapsed = time - getCurrentTime();
        setCurrentTime(time);
        queueStatistics.addMessageArrived(timeElapsed);
        messageArrivalTimes.add(time);
        queue.add(payload);
    }

    public Payload<T> popFromQueue(double time) {
        queueStatistics.addMessageQueueTime(time - messageArrivalTimes.pop());
        return queue.pop();
    }

    public QueueStatistics getQueueStatistics() {
        return queueStatistics;
    }
}
