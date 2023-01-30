package simulation.simulator;

import simulation.event.Event;
import simulation.event.InitializationEvent;
import simulation.event.TimedEvent;
import simulation.network.entity.NetworkNode;

import java.util.List;
import java.util.PriorityQueue;

public class Simulator {

    private PriorityQueue<Event> eventQueue;
    private int roundCount;
    private List<NetworkNode> nodes;
    private static int snapshotInterval = 10;
    public Simulator(List<NetworkNode> nodes) {
        this.nodes = nodes;
        eventQueue = new PriorityQueue<>();
        for (NetworkNode node : nodes) {
            eventQueue.add(new InitializationEvent(node));
            eventQueue.add(new TimedEvent(node.getNextNotificationTime(), node));
        }

        roundCount = 0;
    }

    public String simulate() {
        Event nextEvent = eventQueue.poll();
        List<Event> resultingEvents = nextEvent.simulate();
        eventQueue.addAll(resultingEvents);
        roundCount++;

        String finalString = nextEvent.toString();
        if (roundCount % snapshotInterval == 0) {
            finalString = finalString + "\n\nSnapshot:\n" + getSnapshotOfNodes() + "\n" + eventQueue + "\n";
        }
        return finalString;
    }

    private String getSnapshotOfNodes() {
        if (nodes.isEmpty()) {
            return "";
        } else {
            return nodes.stream()
                    .map(NetworkNode::toString)
                    .reduce((x, y) -> x + "\n" + y)
                    .get();
        }
    }

    public boolean isSimulationOver() {
        return eventQueue.isEmpty();
    }
}
