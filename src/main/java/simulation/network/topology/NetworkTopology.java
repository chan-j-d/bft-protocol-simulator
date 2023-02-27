package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.entity.Node;
import simulation.network.router.Switch;

import java.util.ArrayList;
import java.util.List;

public class NetworkTopology {
    public static <T> List<Node<T>> arrangeCliqueStructure(List<? extends EndpointNode<T>> nodes) {
        List<Switch<T>> switches = new ArrayList<>();
        for (EndpointNode<T> node : nodes) {
            Switch<T> switch_ = new Switch<>("Switch-" + node.getName(), new ArrayList<>(nodes));
            switches.add(switch_);
            node.setOutflowNodes(List.of(switch_));
        }

        for (Switch<T> switch_ : switches) {
            switch_.setNeighbors(new ArrayList<>(switches));
        }
        return new ArrayList<>(nodes);
    }
}
