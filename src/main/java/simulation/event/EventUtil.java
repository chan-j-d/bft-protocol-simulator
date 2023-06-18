package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains static event utility methods for {@code TimedEvent} and its subclasses.
 */
public class EventUtil {

    /**
     * Returns a list of queue events consisting of payloads being queued at their respective next hop node.
     *
     * @param time Time when the payload is queued.
     * @param currentNode Node sending the {@code payloads}.
     * @param payloads Payloads being sent by {@code currentNode}.
     * @return List of queue events of payloads being queued.
     * @param <T> Message class being carried by the payload.
     */
    public static <T> List<NodeEvent<T>> convertPayloadsToQueueEvents(double time, Node<T> currentNode,
            List<? extends Payload<T>> payloads) {
        List<NodeEvent<T>> events = new ArrayList<>();
        for (Payload<T> payload : payloads) {
            events.add(new QueueEvent<>(time, currentNode.getNextNodeFor(payload), payload));
        }
        return events;
    }
}
