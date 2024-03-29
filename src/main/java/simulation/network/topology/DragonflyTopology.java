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
     * @param networkParameters Network parameters for {# of switches in a group}.
     *                          Number of switches in a group - positive integer > 0
     *                          By default, # of groups = # of switches + 1.
     * @param messageChannelSuccessRate Success rate of a message being sent by the switch.
     * @param switchProcessingTimeGenerator Rng for switch processing time.
     * @return Returns a list of list of switches separated by level in the butterfly network.
     * @param <T> Message class being carried by switches.
     */
    public static <T> List<List<Switch<T>>> arrangeDragonflyStructure(List<? extends EndpointNode<T>> nodes,
            List<Integer> networkParameters,
            double messageChannelSuccessRate,
            RandomNumberGenerator switchProcessingTimeGenerator) {
        int a = networkParameters.get(0);
        int numGroups = a + 1;
        int numSwitches = a * numGroups;
        List<List<Switch<T>>> groupsOfSwitches = new ArrayList<>();
        for (int i = 0; i < numGroups; i++) {
            List<Switch<T>> groupSwitches = new ArrayList<>();
            groupsOfSwitches.add(groupSwitches);
            for (int j = 0; j < a; j++) {
                int index = i + j * numGroups;
                List<? extends EndpointNode<T>> endpointSublist = TopologyUtil.getEndpointSublist(
                        nodes, numSwitches, index);
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
                int correspondingGroup = (i + a - j) % numGroups;
                int correspondingIndex = a - 1 - j;
                switchNeighbors.add(groupsOfSwitches.get(correspondingGroup).get(correspondingIndex));
                Switch<T> currentSwitch = groupsOfSwitches.get(i).get(j);
                switchNeighbors.remove(currentSwitch);
                currentSwitch.setSwitchNeighbors(switchNeighbors);
            }
        }

        TopologyUtil.flattenAndUpdateRoutes(groupsOfSwitches);
        return groupsOfSwitches;
    }

}
