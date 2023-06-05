package simulation;

import com.google.gson.Gson;
import simulation.io.FileIo;
import simulation.io.IoInterface;
import simulation.json.QueueResultsJson;
import simulation.json.RunConfigJson;
import simulation.network.entity.EndpointNode;
import simulation.network.entity.Node;
import simulation.network.entity.hotstuff.HSMessage;
import simulation.network.entity.hotstuff.HSReplica;
import simulation.network.entity.ibft.IBFTStatistics;
import simulation.network.router.Switch;
import simulation.network.topology.NetworkTopology;
import simulation.simulator.Simulator;
import simulation.statistics.QueueStatistics;
import simulation.util.logging.Logger;
import simulation.util.rng.DegenerateDistribution;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RNGUtil;
import simulation.util.rng.RandomNumberGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.LogManager;

public class Main {

    private static final Path JSON_DIRECTORY = Paths.get("json");
    private static final String RESULTS_JSON_FILEPATH =
            JSON_DIRECTORY.resolve("validator_results.json").toString();
    private static final String SWITCH_GROUP_STATISTICS =
            JSON_DIRECTORY.resolve("switch_group_%d.json").toString();
    private static final Gson GSON = new Gson();
    public static void main(String[] args) {
        setup();

        RunConfigJson runConfigJson = readFromJson(args[0], RunConfigJson.class);

        double timeLimit = runConfigJson.getBaseTimeLimit();
        int numNodes = runConfigJson.getNumNodes();
        int numTrials = runConfigJson.getNumRuns();
        int seedMultiplier = runConfigJson.getSeedMultiplier();
        int consensusLimit = runConfigJson.getNumConsensus();
        int startingSeed = runConfigJson.getStartingSeed();
        double validatorServiceRate = runConfigJson.getNodeProcessingRate();

        IoInterface io = new FileIo("output.txt");
        IBFTStatistics ibftStats = null;
        QueueStatistics validatorQueueStats = null;
        List<QueueStatistics> switchStatistics = new ArrayList<>();
        int numGroups = -1;
        for (int i = 0; i < numTrials; i++) {
            RNGUtil.setSeed(startingSeed + seedMultiplier * i);
//            List<IBFTNode> nodes = new ArrayList<>();
//            Simulator<IBFTMessage> simulator = new Simulator<>();
            List<HSReplica> nodes = new ArrayList<>();
            Simulator<HSMessage> simulator = new Simulator<>();
            for (int j = 0; j < numNodes; j++) {
//                nodes.add(new IBFTNode("IBFT-" + j, j, timeLimit, simulator, numNodes, consensusLimit,
//                        new ExponentialDistribution(validatorServiceRate)));
                nodes.add(new HSReplica("HS-" + j, j, timeLimit, simulator, numNodes, consensusLimit,
                        new ExponentialDistribution(validatorServiceRate)));
            }
            simulator.setNodes(nodes);
//            List<List<Switch<IBFTMessage>>> groupedSwitches = arrangeNodesFollowingConfigTopology(runConfigJson, nodes);
            List<List<Switch<HSMessage>>> groupedSwitches = arrangeNodesFollowingConfigTopology(runConfigJson, nodes);

//            for (IBFTNode node : nodes) {
            for (HSReplica node : nodes) {
                node.setAllNodes(nodes);
            }
            while (!simulator.isSimulationOver()) {
                simulator.simulate().ifPresent(io::output);
            }
            io.output("\nSnapshot:\n" + simulator.getSnapshotOfNodes());

//            IBFTStatistics runStats = nodes.stream()
//                    .map(IBFTNode::getIbftStatistics)
//                    .reduce(IBFTStatistics::addStatistics).orElseThrow();
//            ibftStats = Optional.ofNullable(ibftStats)
//                    .map(stats -> stats.addStatistics(runStats))
//                    .orElse(runStats);

            QueueStatistics runValidatorQueueStats = nodes.stream()
                    .map(Node::getQueueStatistics)
                    .reduce(QueueStatistics::addStatistics).orElseThrow();
            validatorQueueStats = Optional.ofNullable(validatorQueueStats)
                    .map(stats -> stats.addStatistics(runValidatorQueueStats))
                    .orElse(runValidatorQueueStats);


//            String ibftStatisticsResults = runStats.toString();
//            io.output("\nSummary:");
//            io.output(ibftStatisticsResults);

            numGroups = groupedSwitches.size();
            io.output("\nEvery switch summary:");
            groupedSwitches.forEach(group ->
                    group.stream().map(switch_ -> switch_.getName() + "\n" + switch_.getQueueStatistics())
                            .forEach(io::output));
            if (i == 0) {
                groupedSwitches.forEach(group -> switchStatistics.add(group.stream().map(Node::getQueueStatistics)
                        .reduce(QueueStatistics::addStatistics).orElseThrow()));
            } else {
                for (int j = 0; j < groupedSwitches.size(); j++) {
                    QueueStatistics switchQueueStatistics = groupedSwitches.get(j).stream()
                            .map(Node::getQueueStatistics)
                            .reduce(QueueStatistics::addStatistics).orElseThrow();
                    switchStatistics.set(j, switchStatistics.get(j).addStatistics(switchQueueStatistics));
                }
            }
        }
//        io.output(ibftStats.toString());
//        io.output(validatorQueueStats.toString());
        io.close();



//        IBFTResultsJson ibftResultsJson = new IBFTResultsJson(ibftStats, validatorQueueStats);
//        writeObjectToJson(ibftResultsJson, RESULTS_JSON_FILEPATH);

        for (int i = 0; i < numGroups; i++) {
            QueueStatistics queueStatistics = switchStatistics.get(i);
            QueueResultsJson queueResultsJson = new QueueResultsJson(queueStatistics.getAverageNumMessagesInQueue(),
                    queueStatistics.getMessageArrivalRate(), queueStatistics.getAverageMessageWaitingTime());
            writeObjectToJson(queueResultsJson, String.format(SWITCH_GROUP_STATISTICS, i));
        }

//        System.out.println(ibftStats);
        System.out.println("\nAverage queue stats");
        System.out.println(validatorQueueStats);

        cleanup();
    }

    public static <T> List<List<Switch<T>>> arrangeNodesFollowingConfigTopology(RunConfigJson json,
            List<? extends EndpointNode<T>> nodes) {
        double switchServiceRate = json.getSwitchProcessingRate();
        String networkType = json.getNetworkType();
        List<Integer> networkParameters = json.getNetworkParameters();
        Function<Integer, RandomNumberGenerator> processingGeneratorFunction =
                switchServiceRate < 0 ? x -> new DegenerateDistribution(0)
                        : x -> new ExponentialDistribution(switchServiceRate);
        Supplier<RandomNumberGenerator> processingGeneratorSupplier =
                switchServiceRate < 0 ? () -> new DegenerateDistribution(0)
                        : () -> new ExponentialDistribution(switchServiceRate);
        switch (networkType) {
        case "FoldedClos": case "fc":
            return NetworkTopology.arrangeFoldedClosStructure(nodes, networkParameters,
                    processingGeneratorFunction);
        case "Butterfly": case "b":
            return NetworkTopology.arrangeButterflyStructure(nodes, networkParameters,
                    processingGeneratorFunction);
        case "Clique": case "c":
            return NetworkTopology.arrangeCliqueStructure(nodes, networkParameters,
                    processingGeneratorSupplier);
        case "Torus": case "t":
            return NetworkTopology.arrangeTorusStructure(nodes, networkParameters,
                    processingGeneratorSupplier);
        case "Mesh": case "m":
            return NetworkTopology.arrangeMeshStructure(nodes, networkParameters,
                    processingGeneratorSupplier);
        default:
            throw new RuntimeException(String.format("The network type %s has not been defined/implemented.",
                    networkType));
        }
    }

    public static <T> T readFromJson(String filename, Class<T> clazz) {
        try (FileReader fr = new FileReader(filename)) {
            return GSON.fromJson(fr, clazz);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to locate/parse %s file to read as Json.",
                    filename));
        }
    }

    public static void writeObjectToJson(Object object, String filename) {
        try (FileWriter fw = new FileWriter(filename)) {
            GSON.toJson(object, fw);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to write %s to %s json file.", object, filename));
        }
    }

    private static void setup() {
        deleteFilesInDirectory(Logger.DEFAULT_DIRECTORY);
        deleteFilesInDirectory(JSON_DIRECTORY.toString());
        Logger.setup();
    }

    private static void deleteFilesInDirectory(String path) {
        try {
            File directory = new File(path);
            if (directory.exists() && directory.isDirectory()) {
                for (File file : directory.listFiles()) {
                    file.delete();
                }
            }
            Files.deleteIfExists(Paths.get(Logger.DEFAULT_DIRECTORY));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void cleanup() {
        LogManager.getLogManager().reset();
    }
}