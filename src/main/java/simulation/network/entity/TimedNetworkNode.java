package simulation.network.entity;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.NodeTimerNotifier;
import simulation.network.entity.Payload;

import java.util.List;

public abstract class TimedNetworkNode<T> extends NetworkNode<T> {

    private final NodeTimerNotifier<T> timerNotifier;

    public TimedNetworkNode(String name, NodeTimerNotifier<T> timerNotifier) {
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
