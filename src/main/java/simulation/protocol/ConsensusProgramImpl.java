package simulation.protocol;

import simulation.network.entity.BFTMessage;
import simulation.network.entity.Payload;
import simulation.network.entity.timer.TimerNotifier;
import simulation.statistics.ConsensusStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Partial implementation of a consensus program that fulfills general responsibilities.
 * Provides implementation for timer and consensus statistics responsibilities.
 */
public abstract class ConsensusProgramImpl<T extends BFTMessage> implements ConsensusProgram<T> {

    /**
     * Stores payloads while node is processing a message.
     * All payloads are retrieved and sent out after message processing.
     */
    private List<Payload<T>> tempPayloadStore;

    private final ConsensusStatistics statistics;
    private final Map<Integer, String> idNodeNameMap;
    private final TimerNotifier<ConsensusProgram<T>> timerNotifier;
    private int timerCount; // Used to differentiate multiple timers in the same instance & round
    private double timeoutTime;

    /**
     * @param idNodeNameMap Map of id to node names.
     * @param timerNotifier For tracking time and setting timers.
     */
    public ConsensusProgramImpl(Map<Integer, String> idNodeNameMap, TimerNotifier<ConsensusProgram<T>> timerNotifier) {
        this.idNodeNameMap = idNodeNameMap;
        this.tempPayloadStore = new ArrayList<>();
        this.timerNotifier = timerNotifier;
        this.timeoutTime = 0;
        this.timerCount = 0;
        this.statistics = new ConsensusStatistics(getStates());
    }

    protected String getNameFromId(int id) {
        return idNodeNameMap.get(id);
    }

    /**
     * Tracks consensus related statistics.
     */
    private void registerTimeElapsed(double time) {
        statistics.addTime(getState(), time);
        statistics.addRoundTime((getNumConsecutiveFailure() + 1), getState(), time);
        statistics.setConsensusCount(getConsensusCount());
    }

    @Override
    public void registerMessageProcessed(T message, double timeTaken) {
        registerTimeElapsed(timeTaken);
        statistics.addMessageCountForState(message.getType());
    }

    @Override
    public ConsensusStatistics getStatistics() {
        return statistics;
    }

    // Payload handling responsibilities during processing

    /**
     * Returns payloads generated from a processing step.
     * Empties the payload list.
     *
     * @return List of payloads that were generated from a processing step.
     */
    protected List<Payload<T>> getProcessedPayloads() {
        List<Payload<T>> payloads = tempPayloadStore;
        tempPayloadStore = new ArrayList<>();
        return payloads;
    }

    /**
     * Sends {@code message} to destination {@code nodeName}.
     */
    protected void sendMessage(T message, String nodeName) {
        tempPayloadStore.add(createPayload(message, nodeName));
    }

    /**
     * Sends {@code message} to all nodes in the collection of {@code nodeNames}.
     */
    protected void broadcastMessage(T message, Collection<String> nodeNames) {
        tempPayloadStore.addAll(createPayloads(message, nodeNames));
    }

    protected void broadcastMessageToAll(T message) {
        broadcastMessage(message, idNodeNameMap.values());
    }

    public Payload<T> createPayload(T message, String nodeName) {
        return new Payload<>(message, nodeName);
    }

    /**
     * Returns a list of payloads for the {@code message} to all nodes specified in the collection of {@code nodeNames}.
     */
    public List<Payload<T>> createPayloads(T message, Collection<String> nodeNames) {
        List<Payload<T>> payloads = new ArrayList<>();
        for (String nodeName : nodeNames) {
            payloads.add(new Payload<>(message, nodeName));
        }
        return payloads;
    }

    // Timer utilities

    public double getTime() {
        return timerNotifier.getTime();
    }

    /**
     * @param time Time to be notified.
     * @param id Unique identification for the timer.
     */
    public void notifyAtTime(double time, int id) {
        timerNotifier.notifyAtTime(this, time, id);
    }

    /**
     * @param duration Starts a timer for a given duration starting at the current time.
     */
    protected void startTimer(double duration) {
        timeoutTime = getTime() + duration;
        notifyAtTime(timeoutTime, ++timerCount); // Every time a timer starts, a unique one is set.
    }

    protected double getTimeoutTime() {
        return timeoutTime;
    }

    /**
     * Returns a list of payloads after notifying the node at the requested time.
     *
     * @param timerCount int identification for the specific timer.
     * @return List of payloads to be sent at the given time.
     */
    public List<Payload<T>> notifyTime(int timerCount) {
        if (timerCount == this.timerCount) {
            statistics.addRoundChangeStateCount(getState());
            return onTimerExpiry();
        }
        return List.of();
    }

    /**
     * Operation to be called on timer expiry.
     */
    protected abstract List<Payload<T>> onTimerExpiry();
}
