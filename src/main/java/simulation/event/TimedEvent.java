package simulation.event;

import simulation.network.entity.TimedNode;

import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

public class TimedEvent<T> extends NodeEvent<T> {

    private final int id;
    private final TimedNode<T> node;

    public TimedEvent(double time, TimedNode<T> node, int id) {
        super(time, node);
        this.node = node;
        this.id = id;
    }

    @Override
    public List<NodeEvent<T>> simulate() {
        return convertPayloadsToQueueEvents(getTime(), node, node.notifyTime(id));
    }

    @Override
    public String toString() {
        return super.toString() + " (Timed): Notifying " + node + " at " + getTime();
    }
}
