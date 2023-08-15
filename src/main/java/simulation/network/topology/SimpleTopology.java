package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods that construct various common network topologies.
 */
public class SimpleTopology {

    /**
     * Constructs a clique topology with the given {@code nodes}.
     * For ease of coding, for each node there is a proxy switch used that is connected to every other proxy switch.
     * The node is then connected to only its proxy switch.
     * To remove the effect of switch processing time for the switch, set the switch processing rate to -1.
     *
     * @param nodes Nodes to be connected in a clique topology.
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return Returns a list of a single list of proxy switches for each node.
     * @param <T> Message class being carried by switches.
     */
    public static <T> List<List<Switch<T>>> arrangeCliqueStructure(List<? extends EndpointNode<T>> nodes,
            double messageChannelSuccessRate, RandomNumberGenerator switchProcessingTimeGenerator) {
        List<Switch<T>> switches = new ArrayList<>();
        for (EndpointNode<T> node : nodes) {
            Switch<T> switch_ = new Switch<>("Switch-" + node.getName(), messageChannelSuccessRate,
                    new ArrayList<>(nodes), List.of(node), switchProcessingTimeGenerator);
            switches.add(switch_);
            node.setOutflowNodes(List.of(switch_));
        }

        for (Switch<T> switch_ : switches) {
            switch_.setSwitchNeighbors(new ArrayList<>(switches));
        }

        RoutingUtil.updateRoutingTables(switches);
        return List.of(switches);
    }
}
