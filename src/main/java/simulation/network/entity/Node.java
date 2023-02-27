package simulation.network.entity;

import simulation.util.Pair;
import simulation.util.Queueable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class Node<T> implements Queueable<Payload<T>> {

    private List<Node<T>> neighbors;
    private String name;
    private LinkedList<Payload<T>> queue;
    private double currentTime;

    public Node(String name) {
        this.name = name;
        this.neighbors = new ArrayList<>();
        this.queue = new LinkedList<>();
        this.currentTime = 0;
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

    public abstract Node<T> getNextNodeFor(Payload<T> payload);

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
