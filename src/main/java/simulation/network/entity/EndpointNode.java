package simulation.network.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public abstract class EndpointNode<T> extends Node<T> {

    private List<Node<T>> outflowNodes;
    /**
     * Stores payloads while node is processing a message.
     * All payloads are retrieved and sent out after message processing.
     */
    private List<Payload<T>> tempPayloadStore;

    public EndpointNode(String name) {
        super(name);
        this.outflowNodes = new ArrayList<>();
        this.tempPayloadStore = new ArrayList<>();
    }

    public void setOutflowNodes(List<Node<T>> outflowNodes) {
        this.outflowNodes = new ArrayList<>(outflowNodes);
    }

    public List<Node<T>> getOutflowNodes() {
        return outflowNodes;
    }

    /**
     * Retries and returns payloads generated from a processing step.
     * Empties the payload list.
     *
     * @return List of payloads that were generated from a processing step.
     */
    protected List<Payload<T>> getProcessedPayloads() {
        List<Payload<T>> payloads = tempPayloadStore;
        tempPayloadStore = new ArrayList<>();
        return payloads;
    }

    protected void sendMessage(T message, EndpointNode<T> node) {
        tempPayloadStore.add(createPayloads(message, node));
    }
    protected void broadcastMessage(T message, Collection<? extends EndpointNode<? extends T>> nodes) {
        tempPayloadStore.addAll(createPayloads(message, nodes));
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
