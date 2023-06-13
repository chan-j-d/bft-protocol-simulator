package simulation.simulator;

import simulation.event.InitializationEvent;
import simulation.event.NodeEvent;
import simulation.event.TimedEvent;
import simulation.network.entity.Node;
import simulation.network.entity.TimedNode;
import simulation.network.entity.TimerNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

public class Simulator<T> implements TimerNotifier<T> {

    private static final int SNAPSHOT_INTERVAL = 1000000;
    private static final double TIME_CUTOFF = 10000000; // for safety

    private PriorityQueue<NodeEvent<T>> eventQueue;
    private int roundCount;
    private List<Node<T>> nodes;
    private double currentTime;
    private List<Node<T>> unfinishedNodesTracker;

    public Simulator() {
    }
    public void setNodes(List<? extends Node<T>> nodes) {
        this.nodes = new ArrayList<>(nodes);
        eventQueue = new PriorityQueue<>();
        for (Node<T> node : nodes) {
            eventQueue.add(new InitializationEvent<>(node));
        }

        roundCount = 0;
        currentTime = 0;

        unfinishedNodesTracker = new ArrayList<>(nodes);
    }

    public Optional<String> simulate() {
        NodeEvent<T> nextEvent = eventQueue.poll();
        assert nextEvent != null; // isSimulationOver should be used to check before calling this function
        currentTime = nextEvent.getTime();
        if (currentTime > TIME_CUTOFF) {
            return Optional.empty();
        }
        List<NodeEvent<T>> resultingEvents = nextEvent.simulate();

        Node<T> node = nextEvent.getNode();
        if (!node.isStillRequiredToRun()) {
            unfinishedNodesTracker.remove(node);
        }

        eventQueue.addAll(resultingEvents);
        roundCount++;

        String finalString = nextEvent.toString();
        if (roundCount % SNAPSHOT_INTERVAL == 0) {
            finalString = finalString + "\n\nSnapshot:\n" + getSnapshotOfNodes() + "\n" + eventQueue + "\n";
        }
        return Optional.of(finalString);
    }

    public String getSnapshotOfNodes() {
        if (nodes.isEmpty()) {
            return "";
        } else {
            return nodes.stream()
                    .map(Node::toString)
                    .reduce((x, y) -> x + "\n" + y)
                    .get();
        }
    }

    public boolean isSimulationOver() {
        return unfinishedNodesTracker.isEmpty() || getTime() > TIME_CUTOFF;
    }

    @Override
    public void notifyAtTime(TimedNode<T> node, double time, int count) {
        eventQueue.add(new TimedEvent<T>(time, node, count));
    }

    @Override
    public double getTime() {
        return currentTime;
    }
}
