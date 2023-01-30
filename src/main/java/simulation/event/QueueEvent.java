package simulation.event;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;

import java.util.List;

public class QueueEvent extends Event {

    private NetworkNode node;
    private Payload payload;
    public QueueEvent(double time, NetworkNode node, Payload payload) {
        super(time);
        this.node = node;
        this.payload = payload;
    }

    @Override
    public List<Event> simulate() {
        node.addToQueue(payload);
        return List.of(new PollQueueEvent(getTime(), node));
    }

    @Override
    public String toString() {
        return super.toString() + " (QueueEvent): payload queued at " + node;
    }
}
