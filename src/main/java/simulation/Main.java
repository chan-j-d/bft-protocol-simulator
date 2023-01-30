package simulation;

import simulation.io.FileIo;
import simulation.io.IoInterface;
import simulation.network.entity.ibft.IBFTNode;
import simulation.simulator.Simulator;

import java.util.ArrayList;
import java.util.List;

import static simulation.network.structure.NetworkStructure.cliqueStructure;

public class Main {
    public static void main(String[] args) {

        IoInterface io = new FileIo("output.txt");

        List<IBFTNode> nodes = new ArrayList<>();
        int numNodes = 4;
        for (int i = 0; i < numNodes; i++) {
            nodes.add(new IBFTNode("IBFT-" + i, i, 1));
        }
        for (IBFTNode node : nodes) {
            List<IBFTNode> copy = new ArrayList<>(nodes);
            copy.remove(node);
            node.setOtherNodes(copy);
        }
        Simulator simulator = new Simulator(cliqueStructure(nodes));
        while (!simulator.isSimulationOver()) {
            io.output(simulator.simulate());
        }
    }
}