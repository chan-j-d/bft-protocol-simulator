package simulation.network.entity;

import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;

public abstract class TimedNode<T> extends EndpointNode<T> {

    private final NodeTimerNotifier<T> timerNotifier;
    private final RandomNumberGenerator rng;
    private double previousRecordedTime;

    public TimedNode(String name, NodeTimerNotifier<T> timerNotifier, RandomNumberGenerator serviceTimeGenerator) {
        super(name);
        this.timerNotifier = timerNotifier;
        this.rng = serviceTimeGenerator;
        this.previousRecordedTime = 0;
    }

    public double getTime() {
        return timerNotifier.getTime();
    }

    public void notifyAtTime(double time, T message) {
        timerNotifier.notifyAtTime(this, time, message);
    }

    /**
     * Returns a list of payloads after notifying the node at the requested time.
     *
     * @param time to be notified, determined by a previous {@code getNextNotificationTime} call.
     * @param message to be attached with the notification for analysis.
     * @return List of payloads to be sent at the given time.
     */
    public abstract List<Payload<T>> notifyTime(double time, T message);

    protected abstract List<Payload<T>> processMessage(T message);

    protected abstract void registerTimeElapsed(double time);

    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        double duration = rng.generateRandomNumber();
        double timePassed = time - previousRecordedTime;
        double newCurrentTime = time + duration;
        super.processPayload(newCurrentTime, payload);
        previousRecordedTime = newCurrentTime;
        T message = payload.getMessage();
        registerTimeElapsed(duration + timePassed);
//        logger.log(String.format("%.3f-%.3f: %s processing %s\n%s\n%s", time, newCurrentTime,
//                this, message, super.getQueueStatistics(), getIbftStatistics()));
        return new Pair<>(duration, processMessage(message));
    }
}
