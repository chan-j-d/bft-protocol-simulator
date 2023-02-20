package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

public class ProcessPayloadEvent extends RandomDurationEvent {

    private static double seed = 500; // TODO update seed and ways to change seed
    private static RandomNumberGenerator rng = new ExponentialDistribution(1);
    //private static RandomNumberGenerator rng = new TestGenerator(0);

    private NetworkNode node;
    private Payload payload;
    private double processingEndTime;

    public ProcessPayloadEvent(double time, NetworkNode node, Payload payload) {
        super(time);
        this.node = node;
        this.payload = payload;
        this.processingEndTime = time + generateRandomDuration();
    }

    @Override
    public List<Event> simulate() {
        List<Payload> processedPayloads = node.processPayload(processingEndTime, payload);
        List<Event> eventList =
                new ArrayList<>(convertPayloadsToQueueEvents(processingEndTime, node, processedPayloads));
        eventList.add(new PollQueueEvent(processingEndTime, node));
        return eventList;
    }

    @Override
    public double generateRandomDuration() {
        return rng.generateRandomNumber();
    }

    @Override
    public String toString() {
        return String.format("%s-%.3f (ProcessPayload): Processing payload at %s (%s)",
                super.toString(), processingEndTime, node, payload);
    }
}
