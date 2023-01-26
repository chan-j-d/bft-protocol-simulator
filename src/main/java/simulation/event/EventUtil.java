package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;

import java.util.ArrayList;
import java.util.List;

public class EventUtil {

    public static List<Event> convertPayloadsToQueueEvents(double time, NetworkNode currentNode,
            List<? extends Payload> payloads) {
        List<Event> events = new ArrayList<>();
        for (Payload payload : payloads) {
            events.add(new QueueEvent(time, currentNode.getNextNodeFor(payload), payload));
        }
        return events;
    }
}
