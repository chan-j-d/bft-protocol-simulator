package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.TestGenerator;

import java.util.ArrayList;
import java.util.List;

public class ProcessHeaderEvent<T> extends RandomDurationEvent {

    private static double seed = 0; // TODO update seed and ways to change seed
    //private static RandomNumberGenerator rng = new ExponentialDistribution(1);
    private static RandomNumberGenerator rng = new TestGenerator(0);
    private NetworkNode<T> node;
    private Payload<T> payload;
    private double processingEndTime;
    public ProcessHeaderEvent(double time, NetworkNode<T> node, Payload<T> payload) {
        super(time);
        this.node = node;
        this.payload = payload;
        this.processingEndTime = time + generateRandomDuration();
    }

    @Override
    public List<Event> simulate() {
        List<Event> eventList = new ArrayList<>();
        if (node.isPayloadDestination(payload)) {
            eventList.add(new ProcessPayloadEvent<>(processingEndTime, node, payload));
        } else {
            NetworkNode<T> nextHopNode = node.getNextNodeFor(payload);
            eventList.add(new QueueEvent<>(processingEndTime, nextHopNode, payload));
            eventList.add(new PollQueueEvent<>(processingEndTime, node));
        }

        return eventList;
    }

    @Override
    public double generateRandomDuration() {
        return rng.generateRandomNumber();
    }

    @Override
    public String toString() {
        return String.format("%s-%.3f (ProcessHeader): Processing header at %s",
                super.toString(), processingEndTime, node);
    }
}
