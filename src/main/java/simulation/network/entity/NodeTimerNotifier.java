package simulation.network.entity;

public interface NodeTimerNotifier<T> {

    void notifyAtTime(TimedNode<T> node, double time, T message);
    double getTime();
}
