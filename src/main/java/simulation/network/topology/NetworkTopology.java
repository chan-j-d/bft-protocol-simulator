package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.entity.Node;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetworkTopology {
    public static <T> List<Switch<T>> arrangeCliqueStructure(
            List<? extends EndpointNode<T>> nodes) {
        List<Switch<T>> switches = new ArrayList<>();
        for (EndpointNode<T> node : nodes) {
            Switch<T> switch_ = new Switch<>("Switch-" + node.getName(), List.of(node), new ArrayList<>(nodes));
            switches.add(switch_);
            node.setOutflowNodes(List.of(switch_));
        }

        for (Switch<T> switch_ : switches) {
            switch_.setNeighbors(new ArrayList<>(switches));
        }
        return switches;
    }

    public static <T> List<Switch<T>> arrangeMeshStructure(
            List<? extends EndpointNode<T>> nodes, int n) {
        if (nodes.size() % n != 0) {
            throw new RuntimeException(String.format(
                    "Specified side length %d does not divide number of nodes %d", n, nodes.size()));
        }

        List<List<Switch<T>>> switchArray = new ArrayList<>();
        List<Switch<T>> switches = new ArrayList<>();
        int m = nodes.size() / n;

        // creates an incomplete version of all the switches
        for (int i = 0; i < n; i++) {
            switchArray.add(new ArrayList<>());
            for (int j = 0; j < m; j++) {
                String switchName = String.format("Mesh-Switch-(x: %d, y: %d)", i, j);
                EndpointNode<T> endNode = nodes.get(i * m + j);
                Switch<T> newSwitch = new Switch<>(switchName, List.of(endNode), new ArrayList<>(nodes));
                switchArray.get(i).add(newSwitch);
                switches.add(newSwitch);

                endNode.setOutflowNodes(List.of(newSwitch));
            }
        }

        // sets neighbors for each switch
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                List<Switch<T>> neighboringSwitches = Stream.of(new Pair<>(i - 1, j - 1), new Pair<>(i - 1, j + 1),
                        new Pair<>(i + 1, j - 1), new Pair<>(i + 1, j + 1))
                        .filter(pair -> pair.first() >= 0 && pair.first() < n
                                && pair.second() >= 0 && pair.second() < m)
                        .map(coordinates -> switchArray.get(coordinates.first()).get(coordinates.second()))
                        .collect(Collectors.toList());
                Switch<T> switch_ = switchArray.get(i).get(j);
                switch_.setNeighbors(neighboringSwitches);
            }
        }

        RoutingUtil.updateRoutingTables(switches);

        return switches;
    }

    public static <T> List<Switch<T>> arrangeFoldedClosStructure(
            List<? extends EndpointNode<T>> nodes, int radix, int levels) {
        if (!isValidFoldedClosSetup(nodes.size(), radix, levels)) {
            throw new RuntimeException(String.format(
                    "The dimensions (Size: %d, Radix: %d, Levels: %d), provided do not match the requirements " +
                            "for a simple folded clos network.", nodes.size(), radix, levels));
        }

        int numFirstLayerGroups = nodes.size() / radix;
        List<Switch<T>> allSwitches = new ArrayList<>();

        List<Switch<T>> firstLayerSwitches = Stream
                .iterate(0, index -> index < numFirstLayerGroups, index -> index + 1)
                .map(index -> new Switch<>(
                        getFoldedClosSwitchName(1, 1, index),
                        nodes.subList(0, index * radix),
                        new ArrayList<>(nodes)))
                .collect(Collectors.toList());
        for (int i = 0; i < numFirstLayerGroups; i++) {
            int index = i;
            List<? extends EndpointNode<T>> endpointSublist = nodes.subList(0, i * radix);
            endpointSublist.forEach(switch_ -> switch_.setOutflowNodes(List.of(firstLayerSwitches.get(index))));
        }

        allSwitches.addAll(firstLayerSwitches);

        int currentLayer = 2;
        List<List<Switch<T>>> prevLayerGroupedSwitches = List.of(firstLayerSwitches);
        while (currentLayer <= levels) {
            List<List<Switch<T>>> newLayerGroupedSwitches = new ArrayList<>();
            for (List<Switch<T>> prevGroupedSwitches : prevLayerGroupedSwitches) {
                newLayerGroupedSwitches.addAll(addNextSwitchLayer(nodes, prevGroupedSwitches, radix, currentLayer));
            }
            currentLayer++;
            prevLayerGroupedSwitches = newLayerGroupedSwitches;
            newLayerGroupedSwitches.stream()
                    .forEach(allSwitches::addAll);
        }

        return allSwitches;
    }

    private static <T> List<List<Switch<T>>> addNextSwitchLayer(
            List<? extends EndpointNode<T>> endpoints, List<Switch<T>> prevLayer, int radix, int level) {
        int numNodes = prevLayer.size();
        int numGroups = numNodes / radix;

        List<List<Switch<T>>> nextLayerSwitches = new ArrayList<>();
        Stream.generate(() -> new ArrayList<Switch<T>>()).limit(numGroups).forEach(nextLayerSwitches::add);
        for (int groupNumber = 0; groupNumber < numGroups; groupNumber++) {
            int finalGroupNumber = groupNumber;
            List<Switch<T>> newNeighborGroup = Stream.iterate(0, index -> index < radix, index -> index + 1)
                    .map(index -> new Switch<>(getFoldedClosSwitchName(level, finalGroupNumber, index),
                            List.of(),
                            new ArrayList<>(endpoints)))
                    .collect(Collectors.toList());
            for (int index = 0; index < radix; index++) {
                int prevLayerIndex = groupNumber * radix + index;
                prevLayer.get(prevLayerIndex).updateNeighbors(newNeighborGroup);
                nextLayerSwitches.get(index).add(newNeighborGroup.get(index));
            }
        }
        return nextLayerSwitches;
    }

    private static String getFoldedClosSwitchName(int level, int group, int index) {
        return String.format("Folded-Clos-(Level: %d, Group: %d, Index: %d)", level, group, index);
    }

    private static boolean isValidFoldedClosSetup(int N, int radix, int levels) {
        if (levels == 1) {
            return false;
        }
        return N % radix == 0;
    }
}
