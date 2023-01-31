package simulation;

import simulation.io.FileIo;
import simulation.io.IoInterface;
import simulation.io.NoIo;
import simulation.network.entity.ibft.IBFTNode;
import simulation.simulator.Simulator;
import simulation.util.rng.ExponentialDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static simulation.network.structure.NetworkStructure.cliqueStructure;

public class Main {
    public static void main(String[] args) {


        double timeLimit = 10000;

        int numNodes = 48;
        int numTrials = 5;
        int seedMultiplier = 5;

        double totalTime2 = 0;
        int totalNodesConsensus = 0;
        for (int j = 0; j < numTrials; j++) {
            ExponentialDistribution.UNIFORM_DISTRIBUTION = new Random(seedMultiplier * j);
            IoInterface io = new FileIo("output" + j + ".txt");
            List<IBFTNode> nodes = new ArrayList<>();

            for (int i = 0; i < numNodes; i++) {
                nodes.add(new IBFTNode("IBFT-" + i, i, timeLimit));
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
            io.output("\nSnapshot:\n" + simulator.getSnapshotOfNodes());

            int numNodesReachingConsensus = 0;
            double totalTime = 0;
            for (IBFTNode node : nodes) {
                if (node.getConsensusTime() != -1) {
                    totalTime += node.getConsensusTime();
                    numNodesReachingConsensus += 1;
                }
            }

            io.output("Average time to consensus: " + totalTime / numNodesReachingConsensus);

            System.out.println(totalTime / numNodesReachingConsensus);
            totalNodesConsensus += numNodesReachingConsensus;
            totalTime2 += totalTime;
            io.close();
        }

        System.out.println(totalTime2 / totalNodesConsensus);
    }
}