package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;
import simulation.util.MathUtil;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkTopology {
    public static <T> List<List<Switch<T>>> arrangeCliqueStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            Supplier<RandomNumberGenerator> rngSupplier) {
        List<Switch<T>> switches = new ArrayList<>();
        for (EndpointNode<T> node : nodes) {
            Switch<T> switch_ = new Switch<>("Switch-" + node.getName(), new ArrayList<>(nodes),
                    List.of(node), rngSupplier.get());
            switches.add(switch_);
            node.setOutflowNodes(List.of(switch_));
        }

        for (Switch<T> switch_ : switches) {
            switch_.setSwitchNeighbors(new ArrayList<>(switches));
        }

        RoutingUtil.updateRoutingTables(switches);
        return List.of(switches);
    }

    public static <T> List<List<Switch<T>>> arrangeMeshStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            Supplier<RandomNumberGenerator> rngSupplier) {
        if (networkParameters.size() == 0) {
            throw new RuntimeException("Please specify side length for network parameters.");
        }
        int n = networkParameters.get(0);
        if (nodes.size() % n != 0) {
            throw new RuntimeException(String.format(
                    "Specified side length %d does not divide number of nodes %d", n, nodes.size()));
        }

        int m = nodes.size() / n;
        List<List<Switch<T>>> switchArray =
                createSwitchArray(nodes, n, m, "Mesh-Switch-(x: %d, y: %d)", rngSupplier);
        List<Switch<T>> switches = switchArray.stream().flatMap(List::stream).collect(Collectors.toList());

        // sets neighbors for each switch
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                List<Switch<T>> neighboringSwitches = Stream.of(new Pair<>(i - 1, j), new Pair<>(i + 1, j),
                        new Pair<>(i, j - 1), new Pair<>(i, j + 1))
                        .filter(pair -> pair.first() >= 0 && pair.first() < n
                                && pair.second() >= 0 && pair.second() < m)
                        .map(coordinates -> switchArray.get(coordinates.first()).get(coordinates.second()))
                        .collect(Collectors.toList());
                Switch<T> switch_ = switchArray.get(i).get(j);
                switch_.setSwitchNeighbors(neighboringSwitches);
            }
        }

        RoutingUtil.updateRoutingTables(switches);
        return List.of(switches);
    }

    private static <T> List<List<Switch<T>>> createSwitchArray(List<? extends EndpointNode<T>> nodes,
            int n, int m, String nameFormat, Supplier<RandomNumberGenerator> rngSupplier) {
        List<List<Switch<T>>> switchArray = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            switchArray.add(new ArrayList<>());
            for (int j = 0; j < m; j++) {
                String switchName = String.format(nameFormat, i, j);
                EndpointNode<T> endNode = nodes.get(i * m + j);
                Switch<T> newSwitch =
                        new Switch<>(switchName, new ArrayList<>(nodes), List.of(endNode), rngSupplier.get());
                switchArray.get(i).add(newSwitch);
                endNode.setOutflowNodes(List.of(newSwitch));
            }
        }
        return switchArray;
    }

    public static <T> List<List<Switch<T>>> arrangeTorusStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            Supplier<RandomNumberGenerator> rngSupplier) {
        if (networkParameters.size() == 0) {
            throw new RuntimeException("Please specify side length for network parameters.");
        }
        int n = networkParameters.get(0);
        if (nodes.size() % n != 0) {
            throw new RuntimeException(String.format(
                    "Specified side length %d does not divide number of nodes %d", n, nodes.size()));
        }

        int m = nodes.size() / n;
        List<List<Switch<T>>> switchArray = createSwitchArray(nodes, n, m, "Torus-Switch-(x: %d, y: %d)",
                rngSupplier);
        List<Switch<T>> switches = switchArray.stream().flatMap(List::stream).collect(Collectors.toList());
        // sets neighbors for each switch
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                List<Switch<T>> neighboringSwitches = Stream.of(new Pair<>(i - 1, j), new Pair<>(i + 1, j),
                                new Pair<>(i, j - 1), new Pair<>(i, j + 1))
                        .map(pair -> new Pair<>(
                                pair.first() < 0 ? n - 1 : pair.first() == n ? 0 : pair.first(),
                                pair.second() < 0 ? m - 1 : pair.second() == m ? 0 : pair.second()))
                        .map(coordinates -> switchArray.get(coordinates.first()).get(coordinates.second()))
                        .collect(Collectors.toList());
                Switch<T> switch_ = switchArray.get(i).get(j);
                switch_.setSwitchNeighbors(neighboringSwitches);
            }
        }

        RoutingUtil.updateRoutingTables(switches);
        return List.of(switches);
    }

    public static <T> List<List<Switch<T>>> arrangeButterflyStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            Function<Integer, RandomNumberGenerator> levelRngFunction) {
        List<List<Switch<T>>> groupedSwitches =
                arrangeButterflyArrangement(nodes, networkParameters, levelRngFunction, false);
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
        flattenAndUpdateRoutes(groupedSwitches);
        return groupedSwitches;
    }


    private static String getTreeSwitchName(int level, int group, int index) {
        return String.format("(Level: %d, Group: %d, Index: %d)", level, group, index);
    }

    private static <T> void flattenAndUpdateRoutes(List<List<Switch<T>>> groupedSwitches) {
        List<Switch<T>> allSwitches = groupedSwitches.stream().flatMap(Collection::stream).collect(Collectors.toList());
        RoutingUtil.updateRoutingTables(allSwitches);
    }

    public static <T> List<List<Switch<T>>> arrangeFoldedClosStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            Function<Integer, RandomNumberGenerator> levelRngFunction) {
        List<List<Switch<T>>> groupedSwitches =
                arrangeButterflyArrangement(nodes, networkParameters, levelRngFunction, true);
        flattenAndUpdateRoutes(groupedSwitches);
        return groupedSwitches;
    }
    private static <T> List<List<Switch<T>>> arrangeButterflyArrangement(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            Function<Integer, RandomNumberGenerator> levelRngFunction,
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
        int minEndpointPerSwitch = nodes.size() / numFirstLayerGroups;
        int numAdditional = nodes.size() % numFirstLayerGroups;
        int startIndex = 0;
        for (int i = 0; i < numFirstLayerGroups; i++) {
            int index = i;
            List<? extends EndpointNode<T>> endpointSublist;
            if (networkParameters.get(1) == 1) {
                int endIndex = startIndex + minEndpointPerSwitch + (i < numAdditional ? 1 : 0);
                endpointSublist = nodes.subList(startIndex, endIndex);
                startIndex = endIndex;
            } else if (networkParameters.get(1) == 0) {
                endpointSublist = (i * radix) >= nodes.size() ? List.of() :
                        nodes.subList(i * radix, Math.min(nodes.size(), (i + 1) * radix));
            } else {
                throw new RuntimeException("Initial connection parameter (second field) must be 1 or 0.");
            }
            Switch<T> directSwitch_ = new Switch<>(
                    getTreeSwitchName(1, 0, i),
                    new ArrayList<>(nodes),
                    isBackwardConnecting ? endpointSublist : List.of(),
                    levelRngFunction.apply(1));
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
                    newLayerGroupedSwitches.addAll(addNextSwitchLayer(nodes, prevGroupedSwitches,
                            radix, currentLayer, i, levelRngFunction, isBackwardConnecting));
                } else if (networkParameters.get(2) == 1) {
                    newLayerGroupedSwitches.addAll(addNextSwitchLayerTwo(nodes, prevGroupedSwitches,
                            radix, currentLayer, i, levelRngFunction, isBackwardConnecting));
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
            List<Switch<T>> prevLayer, int radix, int level, int group, Function<Integer,
            RandomNumberGenerator> levelRngFunction, boolean isBackwardConnecting) {
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
                        new ArrayList<>(endpoints),
                        List.of(),
                        levelRngFunction.apply(level));
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
            List<Switch<T>> prevLayer, int radix, int level, int group, Function<Integer,
            RandomNumberGenerator> levelRngFunction, boolean isBackwardConnecting) {
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
                                    new ArrayList<>(endpoints),
                                    List.of(),
                                    levelRngFunction.apply(level)))
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
