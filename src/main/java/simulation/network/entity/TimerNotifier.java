package simulation.network.entity;

public interface TimerNotifier<T> {

    void notifyAtTime(TimedNode<T> node, double time, int id);
    double getTime();
}
