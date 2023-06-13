package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;

import java.util.List;

public class PollQueueEvent<T> extends NodeEvent<T> {

    private Payload<T> payload;

    public PollQueueEvent(double time, Node<T> node) {
        super(time, node);
    }

    @Override
    public List<NodeEvent<T>> simulate() {
        Node<T> node = getNode();
        if (!node.isEmpty() && !node.isOccupiedAtTime(getTime())) {
            payload = node.popFromQueue(getTime());
            return List.of(new ProcessPayloadEvent<>(getTime(), node, payload));
        } else {
            return List.of();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " (PollQueueEvent): Polling from " + getNode() + " (" + payload + ")";
    }
}
