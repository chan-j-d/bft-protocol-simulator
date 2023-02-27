package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;

import java.util.List;

public class PollQueueEvent<T> extends Event {

    private Node<T> node;
    private Payload<T> payload;

    public PollQueueEvent(double time, Node<T> node) {
        super(time);
        this.node = node;
    }

    @Override
    public List<Event> simulate() {
        if (!node.isEmpty() && !node.isOccupiedAtTime(getTime())) {
            payload = node.popFromQueue();
            return List.of(new ProcessHeaderEvent<T>(getTime(), node, payload));
        } else {
            return List.of();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " (PollQueueEvent): Polling from " + node + " (" + payload + ")";
    }
}
