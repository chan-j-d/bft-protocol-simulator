package simulation.network.entity;

import java.util.List;

public abstract class TimedNode<T> extends EndpointNode<T> {

    private final NodeTimerNotifier<T> timerNotifier;

    public TimedNode(String name, NodeTimerNotifier<T> timerNotifier) {
        super(name);
        this.timerNotifier = timerNotifier;
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

}
