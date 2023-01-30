package simulation.event;

import simulation.network.entity.NetworkNode;

import java.util.List;

public class PollQueueEvent extends Event {

    private NetworkNode node;

    public PollQueueEvent(double time, NetworkNode node) {
        super(time);
        this.node = node;
    }

    @Override
    public List<Event> simulate() {
        if (!node.isEmpty() && !node.isOccupiedAtTime(getTime())) {
            return List.of(new ProcessHeaderEvent(getTime(), node, node.popFromQueue()));
        } else {
            return List.of();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " (PollQueueEvent): Polling from " + node;
    }
}
