package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkTopology {
    public static <T> List<Switch<T>> arrangeCliqueStructure(
            List<? extends EndpointNode<T>> nodes, Supplier<RandomNumberGenerator> rngSupplier) {
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
        return switches;
    }

    public static <T> List<Switch<T>> arrangeMeshStructure(List<? extends EndpointNode<T>> nodes, int n,
            Supplier<RandomNumberGenerator> rngSupplier) {
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
        return switches;
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

    public static <T> List<Switch<T>> arrangeTorusStructure(List<? extends EndpointNode<T>> nodes, int n,
            Supplier<RandomNumberGenerator> rngSupplier) {
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
        return switches;
    }

    public static <T> List<Switch<T>> arrangeFoldedClosStructure(List<? extends EndpointNode<T>> nodes, int radix,
            Function<Integer, RandomNumberGenerator> levelRngFunction) {
        if (!isValidFoldedClosSetup(nodes.size(), radix)) {
            throw new RuntimeException(String.format(
                    "The dimensions (Size: %d, Radix: %d, Levels: %d), provided do not match the requirements " +
                            "for a simple folded clos network.", nodes.size(), radix));
        }

        int numFirstLayerGroups = nodes.size() / radix;

        List<Switch<T>> firstLayerSwitches = new ArrayList<>();
        for (int i = 0; i < numFirstLayerGroups; i++) {
            int index = i;
            List<? extends EndpointNode<T>> endpointSublist = nodes.subList(index * radix, (index + 1) * radix);
            Switch<T> directSwitch_ = new Switch<>(
                    getFoldedClosSwitchName(1, 0, i),
                    new ArrayList<>(nodes),
                    endpointSublist,
                    levelRngFunction.apply(1));
            firstLayerSwitches.add(directSwitch_);
            endpointSublist.forEach(switch_ -> switch_.setOutflowNodes(List.of(firstLayerSwitches.get(index))));
        }

        List<Switch<T>> allSwitches = new ArrayList<>(firstLayerSwitches);

        int currentLayer = 2;
        int nextGroupSize;
        List<List<Switch<T>>> prevLayerGroupedSwitches = List.of(firstLayerSwitches);
        do {
            List<List<Switch<T>>> newLayerGroupedSwitches = new ArrayList<>();
            for (int i = 0; i < prevLayerGroupedSwitches.size(); i++) {
                List<Switch<T>> prevGroupedSwitches = prevLayerGroupedSwitches.get(i);
                newLayerGroupedSwitches.addAll(addNextSwitchLayer(nodes, prevGroupedSwitches,
                        radix, currentLayer, i, levelRngFunction));
            }
            currentLayer++;
            prevLayerGroupedSwitches = newLayerGroupedSwitches;
            newLayerGroupedSwitches.forEach(allSwitches::addAll);
            nextGroupSize = newLayerGroupedSwitches.get(0).size();
        } while (nextGroupSize != 1);

        RoutingUtil.updateRoutingTables(allSwitches);
        return allSwitches;
    }

    private static <T> List<List<Switch<T>>> addNextSwitchLayer(List<? extends EndpointNode<T>> endpoints,
            List<Switch<T>> prevLayer, int radix, int level, int group, Function<Integer,
            RandomNumberGenerator> levelRngFunction) {
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
                    .map(index -> new Switch<>(getFoldedClosSwitchName(level,
                            effectiveRadix * group + index, finalGroupNumber),
                            new ArrayList<>(endpoints),
                            List.of(),
                            levelRngFunction.apply(level)))
                    .collect(Collectors.toList());
            List<Switch<T>> prevLayerGroup = Stream.iterate(0, index -> index < effectiveRadix, index -> index + 1)
                    .map(index -> prevLayer.get(finalGroupNumber * effectiveRadix + index))
                    .collect(Collectors.toList());
            prevLayerGroup.forEach(switch_ -> switch_.updateSwitchNeighbors(newNeighborGroup));
            newNeighborGroup.forEach(switch_ -> switch_.setSwitchNeighbors(prevLayerGroup));
            for (int index = 0; index < radix; index++) {
                nextLayerSwitches.get(index).add(newNeighborGroup.get(index));
            }
        }
        return nextLayerSwitches;
    }

    private static String getFoldedClosSwitchName(int level, int group, int index) {
        return String.format("Folded-Clos-(Level: %d, Group: %d, Index: %d)", level, group, index);
    }

    private static boolean isValidFoldedClosSetup(int N, int radix) {
        while (N > radix) {
            if (N % radix != 0) {
                return false;
            }
            N = N / radix;
        }
        return true;
    }
}
