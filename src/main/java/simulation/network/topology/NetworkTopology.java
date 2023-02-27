package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.entity.Node;
import simulation.network.router.RoutingUtil;
import simulation.network.router.Switch;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkTopology {
    public static <T> Pair<List<EndpointNode<T>>, List<Switch<T>>> arrangeCliqueStructure(
            List<? extends EndpointNode<T>> nodes) {
        List<Switch<T>> switches = new ArrayList<>();
        for (EndpointNode<T> node : nodes) {
            Switch<T> switch_ = new Switch<>("Switch-" + node.getName(), new ArrayList<>(nodes));
            switches.add(switch_);
            node.setOutflowNodes(List.of(switch_));
        }

        for (Switch<T> switch_ : switches) {
            switch_.setNeighbors(new ArrayList<>(switches));
        }
        return new Pair<>(new ArrayList<>(nodes), switches);
    }

    public static <T> Pair<List<Node<T>>, List<Switch<T>>> arrangeMeshStructure(
            List<? extends EndpointNode<T>> nodes, int n) {
        if (nodes.size() % n != 0) {
            throw new RuntimeException(String.format(
                    "Specified side length %d does not divide number of nodes %d", n, nodes.size()));
        }

        List<List<Switch<T>>> switchArray = new ArrayList<>();
        List<Switch<T>> switches = new ArrayList<>();
        int m = nodes.size() / n;
        for (int i = 0; i < n; i++) {
            switchArray.add(new ArrayList<>());
            for (int j = 0; j < m; j++) {
                String switchName = String.format("Switch (%d, %d)", i, j);
                Switch<T> newSwitch = new Switch<>(switchName, new ArrayList<>(nodes));
                switchArray.get(i).add(newSwitch);
                switches.add(newSwitch);
            }
        }
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

        return new Pair<>(new ArrayList<>(nodes), switches);
    }

    public static <T> List<Node<T>> arrangeClosStructure(List<? extends EndpointNode<T>> nodes, int n, int m, int p) {
        return null;
    }
}
