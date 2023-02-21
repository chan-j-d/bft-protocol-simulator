package simulation.network.entity;

import java.util.List;

public class Switch<T> extends NetworkNode<T> {

    public Switch(String name, List<NetworkNode<T>> neighbors) {
        super(name, neighbors);
    }

    @Override
    public List<Payload<T>> processPayload(double time, Payload<T> payload) {
        return List.of(payload);
    }

    @Override
    public List<Payload<T>> initializationPayloads() {
        return List.of();
    }
}
