package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.TestGenerator;

import java.util.ArrayList;
import java.util.List;

public class ProcessHeaderEvent<T> extends Event {

    private static RandomNumberGenerator rng = new TestGenerator(0);
    private NetworkNode<T> node;
    private Payload<T> payload;
    private double processingEndTime;

    public ProcessHeaderEvent(double time, NetworkNode<T> node, Payload<T> payload) {
        super(time);
        this.node = node;
        this.payload = payload;
    }

    @Override
    public List<Event> simulate() {
        List<Event> eventList = new ArrayList<>();
        Pair<Double, Boolean> durationBooleanPair = node.isPayloadDestination(payload);
        processingEndTime = durationBooleanPair.first() + getTime();
        if (durationBooleanPair.second()) {
            eventList.add(new ProcessPayloadEvent<>(processingEndTime, node, payload));
        } else {
            NetworkNode<T> nextHopNode = node.getNextNodeFor(payload);
            eventList.add(new QueueEvent<>(processingEndTime, nextHopNode, payload));
            eventList.add(new PollQueueEvent<>(processingEndTime, node));
        }

        return eventList;
    }

    @Override
    public String toString() {
        return String.format("%s-%.3f (ProcessHeader): Processing header at %s",
                super.toString(), processingEndTime, node);
    }
}
