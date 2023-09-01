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

    /**
     * @param nodes Nodes to be arranged in a dragonfly topology.
     * @param networkParameters Network parameters for {radix, connection type}.
     *                          Number of terminals - positive integer > 0
     *                          Number of switches in a group - positive integer > 0
     *                          Connection type - 0 or 1. 0 for a flushed type connection and 1 for spread.
     *                          Distribution of nodes over switches - 0 or 1. 1 for fitting into as few groups as
     *                          possible and 0 for spreading out over as many groups as possible.
     *
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return Returns a list of list of switches separated by level in the butterfly network.
     * @param <T> Message class being carried by switches.
     */
    public static <T> List<List<Switch<T>>> arrangeDragonflyStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
        int p = networkParameters.get(0);
        int a = networkParameters.get(1);
        int numGroups = a + 1;
        int numSwitches = a * numGroups;
        List<List<Switch<T>>> groupsOfSwitches = new ArrayList<>();
        for (int i = 0; i < numGroups; i++) {
            List<Switch<T>> groupSwitches = new ArrayList<>();
            groupsOfSwitches.add(groupSwitches);
            for (int j = 0; j < a; j++) {
                int index = networkParameters.get(3) == 1 ? i * a + j : i + j * numGroups;
                List<? extends EndpointNode<T>> endpointSublist = TopologyUtil.getEndpointSublist(
                        nodes, networkParameters.get(2), numSwitches, p, index);
                Switch<T> newSwitch = new Switch<>(String.format("Switch-(G:%d,N:%d)", i, j), messageChannelSuccessRate,
                        new ArrayList<>(nodes),
                        endpointSublist,
                        switchProcessingTimeGenerator);
                groupSwitches.add(newSwitch);
                endpointSublist.forEach(node -> node.setOutflowNodes(List.of(newSwitch)));
            }
        }

        for (int i = 0; i < numGroups; i++) {
            for (int j = 0; j < a; j++) {
                List<Switch<T>> switchNeighbors = new ArrayList<>(groupsOfSwitches.get(i));
                int correspondingGroup = i + (a - j) - numGroups * (a - j >= numGroups - i ? 1 : 0);
                int correspondingIndex = a - 1 - j;
                switchNeighbors.add(groupsOfSwitches.get(correspondingGroup).get(correspondingIndex));
                groupsOfSwitches.get(i).get(j).setSwitchNeighbors(switchNeighbors);
            }
        }

        TopologyUtil.flattenAndUpdateRoutes(groupsOfSwitches);
        return groupsOfSwitches;
    }

}
