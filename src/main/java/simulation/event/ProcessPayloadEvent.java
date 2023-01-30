package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.TestGenerator;

import java.util.ArrayList;
import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

public class ProcessPayloadEvent extends RandomDurationEvent {

    private static double seed = 0; // TODO update seed and ways to change seed
    //private static RandomNumberGenerator rng = new ExponentialDistribution(2);
    private static RandomNumberGenerator rng = new TestGenerator(0);

    private NetworkNode node;
    private Payload payload;

    public ProcessPayloadEvent(double time, NetworkNode node, Payload payload) {
        super(time);
        this.node = node;
        this.payload = payload;
    }

    @Override
    public List<Event> simulate() {
        double processingEndTime = generateRandomDuration() + getTime();
        List<Payload> processedPayloads = node.processPayload(getTime(), payload);
        List<Event> eventList =
                new ArrayList<>(convertPayloadsToQueueEvents(processingEndTime, node, processedPayloads));

        if (!node.isEmpty()) {
            eventList.add(new ProcessHeaderEvent(processingEndTime, node, node.popFromQueue()));
        }

        double nextNotificationTime = node.getNextNotificationTime();
        if (nextNotificationTime != -1) {
            eventList.add(new TimedEvent(nextNotificationTime, node));
        }
        return eventList;
    }

    @Override
    public double generateRandomDuration() {
        return rng.generateRandomNumber();
    }

    @Override
    public String toString() {
        return super.toString() + " (ProcessPayload): Processing payload at " + node + " (" + payload + ")";
    }
}
