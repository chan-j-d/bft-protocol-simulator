package simulation;

import simulation.io.FileIo;
import simulation.io.IoInterface;
import simulation.network.entity.ibft.IBFTMessage;
import simulation.network.entity.ibft.IBFTNode;
import simulation.network.entity.ibft.IBFTStatistics;
import simulation.network.router.Switch;
import simulation.network.topology.NetworkTopology;
import simulation.simulator.Simulator;
import simulation.util.logging.Logger;
import simulation.util.rng.ExponentialDistribution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.LogManager;

public class Main {

    public static Logger MAIN_LOGGER;
    public static void main(String[] args) {
        setup();

        double timeLimit = 100000;

        int numNodes = 64;
        int numTrials = 1;
        int seedMultiplier = 100;
        int consensusLimit = 10;

        IBFTStatistics statistics = null;
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

            //List<Switch<IBFTMessage>> newSwitches = NetworkTopology.arrangeCliqueStructure(nodes);
            //List<Switch<IBFTMessage>> newSwitches = NetworkTopology.arrangeMeshStructure(nodes, 8);
            List<Switch<IBFTMessage>> newSwitches = NetworkTopology.arrangeFoldedClosStructure(nodes, 4, 3);

            for (IBFTNode node : nodes) {
                node.setAllNodes(nodes);
            }
            while (!simulator.isSimulationOver()) {
                simulator.simulate().ifPresent(io::output);
            }
            io.output("\nSnapshot:\n" + simulator.getSnapshotOfNodes());

            IBFTStatistics runStats = nodes.stream()
                    .map(IBFTNode::getStatistics)
                    .reduce(IBFTStatistics::addStatistics).orElseThrow();
            statistics = Optional.ofNullable(statistics)
                    .map(stats -> stats.addStatistics(runStats))
                    .orElse(runStats);

            String statisticsResults = runStats.toString();
            io.output("\nSummary:");
            io.output(statisticsResults);

            io.close();
        }

        String fullRunResults = statistics.toString();
        System.out.println(fullRunResults);

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