package simulation.network.entity;

import simulation.util.Pair;

import java.util.List;

public class Switch<T> extends NetworkNode<T> {

    public Switch(String name, List<NetworkNode<T>> neighbors) {
        super(name, neighbors);
    }

    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        return new Pair<>(0.0, List.of(payload));
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
