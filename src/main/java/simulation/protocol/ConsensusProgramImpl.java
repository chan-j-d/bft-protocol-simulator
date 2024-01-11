package simulation.protocol;

import simulation.network.entity.BFTMessage;
import simulation.network.entity.timer.TimerNotifier;
import simulation.statistics.ConsensusStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Partial implementation of a consensus program that fulfills general responsibilities.
 * Provides implementation for timer and consensus statistics responsibilities.
 */
public abstract class ConsensusProgramImpl<T extends BFTMessage> implements ConsensusProgram<T> {

    /**
     * Stores payloads while node is processing a message.
     * All payloads are retrieved and sent out after message processing.
     */
    private List<T> tempMessageStore;

    private final ConsensusStatistics statistics;
    private final TimerNotifier<ConsensusProgram<T>> timerNotifier;
    private final int numNodes;
    private int timerCount; // Used to differentiate multiple timers in the same instance & round
    private double timeoutTime;
    private double previousRecordedTime;

    /**
     * @param numNodes Number of nodes in the consensus program.
     * @param timerNotifier For tracking time and setting timers.
     */
    public ConsensusProgramImpl(int numNodes, TimerNotifier<ConsensusProgram<T>> timerNotifier) {
        this.numNodes = numNodes;
        this.tempMessageStore = new ArrayList<>();
        this.timerNotifier = timerNotifier;
        this.timeoutTime = 0;
        this.timerCount = 0;
        this.previousRecordedTime = 0;
        this.statistics = new ConsensusStatistics(getStates());
    }

    /**
     * Tracks consensus related statistics.
     */
    private void registerTimeElapsed(double time) {
        statistics.addTime(getState(), time);
        statistics.addRoundTime((getNumConsecutiveFailure() + 1), getState(), time);
        statistics.setConsensusCount(getConsensusCount());
    }

    public void registerMessageProcessed(T message, double currentTime) {
        double timeTaken = currentTime - previousRecordedTime;
        previousRecordedTime = currentTime;
        registerTimeElapsed(timeTaken);
        statistics.addMessageCountForState(message.getType());
    }

    public void registerMessagesSent(List<T> messages) {
        messages.forEach(m -> statistics.addMessageSent(m.getType()));
    }

    @Override
    public List<T> processAndRegisterMessage(T message, double currentTime) {
        registerMessageProcessed(message, currentTime);
        List<T> messages = processMessage(message);
        registerMessagesSent(messages);
        return messages;
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
    protected List<T> getMessages() {
        List<T> payloads = tempMessageStore;
        tempMessageStore = new ArrayList<>();
        return payloads;
    }

    /**
     * Sends {@code message} to destination {@code nodeName}.
     */
    protected void sendMessage(T message) {
        tempMessageStore.add(message);
    }

    /**
     * Sends {@code message} to all nodes in the collection of {@code nodeNames}.
     */
    protected void broadcastMessage(Function<Integer, T> messageGenerator) {
        IntStream.iterate(0, x -> x + 1).limit(numNodes)
                .forEach(id -> sendMessage(messageGenerator.apply(id)));
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
        // Note here the 0 does not matter for now as the program itself does not need to differentiate timers.
        timerNotifier.notifyAtTime(this, time, 0, id);
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
    public List<T> notifyTime(int timerCount) {
        if (timerCount == this.timerCount) {
            statistics.addRoundChangeStateCount(getState());
            List<T> messages = onTimerExpiry();
            registerMessagesSent(messages);
            return messages;
        }
        return List.of();
    }

    /**
     * Operation to be called on timer expiry.
     */
    protected abstract List<T> onTimerExpiry();
}
