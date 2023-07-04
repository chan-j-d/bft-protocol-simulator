package simulation.network.entity;

import simulation.network.entity.timer.TimerNotifier;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;

/**
 * Encapsulates a network node with a notion of time.
 *
 * @param <T> Message class generated by {@code TimedNode}.
 */
public abstract class TimedNode<T> extends EndpointNode<T> {

    private final TimerNotifier<T> timerNotifier;
    private final RandomNumberGenerator rng;
    private double previousRecordedTime;
    private int timerCount; // Used to differentiate multiple timers in the same instance & round
    private double timeoutTime;

    /**
     * @param name Name of node.
     * @param timerNotifier TimerNotifier to check time and set timers.
     * @param serviceTimeGenerator Service rate of messages assuming an exponential distribution for processing time.
     */
    public TimedNode(String name, TimerNotifier<T> timerNotifier, RandomNumberGenerator serviceTimeGenerator) {
        super(name);
        this.timerNotifier = timerNotifier;
        this.rng = serviceTimeGenerator;
        this.previousRecordedTime = 0;
        this.timerCount = 0;
    }

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
            return onTimerExpiry();
        }
        return List.of();
    }

    /**
     * Returns a list of payloads after processing a given {@code message}.
     */
    protected abstract List<Payload<T>> processMessage(T message);

    /**
     * Record any changes as a result of {@code time} elapsed.
     */
    protected abstract void registerTimeElapsed(double time);

    /**
     * Operation to be called on timer expiry.
     */
    protected abstract List<Payload<T>> onTimerExpiry();

    /**
     * Processes payload and returns duration and list of resulting payloads.
     * Duration is generated randomly by an exponential random variable.
     * New time is set to be {@code time} + duration generated in order to set the node as occupied up to end time.
     * A consequence of this is that timeouts are only registered after the message that crosses
     * the timeout is processed.
     *
     * @param time Time payload is being processed.
     * @param payload Payload to be processed.
     * @return Returns time taken to process the payload and list of resulting payloads from processing.
     */
    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        double duration = rng.generateRandomNumber();
        double timePassed = time - previousRecordedTime;
        previousRecordedTime = time + duration;
        registerTimeElapsed(duration + timePassed);
        T message = payload.getMessage();
        List<Payload<T>> payloads = processMessage(message);
        return new Pair<>(duration, payloads);
    }
}
