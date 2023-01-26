package simulation.event;

import simulation.network.entity.NetworkNode;

import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

public class TimedEvent extends Event {

    private NetworkNode node;

    public TimedEvent(double time, NetworkNode node) {
        super(time);
        this.node = node;
    }

    @Override
    public List<Event> simulate() {
        return convertPayloadsToQueueEvents(getTime(), node, node.notifyTime(getTime()));
    }

    @Override
    public String toString() {
        return super.toString() + " (Timed): Notifying " + node + " at " + getTime();
    }
}
