package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.TestGenerator;

import java.util.ArrayList;
import java.util.List;

public class ProcessHeaderEvent extends RandomDurationEvent {

    private static double seed = 0; // TODO update seed and ways to change seed
    private static RandomNumberGenerator rng = new ExponentialDistribution(1);
    //private static RandomNumberGenerator rng = new TestGenerator();
    private NetworkNode node;
    private Payload payload;
    public ProcessHeaderEvent(double time, NetworkNode node, Payload payload) {
        super(time);
        this.node = node;
        this.payload = payload;
    }

    @Override
    public List<Event> simulate() {
        double processingEndTime = generateRandomDuration() + getTime();
        List<Event> eventList = new ArrayList<>();
        if (node.isPayloadDestination(payload)) {
            eventList.add(new ProcessPayloadEvent(processingEndTime, node, payload));
        } else {
            NetworkNode nextHopNode = node.getNextNodeFor(payload);
            eventList.add(new QueueEvent(processingEndTime, nextHopNode, payload));
            if (!node.isEmpty()) {
                eventList.add(new ProcessHeaderEvent(processingEndTime, node, node.popFromQueue()));
            }
        }

        return eventList;
    }

    @Override
    public double generateRandomDuration() {
        return rng.generateRandomNumber();
    }

    @Override
    public String toString() {
        return super.toString() + " (ProcessHeader): Processing header at " + node;
    }
}
