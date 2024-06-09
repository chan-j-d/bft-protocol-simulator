package simulation.simulator;

import simulation.event.InitializationEvent;
import simulation.event.NodeEvent;
import simulation.event.TimedEvent;
import simulation.network.entity.BFTMessage;
import simulation.network.entity.Node;
import simulation.network.entity.Validator;
import simulation.network.entity.timer.TimerNotifier;
import simulation.network.router.Switch;
import simulation.statistics.ConsensusStatistics;
import simulation.statistics.ConsensusTimeComparator;
import simulation.statistics.QueueStatistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Implementation of the Simulator interface that also serves as a {@code TimerNotifier} for the nodes in simulation.
 *
 * @param <T> Message class used by nodes in the simulation.
 */
public class SimulatorImpl<T extends BFTMessage> implements Simulator, TimerNotifier<Validator<T>> {

    private static final int SNAPSHOT_INTERVAL = 50;
    private static final double TIME_CUTOFF = 1000000000; // for safety

    private PriorityQueue<NodeEvent<T>> eventQueue;
    private int roundCount;
    private List<Validator<T>> nodes;
    private List<List<Switch<T>>> switches;
    private double currentTime;
    private List<Validator<T>> unfinishedValidatorsTracker;
    private int n;
    private int f;

    public SimulatorImpl() {
    }

    /**
     * Sets the validator nodes for the current simulation.
     */
    public void setNodes(List<? extends Validator<T>> validators) {
        this.nodes = new ArrayList<>(validators);
        this.n = validators.size();
        this.f = (n - 1) / 3;
        eventQueue = new PriorityQueue<>();
        for (Node<T> node : validators) {
            eventQueue.add(new InitializationEvent<>(node));
        }

        roundCount = 0;
        currentTime = 0;

        unfinishedValidatorsTracker = new ArrayList<>(validators);
    }

    /**
     * Sets the switches for the current simulation.
     */
    public void setSwitches(List<List<Switch<T>>> switches) {
        this.switches = switches;
    }

    public List<Validator<T>> getNodes() {
        return nodes;
    }

    /**
     * Simulates a singular event in the event queue and outputs the contents of the event.
     * Each event involves a node and some possible action caused by the node.
     * Once a node is no longer required to run, it is removed from the unfinished list.
     * The simulation is considered 'over' once no more nodes are unfinished.
     */
    @Override
    public Optional<String> simulate() {
        NodeEvent<T> nextEvent = eventQueue.poll();
        assert nextEvent != null; // isSimulationOver should be used to check before calling this function
        currentTime = nextEvent.getTime();
        if (currentTime > TIME_CUTOFF) {
            return Optional.empty();
        }
        Node<T> node = nextEvent.getNode();
        List<NodeEvent<T>> resultingEvents = nextEvent.simulate();

        if (!node.isStillRequiredToRun()) {
            unfinishedValidatorsTracker.remove(node);
        }

        eventQueue.addAll(resultingEvents);
        roundCount++;

        Optional<String> finalString = (node instanceof Switch<?>)
                ? Optional.empty()
                : Optional.of(nextEvent.toString());
        if (roundCount % SNAPSHOT_INTERVAL == 0) {
            finalString = finalString.map(s -> s + "\n\nSnapshot:\n" + getSnapshotOfNodes() + "\n");
        }
        return finalString;
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
        ConsensusStatistics fastestRunConsensusStats = nodes.stream()
                .map(v -> v.getConsensusStatistics(1))
                .sorted(new ConsensusTimeComparator())
                .limit(n - f)
                .reduce(ConsensusStatistics::combineStatistics).orElseThrow();
        ConsensusStatistics remainderRunConsensusStats = nodes.stream()
                .map(v -> v.getConsensusStatistics(1))
                .sorted(new ConsensusTimeComparator().reversed())
                .limit(f)
                .reduce(ConsensusStatistics::combineStatistics).orElseThrow();

        Comparator<Validator<T>> consensusTimeComparatorForQueue = (n1, n2) ->
                new ConsensusTimeComparator().compare(n1.getConsensusStatistics(1), n2.getConsensusStatistics(1));
        QueueStatistics fastestRunValidatorQueueStats = nodes.stream()
                .sorted(consensusTimeComparatorForQueue)
                .map(Node::getQueueStatistics)
                .limit(n - f)
                .reduce(QueueStatistics::combineStatistics).orElseThrow();
        QueueStatistics remainderRunValidatorQueueStats = nodes.stream()
                .sorted(consensusTimeComparatorForQueue.reversed())
                .map(Node::getQueueStatistics)
                .limit(f)
                .reduce(QueueStatistics::combineStatistics).orElseThrow();

        List<QueueStatistics> switchStatistics = switches.stream()
                .map(group -> group.stream().map(Node::getQueueStatistics)
                        .reduce(QueueStatistics::combineStatistics).orElseThrow())
                .collect(Collectors.toList());
        return new RunResults(fastestRunConsensusStats, remainderRunConsensusStats,
                fastestRunValidatorQueueStats, remainderRunValidatorQueueStats, switchStatistics);
    }

    @Override
    public void notifyAtTime(Validator<T> node, double time, int id, int timerCount) {
        eventQueue.add(new TimedEvent<>(time, node, id, timerCount));
    }

    @Override
    public double getTime() {
        return currentTime;
    }
}
