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

    @Override
    public double getNextNotificationTime() {
        return -1;
    }

    @Override
    public List<Payload> notifyTime(double time) {
        return List.of();
    }
}
