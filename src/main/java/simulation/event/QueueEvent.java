package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;

import java.util.List;

public class QueueEvent<T> extends Event {

    private NetworkNode<T> node;
    private Payload<T> payload;
    public QueueEvent(double time, NetworkNode<T> node, Payload<T> payload) {
        super(time);
        this.node = node;
        this.payload = payload;
    }

    @Override
    public List<Event> simulate() {
        boolean isNodeEmpty = node.isEmpty();
        node.addToQueue(payload);
        return isNodeEmpty ? List.of(new PollQueueEvent<>(getTime(), node)) : List.of();
    }

    @Override
    public String toString() {
        return super.toString() + " (QueueEvent): payload queued at " + node + " (" + payload + ")";
    }
}
