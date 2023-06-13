package simulation.network.entity;

import simulation.network.entity.timer.TimerNotifier;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;

public abstract class TimedNode<T> extends EndpointNode<T> {

    private final TimerNotifier<T> timerNotifier;
    private final RandomNumberGenerator rng;
    private double previousRecordedTime;
    private int timerCount; // Used to differentiate multiple timers in the same instance & round
    private double timeoutTime;

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

    public void notifyAtTime(double time, int id) {
        timerNotifier.notifyAtTime(this, time, id);
    }

    protected void startTimer(double duration) {
        timeoutTime = getTime() + duration;
        notifyAtTime(timeoutTime, ++timerCount); // Every time a timer starts, a unique one is set.
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

    protected abstract List<Payload<T>> processMessage(T message);

    protected abstract void registerTimeElapsed(double time);
    protected abstract List<Payload<T>> onTimerExpiry();

    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        double duration = rng.generateRandomNumber();
        double timePassed = time - previousRecordedTime;
        double newCurrentTime = time + duration;
        boolean isTimedOut = false;
        List<Payload<T>> payloads;
        if (newCurrentTime > timeoutTime) {
            newCurrentTime = timeoutTime;
            duration = timeoutTime - time;
            isTimedOut = true;
        }
        super.processPayload(newCurrentTime, payload);

        previousRecordedTime = newCurrentTime;
        registerTimeElapsed(duration + timePassed);
//        logger.log(String.format("%.3f-%.3f: %s processing %s\n%s\n%s", time, newCurrentTime,
//                this, message, super.getQueueStatistics(), getIbftStatistics()));
        if (!isTimedOut) {
            T message = payload.getMessage();
            payloads = processMessage(message);
        } else {
            payloads = onTimerExpiry();
        }
        return new Pair<>(duration, payloads);
    }
}
