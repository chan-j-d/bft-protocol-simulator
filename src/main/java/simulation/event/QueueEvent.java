package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;

import java.util.ArrayList;
import java.util.List;

/**
 * Queues the given {@code payloads} from {@code node} at the next hop nodes.
 *
 * @param <T> Message class carried by payloads generated by the type of protocol being simulated.
 */
public class QueueEvent<T> extends NodeEvent<T> {

    private final List<Payload<T>> payloads;
    public QueueEvent(double time, Node<T> node, List<Payload<T>> payloads) {
        super(time, node);
        this.payloads = payloads;
    }

    @Override
    public List<NodeEvent<T>> simulate() {
        Node<T> node = getNode();
        List<NodeEvent<T>> events = new ArrayList<>();
        for (Payload<T> payload : payloads) {
            Node<T> destination = node.getNextNodeFor(payload);
            boolean wasDestinationEmpty = destination.isEmpty();
            destination.addToQueue(getTime(), payload);
            if (wasDestinationEmpty && !destination.isOccupied()) {
                node.setOccupied();
                // The message has to be added to queue before being popped due to side effects in addToQueue.
                events.add(new ProcessingDelayEvent<>(getTime(), destination, destination.popFromQueue()));
            }
        }
        return events;
    }

    @Override
    public String toString() {
        return String.format("%s (QueueEvent): %s - %s", super.toString(), getNode(), payloads);
    }
}
