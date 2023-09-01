package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayTopololgy {
    /**
     * Constructs a mesh topology with the given {@code nodes}.
     * A 2D mesh is constructed with one side length specified.
     *
     * @param nodes Nodes to be connected in a mesh topology.
     * @param networkParameters Side length of the 2D mesh topology.
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return Returns a list of a single list of proxy switches for each node.
     * @param <T> Message class being carried by switches.
     */
    public static <T> List<List<Switch<T>>> arrangeMeshStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
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
                createSwitchArray(nodes, n, m, "Mesh-Switch-(x: %d, y: %d)",
                        messageChannelSuccessRate, switchProcessingTimeGenerator);
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

    /**
     * Creates an n x m array of switches where each switch is directly connected to a unique node.
     * Connections are not established in this method.
     *
     * @param nodes Nodes to be connected in a 2D array topology.
     * @param n One side length of the 2D array.
     * @param m The other side length of the 2D array.
     * @param nameFormat Name format for switches.
     *                   Requires a string format argument that takes in its 2D index on the array.
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return List of a single list of switches that form the 2D array.
     * @param <T> Message class being carried by switches.
     */
    private static <T> List<List<Switch<T>>> createSwitchArray(List<? extends EndpointNode<T>> nodes,
            int n, int m, String nameFormat, double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
        List<List<Switch<T>>> switchArray = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            switchArray.add(new ArrayList<>());
            for (int j = 0; j < m; j++) {
                String switchName = String.format(nameFormat, i, j);
                EndpointNode<T> endNode = nodes.get(i * m + j);
                Switch<T> newSwitch =
                        new Switch<>(switchName, messageChannelSuccessRate,
                                new ArrayList<>(nodes), List.of(endNode), switchProcessingTimeGenerator);
                switchArray.get(i).add(newSwitch);
                endNode.setOutflowNodes(List.of(newSwitch));
            }
        }
        return switchArray;
    }

    /**
     * Constructs a Torus topology with the given {@code nodes}.
     * A 2D Torus is constructed with one side length specified.
     *
     * @param nodes Nodes to be connected in a Torus topology.
     * @param networkParameters Side length of the 2D Torus topology.
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return Returns a list of a single list of proxy switches for each node.
     * @param <T> Message class being carried by switches.
     */
    public static <T> List<List<Switch<T>>> arrangeTorusStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
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
                messageChannelSuccessRate, switchProcessingTimeGenerator);
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
}
