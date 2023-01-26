package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;

import java.util.ArrayList;
import java.util.List;

public class InitializationEvent extends Event {

    public static final double START_TIME = 0;

    private NetworkNode node;

    public InitializationEvent(NetworkNode node) {
        super(START_TIME);
        this.node = node;
    }

    @Override
    public List<Event> simulate() {
        List<Event> eventList = new ArrayList<>();
        List<Payload> payloads = node.initializationPayloads();
        for (Payload payload : payloads) {
            NetworkNode nextHopNode = node.getNextNodeFor(payload);
            eventList.add(new QueueEvent(START_TIME, nextHopNode, payload));
        }
        return eventList;
    }

    @Override
    public String toString() {
        return super.toString() + " (Initialization): " + node;
    }
}
