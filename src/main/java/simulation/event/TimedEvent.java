package simulation.event;

import simulation.network.entity.TimedNode;

import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

public class TimedEvent<T> extends Event {

    private final TimedNode<T> node;
    private final int timerCount;

    public TimedEvent(double time, TimedNode<T> node, int timerCount) {
        super(time);
        this.node = node;
        this.timerCount = timerCount;
    }

    @Override
    public List<Event> simulate() {
        return convertPayloadsToQueueEvents(getTime(), node, node.notifyTime(timerCount));
    }

    @Override
    public String toString() {
        return super.toString() + " (Timed): Notifying " + node + " at " + getTime();
    }

    @Override
    public boolean toDisplay() {
        return !node.isDone();
    }
}
