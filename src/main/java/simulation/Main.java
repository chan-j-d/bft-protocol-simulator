package simulation;

import simulation.io.FileIo;
import simulation.io.IoInterface;
import simulation.network.entity.ibft.IBFTMessage;
import simulation.network.entity.ibft.IBFTNode;
import simulation.simulator.Simulator;
import simulation.util.logging.Logger;
import simulation.util.rng.ExponentialDistribution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.LogManager;

import static simulation.network.structure.NetworkStructure.arrangeCliqueStructure;

public class Main {

    public static Logger MAIN_LOGGER;
    public static void main(String[] args) {
        setup();

        double timeLimit = 1000;

        int numNodes = 4;
        int numTrials = 1;
        int seedMultiplier = 100;
        int consensusLimit = 10;

        double totalTime2 = 0;
        int totalNodesConsensus = 0;
        for (int j = 0; j < numTrials; j++) {
            ExponentialDistribution.UNIFORM_DISTRIBUTION = new Random(seedMultiplier * j);
            IoInterface io = new FileIo("output" + j + ".txt");
            //IoInterface io = new ConsoleIo();
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
                simulator.simulate().ifPresent(io::output);
            }
            io.output("\nSnapshot:\n" + simulator.getSnapshotOfNodes());
            io.close();
        }

        cleanup();
    }

    private static void setup() {
        try {
            File directory = new File(Logger.DEFAULT_DIRECTORY);
            if (directory.exists() && directory.isDirectory()) {
                for (File file : directory.listFiles()) {
                    file.delete();
                }
            }
            Files.deleteIfExists(Paths.get(Logger.DEFAULT_DIRECTORY));
        } catch (IOException e) {
            MAIN_LOGGER.log("Unable to delete log directory before run\n" + e.getMessage());
            e.printStackTrace();
        }
        Logger.setup();
        MAIN_LOGGER = Logger.MAIN_LOGGER;
    }

    private static void cleanup() {
        LogManager.getLogManager().reset();
    }
}