package simulation.network.entity;

import java.util.List;

public class Switch extends NetworkNode {

    public Switch(String name, List<NetworkNode> neighbors) {
        super(name, neighbors);
    }

    @Override
    public List<Payload> processPayload(double time, Payload payload) {
        return List.of(payload);
    }

    @Override
    public List<Payload> initializationPayloads() {
        return List.of();
    }
}
