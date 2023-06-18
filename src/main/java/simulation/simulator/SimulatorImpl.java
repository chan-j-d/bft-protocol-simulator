package simulation.simulator;

import simulation.event.InitializationEvent;
import simulation.event.NodeEvent;
import simulation.event.TimedEvent;
import simulation.network.entity.Node;
import simulation.network.entity.TimedNode;
import simulation.network.entity.Validator;
import simulation.network.entity.timer.TimerNotifier;
import simulation.network.router.Switch;
import simulation.statistics.ConsensusStatistics;
import simulation.statistics.QueueStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class SimulatorImpl<T> implements Simulator, TimerNotifier<T> {

    private static final int SNAPSHOT_INTERVAL = 1000000;
    private static final double TIME_CUTOFF = 10000000; // for safety

    private PriorityQueue<NodeEvent<T>> eventQueue;
    private int roundCount;
    private List<Validator<T>> nodes;
    private List<List<Switch<T>>> switches;
    private double currentTime;
    private List<Validator<T>> unfinishedValidatorsTracker;

    public SimulatorImpl() {
    }

    public void setNodes(List<? extends Validator<T>> validators) {
        this.nodes = new ArrayList<>(validators);
        eventQueue = new PriorityQueue<>();
        for (Node<T> node : validators) {
            eventQueue.add(new InitializationEvent<>(node));
        }

        roundCount = 0;
        currentTime = 0;

        unfinishedValidatorsTracker = new ArrayList<>(validators);
    }

    public void setSwitches(List<List<Switch<T>>> switches) {
        this.switches = switches;
    }

    public List<Validator<T>> getNodes() {
        return nodes;
    }

    @Override
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
            unfinishedValidatorsTracker.remove(node);
        }

        eventQueue.addAll(resultingEvents);
        roundCount++;

        String finalString = nextEvent.toString();
        if (roundCount % SNAPSHOT_INTERVAL == 0) {
            finalString = finalString + "\n\nSnapshot:\n" + getSnapshotOfNodes() + "\n" + eventQueue + "\n";
        }
        return Optional.of(finalString);
    }

    @Override
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

    @Override
    public boolean isSimulationOver() {
        return unfinishedValidatorsTracker.isEmpty() || getTime() > TIME_CUTOFF;
    }

    @Override
    public RunResults getRunResults() {
        ConsensusStatistics runConsensusStats = nodes.stream()
                .map(Validator::getConsensusStatistics)
                .reduce(ConsensusStatistics::combineStatistics).orElseThrow();

        QueueStatistics runValidatorQueueStats = nodes.stream()
                .map(Node::getQueueStatistics)
                .reduce(QueueStatistics::combineStatistics).orElseThrow();

        List<QueueStatistics> switchStatistics = switches.stream()
                .map(group -> group.stream().map(Node::getQueueStatistics)
                        .reduce(QueueStatistics::combineStatistics).orElseThrow())
                .collect(Collectors.toList());
        return new RunResults(runConsensusStats, runValidatorQueueStats, switchStatistics);
    }

    @Override
    public void notifyAtTime(TimedNode<T> node, double time, int timerCount) {
        eventQueue.add(new TimedEvent<T>(time, node, timerCount));
    }

    @Override
    public double getTime() {
        return currentTime;
    }
}
