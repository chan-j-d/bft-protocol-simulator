package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

/**
 * Processes the given {@code payload} at {@code node}.
 * This should be followed by a ProcessedPayloadEvent occuring at the end of the delay.
 * The actual processing is done here.
 *
 * @param <T> Message class carried by payloads generated by the type of protocol being simulated.
 */
public class ProcessingDelayEvent<T> extends NodeEvent<T> {

    private final Payload<T> payload;

    public ProcessingDelayEvent(double time, Node<T> node, Payload<T> payload) {
        super(time, node);
        this.payload = payload;
    }

    @Override
    public List<NodeEvent<T>> simulate() {
        Node<T> node = getNode();
        Pair<Double, List<Payload<T>>> durationPayloadsPair = node.processPayload(getTime(), payload);
        List<Payload<T>> processedPayloads = durationPayloadsPair.second();
        double processingEndTime = getTime() + durationPayloadsPair.first();
        List<NodeEvent<T>> eventList =
                new ArrayList<>(convertPayloadsToQueueEvents(processingEndTime, node, processedPayloads));
        eventList.add(new ProcessedPayloadEvent<>(processingEndTime, node));
        return eventList;
    }

    @Override
    public String toString() {
        return String.format("%s (ProcessingDelay): Processing payload at %s (%s)",
                super.toString(), getNode(), payload);
    }
}
