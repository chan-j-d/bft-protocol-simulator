package simulation;

import com.google.gson.Gson;
import simulation.json.IBFTResultsJson;
import simulation.json.QueueResultsJson;
import simulation.json.RunConfigJson;
import simulation.io.FileIo;
import simulation.io.IoInterface;
import simulation.network.entity.EndpointNode;
import simulation.network.entity.Node;
import simulation.network.entity.ibft.IBFTMessage;
import simulation.network.entity.ibft.IBFTNode;
import simulation.network.entity.ibft.IBFTStatistics;
import simulation.network.router.Switch;
import simulation.network.topology.NetworkTopology;
import simulation.simulator.Simulator;
import simulation.statistics.QueueStatistics;
import simulation.util.logging.Logger;
import simulation.util.rng.ExponentialDistribution;

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
import java.util.Random;
import java.util.logging.LogManager;

public class Main {

    public static Logger MAIN_LOGGER;
    private static final Path JSON_DIRECTORY = Paths.get("json");
    private static final String RUN_CONFIG_JSON_FILEPATH = JSON_DIRECTORY.resolve("run_config.json").toString();
    private static final String RESULTS_JSON_FILEPATH =
            JSON_DIRECTORY.resolve("validator_results.json").toString();
    private static final String SWITCH_GROUP_STATISTICS =
            JSON_DIRECTORY.resolve("switch_group_%d.json").toString();
    private static final Gson GSON = new Gson();
    public static void main(String[] args) {
        setup();

        RunConfigJson runConfigJson = readFromJson(RUN_CONFIG_JSON_FILEPATH, RunConfigJson.class);

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
            ExponentialDistribution.UNIFORM_DISTRIBUTION = new Random(startingSeed + seedMultiplier * i);
//            IoInterface io = new ConsoleIo();
            List<IBFTNode> nodes = new ArrayList<>();
            Simulator<IBFTMessage> simulator = new Simulator<>();
            for (int j = 0; j < numNodes; j++) {
                nodes.add(new IBFTNode("IBFT-" + j, j, timeLimit, simulator, numNodes, consensusLimit,
                        new ExponentialDistribution(validatorServiceRate)));
            }
            simulator.setNodes(nodes);
            List<List<Switch<IBFTMessage>>> groupedSwitches = arrangeNodesFollowingConfigTopology(runConfigJson, nodes);

            for (IBFTNode node : nodes) {
                node.setAllNodes(nodes);
            }
            while (!simulator.isSimulationOver()) {
                simulator.simulate().ifPresent(io::output);
            }
            io.output("\nSnapshot:\n" + simulator.getSnapshotOfNodes());

            IBFTStatistics runStats = nodes.stream()
                    .map(IBFTNode::getIbftStatistics)
                    .reduce(IBFTStatistics::addStatistics).orElseThrow();
            ibftStats = Optional.ofNullable(ibftStats)
                    .map(stats -> stats.addStatistics(runStats))
                    .orElse(runStats);

            QueueStatistics runValidatorQueueStats = nodes.stream()
                    .map(Node::getQueueStatistics)
                    .reduce(QueueStatistics::addStatistics).orElseThrow();
            validatorQueueStats = Optional.ofNullable(validatorQueueStats)
                    .map(stats -> stats.addStatistics(runValidatorQueueStats))
                    .orElse(runValidatorQueueStats);


            String ibftStatisticsResults = runStats.toString();
            io.output("\nSummary:");
            io.output(ibftStatisticsResults);

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
        io.output(ibftStats.toString());
        io.output(validatorQueueStats.toString());
        io.close();

        IBFTResultsJson ibftResultsJson = new IBFTResultsJson(numNodes, consensusLimit, ibftStats.getNewRoundTime(),
                ibftStats.getPrePreparedTime(), ibftStats.getPrepared(), ibftStats.getAverageConsensusTime());
        writeObjectToJson(ibftResultsJson, RESULTS_JSON_FILEPATH);

        for (int i = 0; i < numGroups; i++) {
            QueueStatistics queueStatistics = switchStatistics.get(i);
            QueueResultsJson queueResultsJson = new QueueResultsJson(queueStatistics.getAverageNumMessagesInQueue(),
                    queueStatistics.getMessageArrivalRate(), queueStatistics.getAverageMessageWaitingTime());
            writeObjectToJson(queueResultsJson, String.format(SWITCH_GROUP_STATISTICS, i));
        }

        System.out.println(ibftStats);
        System.out.println("\nAverage queue stats");
        System.out.println(validatorQueueStats);

        cleanup();
    }

    public static <T> List<List<Switch<T>>> arrangeNodesFollowingConfigTopology(RunConfigJson json,
            List<? extends EndpointNode<T>> nodes) {
        double switchServiceRate = json.getSwitchProcessingRate();
        String networkType = json.getNetworkType();
        List<Integer> networkParameters = json.getNetworkParameters();
        switch (networkType) {
        case "FoldedClos": case "fc":
            return NetworkTopology.arrangeFoldedClosStructure(nodes, networkParameters,
                    x -> new ExponentialDistribution(switchServiceRate));
        case "Butterfly": case "b":
            return NetworkTopology.arrangeButterflyStructure(nodes, networkParameters,
                    x -> new ExponentialDistribution(switchServiceRate));
        case "Clique": case "c":
            return NetworkTopology.arrangeCliqueStructure(nodes, networkParameters,
                    () -> new ExponentialDistribution(switchServiceRate));
        case "Torus": case "t":
            return NetworkTopology.arrangeTorusStructure(nodes, networkParameters,
                    () -> new ExponentialDistribution(switchServiceRate));
        case "Mesh": case "m":
            return NetworkTopology.arrangeMeshStructure(nodes, networkParameters,
                    () -> new ExponentialDistribution(switchServiceRate));
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
                    RUN_CONFIG_JSON_FILEPATH));
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