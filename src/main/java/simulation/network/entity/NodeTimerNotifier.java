package simulation.network.entity;

public interface NodeTimerNotifier<T> {

    void notifyAtTime(TimedNetworkNode<T> node, double time, T message);
    double getTime();
}
