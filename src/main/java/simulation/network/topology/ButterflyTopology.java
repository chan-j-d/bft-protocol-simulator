package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.Switch;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ButterflyTopology {
    /**
     * Constructs a butterfly topology with the given nodes.
     *
     * @param nodes Nodes to be arranged in a butterfly topology.
     * @param networkParameters Network parameters for {# of switch per level, inter-switch radix}
     *                          # of switch per level - number of switches per level in the folded clos network.
     *                          radix - positive integer >= 2, number of in/out connections per switch connecting with
     *                          other switches.
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return Returns a list of list of switches separated by level in the butterfly network.
     * @param <T> Message class being carried by switches.
     */
    public static <T> List<List<Switch<T>>> arrangeButterflyStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
        List<List<Switch<T>>> groupedSwitches =
                arrangeButterflyArrangement(nodes, networkParameters, messageChannelSuccessRate,
                        switchProcessingTimeGenerator, false);
        int radix = networkParameters.get(1);
        List<Switch<T>> flattenedLastLayerSwitches = new ArrayList<>(groupedSwitches.get(groupedSwitches.size() - 1));
        int numGroups = groupedSwitches.get(0).size();
        for (int i = 0; i < numGroups; i++) {
            if (i * radix >= nodes.size()) {
                break;
            }
            List<EndpointNode<T>> endpointSublist = TopologyUtil.getEndpointSublist(nodes, numGroups, i);
            Switch<T> lastHopSwitch = flattenedLastLayerSwitches.get(i);
            lastHopSwitch.setDirectlyConnectedEndpoints(endpointSublist);
        }
        TopologyUtil.flattenAndUpdateRoutes(groupedSwitches);
        return groupedSwitches;
    }

    private static String getTreeSwitchName(int level, int group, int index) {
        return String.format("(Level: %d, Group: %d, Index: %d)", level, group, index);
    }


    /**
     * Arranges a foldedClos network topology.
     * It is largely identical to a butterfly topology except that upon reaching the last level of switches,
     * the message travels back down to find its destination node. All connections among switches are two-way.
     * As such, the radix argument becomes a half-radix argument as the actual radix count is doubled.
     */
    public static <T> List<List<Switch<T>>> arrangeFoldedClosStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
        List<List<Switch<T>>> groupedSwitches =
                arrangeButterflyArrangement(nodes, networkParameters, messageChannelSuccessRate,
                        switchProcessingTimeGenerator, true);
        TopologyUtil.flattenAndUpdateRoutes(groupedSwitches);
        return groupedSwitches;
    }

    /**
     * Creates new switches to be arranged in a butterfly topology.
     * Used for both butterfly and foldedClos topologies.
     *
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param isBackwardConnecting true if switch connections are two directional.
     */
    private static <T> List<List<Switch<T>>> arrangeButterflyArrangement(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator,
            boolean isBackwardConnecting) {
        if (networkParameters.size() <= 1) {
            throw new RuntimeException(
                    "Please specify parameters for network " +
                            "{# of terminal nodes, # of switch per level, inter-switch radix}");
        }
        int numFirstLayerSwitches = networkParameters.get(0);
        int radix = networkParameters.get(1);

        List<Switch<T>> firstLayerSwitches = new ArrayList<>();
        for (int i = 0; i < numFirstLayerSwitches; i++) {
            int index = i;

            List<EndpointNode<T>> endpointSublist = TopologyUtil.getEndpointSublist(nodes, numFirstLayerSwitches, i);
            Switch<T> directSwitch_ = new Switch<>(
                    getTreeSwitchName(1, 0, i),
                    messageChannelSuccessRate,
                    new ArrayList<>(nodes),
                    isBackwardConnecting ? endpointSublist : List.of(),
                    switchProcessingTimeGenerator);
            firstLayerSwitches.add(directSwitch_);
            endpointSublist.forEach(node -> node.setOutflowNodes(List.of(firstLayerSwitches.get(index))));
        }

        List<List<Switch<T>>> groupedSwitches = new ArrayList<>(List.of(firstLayerSwitches));
        List<Switch<T>> allSwitches = new ArrayList<>(firstLayerSwitches);

        int currentLayer = 2;
        List<List<Switch<T>>> prevLayerGroupedSwitches = List.of(firstLayerSwitches);
        int nextGroupSize;
        do {
            List<List<Switch<T>>> newLayerGroupedSwitches = new ArrayList<>();
            for (int i = 0; i < prevLayerGroupedSwitches.size(); i++) {
                List<Switch<T>> prevGroupedSwitches = prevLayerGroupedSwitches.get(i);
                newLayerGroupedSwitches.addAll(addNextSwitchLayer(nodes, messageChannelSuccessRate,
                        prevGroupedSwitches, radix, currentLayer, i, switchProcessingTimeGenerator,
                        isBackwardConnecting));
            }
            currentLayer++;
            prevLayerGroupedSwitches = newLayerGroupedSwitches;
            nextGroupSize = newLayerGroupedSwitches.get(0).size();
            newLayerGroupedSwitches.forEach(allSwitches::addAll);

            List<Switch<T>> nextLayerSwitches = new ArrayList<>();
            newLayerGroupedSwitches.forEach(nextLayerSwitches::addAll);
            groupedSwitches.add(nextLayerSwitches);
        } while (nextGroupSize > 1);
        return groupedSwitches;
    }

    /**
     * Returns the next layer of connection that maximizes number of groups instead of group size.
     */
    private static <T> List<List<Switch<T>>> addNextSwitchLayer(List<? extends EndpointNode<T>> endpoints,
            double messageChannelSuccessRate,
            List<Switch<T>> prevLayer, int radix, int level, int group,
            RandomNumberGenerator switchProcessingTimeGenerator, boolean isBackwardConnecting) {
        int numNodes = prevLayer.size();
        int numGroups = Math.max(numNodes / radix, 1);
        radix = Math.min(numNodes, radix);

        List<List<Switch<T>>> nextLayerSwitches = new ArrayList<>();
        Stream.generate(() -> new ArrayList<Switch<T>>()).limit(radix).forEach(nextLayerSwitches::add);
        for (int groupNumber = 0; groupNumber < numGroups; groupNumber++) {
            int finalGroupNumber = groupNumber;
            int effectiveRadix = radix;
            List<Switch<T>> newNeighborGroup =
                    Stream.iterate(0, index -> index < effectiveRadix, index -> index + 1)
                            .map(index -> new Switch<>(getTreeSwitchName(level,
                                    effectiveRadix * group + index, finalGroupNumber),
                                    messageChannelSuccessRate,
                                    new ArrayList<>(endpoints),
                                    List.of(),
                                    switchProcessingTimeGenerator))
                            .collect(Collectors.toList());
            List<Switch<T>> prevLayerGroup = Stream.iterate(0, index -> index < effectiveRadix, index -> index + 1)
                    .map(index -> prevLayer.get(index * numGroups + finalGroupNumber))
                    .collect(Collectors.toList());
            prevLayerGroup.forEach(switch_ -> switch_.updateSwitchNeighbors(newNeighborGroup));
            if (isBackwardConnecting) {
                newNeighborGroup.forEach(switch_ -> switch_.setSwitchNeighbors(prevLayerGroup));
            }
            for (int index = 0; index < radix; index++) {
                nextLayerSwitches.get(index).add(newNeighborGroup.get(index));
            }
        }
        return nextLayerSwitches;
    }
}
