package simulation.simulator;

import simulation.event.Event;
import simulation.event.InitializationEvent;
import simulation.event.TimedEvent;
import simulation.network.entity.TimedNetworkNode;
import simulation.network.entity.NetworkNode;
import simulation.network.entity.NodeTimerNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Simulator<T> implements NodeTimerNotifier<T> {

    private static final int SNAPSHOT_INTERVAL = 100;

    private PriorityQueue<Event> eventQueue;
    private int roundCount;
    private List<NetworkNode<T>> nodes;
    private double currentTime;

    public Simulator() {
    }
    public void setNodes(List<? extends NetworkNode<T>> nodes) {
        this.nodes = new ArrayList<>(nodes);
        eventQueue = new PriorityQueue<>();
        for (NetworkNode<T> node : nodes) {
            eventQueue.add(new InitializationEvent(node));
        }

        roundCount = 0;
        currentTime = 0;
    }

    public String simulate() {
        Event nextEvent = eventQueue.poll();
        assert nextEvent != null; // isSimulationOver should be used to check before calling this function
        currentTime = nextEvent.getTime();
        List<Event> resultingEvents = nextEvent.simulate();
        eventQueue.addAll(resultingEvents);
        roundCount++;

        String finalString = nextEvent.toString();
        if (roundCount % SNAPSHOT_INTERVAL == 0) {
            finalString = finalString + "\n\nSnapshot:\n" + getSnapshotOfNodes() + "\n" + eventQueue + "\n";
        }
        return finalString;
    }

    public String getSnapshotOfNodes() {
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

    @Override
    public void notifyAtTime(TimedNetworkNode<T> node, double time, T message) {
        eventQueue.add(new TimedEvent<T>(time, node, message));
    }

    @Override
    public double getTime() {
        return currentTime;
    }
}
