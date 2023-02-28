package simulation.network.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class EndpointNode<T> extends Node<T> {

    private List<Node<T>> outflowNodes;

    public EndpointNode(String name) {
        super(name);
        this.outflowNodes = new ArrayList<>();
    }

    public void setOutflowNodes(List<Node<T>> outflowNodes) {
        this.outflowNodes = new ArrayList<>(outflowNodes);
    }

    @Override
    public Node<T> getNextNodeFor(Payload<T> payload) {
        if (outflowNodes.size() == 0) {
            throw new RuntimeException(String.format("Outflow neighbors not initialized for %s", this));
        }
        // random tiebreaker
        int randomIndex = new Random().nextInt(outflowNodes.size());
        return outflowNodes.get(randomIndex);
    }
}
