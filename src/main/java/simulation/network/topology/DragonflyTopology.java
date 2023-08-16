package simulation.network.topology;

import simulation.network.entity.EndpointNode;
import simulation.network.router.Switch;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a simplified variant of the Dragonfly topology.
 * Group topologies are fixed to be cliques and the no. of terminals, p, group members, a and global channels, h
 * follows the relationship a = 2p = 2h for load balancing.
 */
public class DragonflyTopology {


    public static <T> List<List<Switch<T>>> arrangeDragonflyStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
        int k = networkParameters.get(0);
        if (k % 4 != 0) {
            throw new RuntimeException(String.format("Invalid radix count for dragonfly: %d. " +
                    "Please specify a multiple of 4.", k));
        }

        int a = k / 2;
        int p = k / 4;
        int h = k / 4;

        List<List<Switch<T>>> groupsOfSwitches = new ArrayList<>();
        for (int i = 0; i < h + 1; i++) {
            List<Switch<T>> groupSwitches = new ArrayList<>();
            groupsOfSwitches.add(groupSwitches);

            for (int j = 0; j < a; j++) {

                List<? extends EndpointNode<T>> endpointSublist = TopologyUtil.getEndpointSublist(
                        nodes, networkParameters.get(1), a * (h + 1), p, j);
                groupSwitches.add(new Switch<>(String.format("Switch-(G:%d,N:%d)", i, j), messageChannelSuccessRate,
                        new ArrayList<>(nodes),
                        endpointSublist,
                        switchProcessingTimeGenerator));
            }
        }

        return null;
    }
}
