package simulation.event;

import simulation.network.entity.Node;

import java.util.List;

public abstract class NodeEvent<T> implements Comparable<NodeEvent<T>> {

    private double time;
    private Node<T> node;

    public NodeEvent(double time, Node<T> node) {
        this.time = time;
        this.node = node;
    }

    public double getTime() {
        return time;
    }

    public Node<T> getNode() {
        return node;
    }

    @Override
    public int compareTo(NodeEvent<T> e) {
        if (this.time > e.time) {
            return 1;
        } else if (this.time == e.time) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("%.3f", time);
    }

    public abstract List<NodeEvent<T>> simulate();
}
