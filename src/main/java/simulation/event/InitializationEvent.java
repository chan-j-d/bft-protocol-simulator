package simulation.event;

import simulation.network.entity.Node;
import simulation.network.entity.Payload;

import java.util.ArrayList;
import java.util.List;

public class InitializationEvent<T> extends NodeEvent<T> {

    public static final double START_TIME = 0;

    public InitializationEvent(Node<T> node) {
        super(START_TIME, node);
    }

    @Override
    public List<NodeEvent<T>> simulate() {
        List<NodeEvent<T>> eventList = new ArrayList<>();
        List<Payload<T>> payloads = getNode().initializationPayloads();
        for (Payload<T> payload : payloads) {
            Node<T> nextHopNode = getNode().getNextNodeFor(payload);
            eventList.add(new QueueEvent<>(START_TIME, nextHopNode, payload));
        }
        return eventList;
    }

    @Override
    public String toString() {
        return super.toString() + " (Initialization): " + getNode();
    }
}
