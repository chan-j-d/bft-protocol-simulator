package simulation.network.entity.timer;

import simulation.network.entity.TimedNode;

public interface TimerNotifier<T> {

    void notifyAtTime(TimedNode<T> node, double time, int timerCount);
    double getTime();
}
