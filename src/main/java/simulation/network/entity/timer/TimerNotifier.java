package simulation.network.entity.timer;

/**
 * Interface for a timer that a {@code TimedNode} uses to set timeouts and check time.
 */
public interface TimerNotifier<T> {

    /**
     * Notify {@code node} at time {@code time} with unique identification {@code timerCount}.
     *
     * @param receiver Receiver to be notified.
     * @param time Time to be notified.
     * @param id Integer identification to be notified.
     * @param timerCount An integer unique identifier for this specific notification.
     */
    void notifyAtTime(T receiver, double time, int id, int timerCount);

    /**
     * Returns the current {@code time}.
     * The implementation of this method dictates the flow of time that the caller will experience.
     */
    double getTime();
}
