package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TopologyUtil {

    /**
     * @param nodes Nodes to retrieve endpoint sublist from.
     * @param numSwitches Number of switches for direction connection.
     * @param index The index-th sublist to be retrieved. If nodes.size() < connections * index, returns empty list.
     * @return Returns the endpoint sublist corresponding to the parameters specified.
     */
    public static <T> List<EndpointNode<T>> getEndpointSublist(List<? extends EndpointNode<T>> nodes,
            int numSwitches, int index) {
        int minEndpointPerSwitch = nodes.size() / numSwitches;
        int numAdditional = nodes.size() % numSwitches;
        int startIndex = index < numAdditional ? (minEndpointPerSwitch + 1) * index :
                ((minEndpointPerSwitch + 1) * numAdditional) + minEndpointPerSwitch * (index - numAdditional);
        int endIndex = startIndex + minEndpointPerSwitch + (numAdditional > index ? 1 : 0);
        return new ArrayList<>(nodes.subList(startIndex, endIndex));
    }

    /**
     * Updates the routing tables of the grouped switches.
     */
    public static <T> void flattenAndUpdateRoutes(List<List<Switch<T>>> groupedSwitches) {
        List<Switch<T>> allSwitches = groupedSwitches.stream().flatMap(Collection::stream).collect(Collectors.toList());
        RoutingUtil.updateRoutingTables(allSwitches);
    }
}
