package simulation.network.entity;

import simulation.util.rng.RandomNumberGenerator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Validator<T> extends TimedNode<T> {

    private final int id;
    private final Map<Integer, Validator<T>> allNodes;


    public Validator(String name, int id, NodeTimerNotifier<T> timerNotifier,
            RandomNumberGenerator serviceTimeGenerator) {
        super(name, timerNotifier, serviceTimeGenerator);
        this.allNodes = new HashMap<>();
        this.id = id;
    }

    public void setAllNodes(List<? extends Validator<T>> allNodes) {
        for (Validator<T> node : allNodes) {
            this.allNodes.put(node.getId(), node);
        }
    }

    public Collection<Validator<T>> getAllNodes() {
        return allNodes.values();
    }

    public Validator<T> getNode(int id) {
        return allNodes.get(id);
    }

    protected void broadcastMessageToAll(T message) {
        broadcastMessage(message, getAllNodes());
    }

    public int getId() {
        return id;
    }
}
