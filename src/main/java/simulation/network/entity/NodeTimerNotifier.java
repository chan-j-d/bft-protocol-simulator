package simulation.network.entity;

public interface NodeTimerNotifier {

    void notifyAtTime(TimedNetworkNode node, double time, String message);
    double getTime();
}
