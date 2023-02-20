package simulation;

import simulation.io.ConsoleIo;
import simulation.io.IoInterface;
import simulation.network.entity.ibft.IBFTMessage;
import simulation.network.entity.ibft.IBFTNode;
import simulation.simulator.Simulator;
import simulation.util.rng.ExponentialDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static simulation.network.structure.NetworkStructure.arrangeCliqueStructure;

public class Main {
    public static void main(String[] args) {


        double timeLimit = 100;

        int numNodes = 4;
        int numTrials = 1;
        int seedMultiplier = 100;
        int consensusLimit = 10;

        double totalTime2 = 0;
        int totalNodesConsensus = 0;
        for (int j = 0; j < numTrials; j++) {
            ExponentialDistribution.UNIFORM_DISTRIBUTION = new Random(seedMultiplier * j);
            //IoInterface io = new FileIo("output" + j + ".txt");
            IoInterface io = new ConsoleIo();
            List<IBFTNode> nodes = new ArrayList<>();
            Simulator<IBFTMessage> simulator = new Simulator<IBFTMessage>();
            for (int i = 0; i < numNodes; i++) {
                nodes.add(new IBFTNode("IBFT-" + i, i, timeLimit, simulator, numNodes, consensusLimit));
            }
            simulator.setNodes(nodes);
            arrangeCliqueStructure(nodes);
            for (IBFTNode node : nodes) {
                node.setAllNodes(nodes);
            }
            while (!simulator.isSimulationOver()) {
                io.output(simulator.simulate());
            }
            io.output("\nSnapshot:\n" + simulator.getSnapshotOfNodes());
            io.close();
        }
    }
}