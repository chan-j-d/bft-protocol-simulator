package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;

import java.util.ArrayList;
import java.util.List;

public class InitializationEvent<T> extends Event {

    public static final double START_TIME = 0;

    private NetworkNode<T> node;

    public InitializationEvent(NetworkNode<T> node) {
        super(START_TIME);
        this.node = node;
    }

    @Override
    public List<Event> simulate() {
        List<Event> eventList = new ArrayList<>();
        List<Payload<T>> payloads = node.initializationPayloads();
        for (Payload<T> payload : payloads) {
            NetworkNode<T> nextHopNode = node.getNextNodeFor(payload);
            eventList.add(new QueueEvent<>(START_TIME, nextHopNode, payload));
        }
        return eventList;
    }

    @Override
    public String toString() {
        return super.toString() + " (Initialization): " + node;
    }
}
