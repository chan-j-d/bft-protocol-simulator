package simulation.network.topology;

import simulation.network.entity.EndpointNode;

import java.util.ArrayList;
import java.util.List;

public class TopologyUtil {

    /**
     * @param nodes Nodes to retrieve endpoint sublist from.
     * @param param Parameter for determining flushed or spread setting. 0 for flushed, 1 for spread.
     * @param numSwitches Number of switches for direction connection.
     * @param connections Maximum number of connections per switch.
     * @param index The index-th sublist to be retrieved. If nodes.size() < connections * index, returns empty list.
     * @return Returns the endpoint sublist corresponding to the parameters specified.
     */
    public static <T> List<EndpointNode<T>> getEndpointSublist(List<? extends EndpointNode<T>> nodes, int param,
            int numSwitches, int connections, int index) {
        if (connections * index > nodes.size()) {
            return List.of();
        }
        int minEndpointPerSwitch = nodes.size() / numSwitches;
        int numAdditional = nodes.size() % numSwitches;
        if (param == 1) {
            // 1 for spread, 0 for flushed
            int startIndex = index < numAdditional ? (minEndpointPerSwitch + 1) * index :
                    ((minEndpointPerSwitch + 1) * numAdditional) + minEndpointPerSwitch * (index - numAdditional);
            int endIndex = startIndex + minEndpointPerSwitch + (numAdditional > index ? 1 : 0);
            return new ArrayList<>(nodes.subList(startIndex, endIndex));
        } else if (param == 0) {
            return (index * connections) >= nodes.size() ? List.of() :
                    new ArrayList<>(nodes.subList(index * connections, Math.min(nodes.size(),
                            (index + 1) * connections)));
        } else {
            throw new RuntimeException("Initial connection parameter (second field) must be 1 or 0.");
        }
    }
}
