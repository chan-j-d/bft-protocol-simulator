package simulation.event;

import simulation.network.entity.TimedNetworkNode;

import java.util.List;

import static simulation.event.EventUtil.convertPayloadsToQueueEvents;

public class TimedEvent extends Event {

    private TimedNetworkNode node;
    private String message;

    public TimedEvent(double time, TimedNetworkNode node, String message) {
        super(time);
        this.node = node;
        this.message = message;
    }

    @Override
    public List<Event> simulate() {
        return convertPayloadsToQueueEvents(getTime(), node, node.notifyTime(getTime(), message));
    }

    @Override
    public String toString() {
        return super.toString() + " (Timed): Notifying " + node + " at " + getTime();
    }
}
