package simulation.simulator;

import simulation.event.Event;
import simulation.event.InitializationEvent;
import simulation.event.TimedEvent;
import simulation.network.entity.NetworkNode;

import java.util.List;
import java.util.PriorityQueue;

public class Simulator {

    private PriorityQueue<Event> eventQueue;
    public Simulator(List<NetworkNode> nodes) {
        eventQueue = new PriorityQueue<>();
        for (NetworkNode node : nodes) {
            eventQueue.add(new InitializationEvent(node));
            eventQueue.add(new TimedEvent(node.getNextNotificationTime(), node));
        }
    }

    public String simulate() {
        Event nextEvent = eventQueue.poll();
        List<Event> resultingEvents = nextEvent.simulate();
        eventQueue.addAll(resultingEvents);
        return nextEvent.toString();
    }

    public boolean isSimulationOver() {
        return eventQueue.isEmpty();
    }
}
