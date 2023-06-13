package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;

import java.util.List;

public class QueueEvent<T> extends NodeEvent<T> {

    private final Payload<T> payload;
    public QueueEvent(double time, Node<T> node, Payload<T> payload) {
        super(time, node);
        this.payload = payload;
    }

    @Override
    public List<NodeEvent<T>> simulate() {
        Node<T> node = getNode();
        boolean isNodeEmpty = node.isEmpty();
        node.addToQueue(getTime(), payload);
        return isNodeEmpty ? List.of(new PollQueueEvent<>(getTime(), node)) : List.of();
    }

    @Override
    public String toString() {
        return super.toString() + " (QueueEvent): payload queued at " + getNode() + " (" + payload + ")";
    }
}
