package simulation.network.entity;

import java.util.List;

public abstract class TimedNode<T> extends EndpointNode<T> {

    private final TimerNotifier<T> timerNotifier;

    public TimedNode(String name, TimerNotifier<T> timerNotifier) {
        super(name);
        this.timerNotifier = timerNotifier;
    }

    public double getTime() {
        return timerNotifier.getTime();
    }

    public void notifyAtTime(double time, int id) {
        timerNotifier.notifyAtTime(this, time, id);
    }

    /**
     * Returns a list of payloads after notifying the node at the requested time.
     *
     * @param count uniquely identifies the notification.
     * @return List of payloads to be sent at the given time.
     */
    public abstract List<Payload<T>> notifyTime(int count);

}
