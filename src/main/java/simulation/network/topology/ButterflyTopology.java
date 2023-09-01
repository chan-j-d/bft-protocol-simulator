package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.Switch;
import simulation.util.MathUtil;
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
     * @param networkParameters Network parameters for {radix, connection type, front/back concentrated}.
     *                          radix - positive integer >= 2, number of in/out connections per switch.
     *                          connection type - 0 or 1. 0 for a flushed type connection and 1 for spread.
     *                          front/back concentrated - 0 or 1. 0 for concentrating connections to the front,
     *                          0 for back.
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
        int radix = networkParameters.get(0);
        List<Switch<T>> flattenedLastLayerSwitches = new ArrayList<>(groupedSwitches.get(groupedSwitches.size() - 1));
        int numGroups = groupedSwitches.get(0).size();
        for (int i = 0; i < numGroups; i++) {
            if (i * radix >= nodes.size()) {
                break;
            }
            List<? extends EndpointNode<T>> endpointSublist =
                    nodes.subList(i * radix, Math.min((i + 1) * radix, nodes.size()));
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
        if (networkParameters.size() <= 2) {
            throw new RuntimeException(
                    "Please specify parameters for network \"[{radix}, {initialConnection}, {version}]\"");
        }
        int radix = networkParameters.get(0);
        int minimumNodeCount = MathUtil.ceilDiv(nodes.size(), radix);
        int levels = (int) MathUtil.log(minimumNodeCount, radix);
        int baseGroupSize = (int) Math.pow(radix, levels);
        int numFirstLayerGroups = MathUtil.ceilDiv(minimumNodeCount, baseGroupSize) * baseGroupSize;

        List<Switch<T>> firstLayerSwitches = new ArrayList<>();
        for (int i = 0; i < numFirstLayerGroups; i++) {
            int index = i;

            List<EndpointNode<T>> endpointSublist = TopologyUtil.getEndpointSublist(nodes, networkParameters.get(1),
                    numFirstLayerGroups, radix, i);
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
                if (networkParameters.get(2) == 0) {
                    newLayerGroupedSwitches.addAll(addNextSwitchLayer(nodes, messageChannelSuccessRate,
                            prevGroupedSwitches, radix, currentLayer, i, switchProcessingTimeGenerator,
                            isBackwardConnecting));
                } else if (networkParameters.get(2) == 1) {
                    newLayerGroupedSwitches.addAll(addNextSwitchLayerTwo(nodes, messageChannelSuccessRate,
                            prevGroupedSwitches, radix, currentLayer, i, switchProcessingTimeGenerator,
                            isBackwardConnecting));
                } else {
                    throw new RuntimeException("Next layer parameter should be 0 or 1.");
                }
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
     * Returns the next layer of connection that maximizes group size instead of number of groups.
     */
    private static <T> List<List<Switch<T>>> addNextSwitchLayerTwo(List<? extends EndpointNode<T>> endpoints,
            double messageChannelSuccessRate,
            List<Switch<T>> prevLayer, int radix, int level, int group,
            RandomNumberGenerator switchProcessingTimeGenerator, boolean isBackwardConnecting) {
        if (level > 3) {
            return List.of();
        }
        int numNodes = prevLayer.size();
        int groupSize = (int) Math.pow(radix, Math.ceil(MathUtil.log(numNodes, radix) - 1));
        int numGroups = Math.max(numNodes / groupSize, 1);
        radix = numGroups;

        List<List<Switch<T>>> nextLayerSwitches = new ArrayList<>();
        Stream.generate(() -> new ArrayList<Switch<T>>()).limit(numGroups).forEach(nextLayerSwitches::add);
        for (int groupNumber = 0; groupNumber < numGroups; groupNumber++) {
            int effectiveRadix = radix;
            for (int index = 0; index < groupSize; index++) {
                int finalIndex = index;
                Switch<T> switch_ = new Switch<>(getTreeSwitchName(level, group * groupSize + groupNumber, index),
                        messageChannelSuccessRate,
                        new ArrayList<>(endpoints),
                        List.of(),
                        switchProcessingTimeGenerator);
                List<Switch<T>> prevLayerNeighbors = Stream.iterate(0, prevIndex -> prevIndex < effectiveRadix, prevIndex -> prevIndex + 1)
                        .map(prevIndex -> prevLayer.get(finalIndex + groupSize * prevIndex))
                        .collect(Collectors.toList());
                prevLayerNeighbors.forEach(switch_2 -> switch_2.updateSwitchNeighbors(List.of(switch_)));
                if (isBackwardConnecting) {
                    switch_.setSwitchNeighbors(prevLayerNeighbors);
                }
                nextLayerSwitches.get(groupNumber).add(switch_);
            }
        }
        return nextLayerSwitches;
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
