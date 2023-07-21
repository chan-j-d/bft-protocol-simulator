package simulation.simulator;

import simulation.json.NetworkConfigurationJson;
import simulation.json.RunConfigJson;
import simulation.json.SwitchConfigJson;
import simulation.json.ValidatorConfigJson;
import simulation.network.entity.BFTMessage;
import simulation.network.entity.EndpointNode;
import simulation.network.entity.Validator;
import simulation.network.entity.hotstuff.HSMessage;
import simulation.network.entity.hotstuff.HSReplica;
import simulation.network.entity.ibft.IBFTMessage;
import simulation.network.entity.ibft.IBFTNode;
import simulation.network.router.Switch;
import simulation.network.topology.NetworkTopology;
import simulation.util.rng.DegenerateDistribution;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Contains utility methods for reading a run configuration.
 */
public class RunConfigUtil {

    /**
     * Creates a {@code Simulator} from the given run configuration {@code json}.
     */
    public static Simulator createSimulator(RunConfigJson json) {
        ValidatorConfigJson validatorSettings = json.getValidatorSettings();
        String validatorNodeType = validatorSettings.getValidatorType();
        int numNodes = validatorSettings.getNumNodes();
        double timeLimit = validatorSettings.getBaseTimeLimit();
        int consensusLimit = validatorSettings.getNumConsensus();
        double validatorServiceRate = validatorSettings.getNodeProcessingRate();
        switch (validatorNodeType) {
        case "HS": case "HotStuff":
            SimulatorImpl<HSMessage> hsSimulator = new SimulatorImpl<>();
            List<Validator<HSMessage>> hsNodes = IntStream.iterate(0, x -> x < numNodes, x -> x + 1)
                    .mapToObj(x -> new HSReplica("HS-" + x, x, timeLimit, hsSimulator, numNodes, consensusLimit,
                            new ExponentialDistribution(validatorServiceRate))).collect(Collectors.toList());
            hsSimulator.setNodes(hsNodes);
            fixNetworkConnections(json, hsSimulator);
            return hsSimulator;
        case "IBFT":
            SimulatorImpl<IBFTMessage> ibftSimulator = new SimulatorImpl<>();
            List<Validator<IBFTMessage>> ibftNodes = IntStream.iterate(0, x -> x < numNodes, x -> x + 1)
                    .mapToObj(x -> new IBFTNode("IBFT-" + x, x, timeLimit, ibftSimulator, numNodes, consensusLimit,
                            new ExponentialDistribution(validatorServiceRate))).collect(Collectors.toList());
            ibftSimulator.setNodes(ibftNodes);
            fixNetworkConnections(json, ibftSimulator);
            return ibftSimulator;
        default:
            throw new RuntimeException(String.format("%s is an unrecognised validator node type.", validatorNodeType));
        }
    }

    /**
     * Fixes the arrangement of the nodes in {@code simulator} according to the given run configuration {@code json}.
     */
    private static <T extends BFTMessage> void fixNetworkConnections(RunConfigJson json, SimulatorImpl<T> simulator) {
        List<Validator<T>> nodes = simulator.getNodes();
        for (Validator<T> node : nodes) {
            node.setAllNodes(nodes);
        }
        List<List<Switch<T>>> switches = arrangeNodesInTopology(json, simulator.getNodes());
        simulator.setSwitches(switches);
    }

    /**
     * Identifies the network topology specified in the json file and arranges the {@code nodes} according to it.
     */
    public static <T> List<List<Switch<T>>> arrangeNodesInTopology(RunConfigJson json,
            List<? extends EndpointNode<T>> nodes) {
        NetworkConfigurationJson networkSettings = json.getNetworkSettings();
        SwitchConfigJson switchSettings = networkSettings.getSwitchSettings();
        double switchServiceRate = switchSettings.getSwitchProcessingRate();
        double messageChannelSuccessRate = switchSettings.getMessageChannelSuccessRate();
        String networkType = networkSettings.getNetworkType();
        List<Integer> networkParameters = networkSettings.getNetworkParameters();
        Function<Integer, RandomNumberGenerator> processingGeneratorFunction =
                switchServiceRate < 0 ? x -> new DegenerateDistribution(0)
                        : x -> new ExponentialDistribution(switchServiceRate);
        Supplier<RandomNumberGenerator> processingGeneratorSupplier =
                switchServiceRate < 0 ? () -> new DegenerateDistribution(0)
                        : () -> new ExponentialDistribution(switchServiceRate);
        switch (networkType) {
            case "FoldedClos": case "fc":
                return NetworkTopology.arrangeFoldedClosStructure(nodes, networkParameters,
                        messageChannelSuccessRate, processingGeneratorFunction);
            case "Butterfly": case "b":
                return NetworkTopology.arrangeButterflyStructure(nodes, networkParameters,
                        messageChannelSuccessRate, processingGeneratorFunction);
            case "Clique": case "c":
                return NetworkTopology.arrangeCliqueStructure(nodes, messageChannelSuccessRate,
                        processingGeneratorSupplier);
            case "Torus": case "t":
                return NetworkTopology.arrangeTorusStructure(nodes, networkParameters, messageChannelSuccessRate,
                        processingGeneratorSupplier);
            case "Mesh": case "m":
                return NetworkTopology.arrangeMeshStructure(nodes, networkParameters, messageChannelSuccessRate,
                        processingGeneratorSupplier);
            default:
                throw new RuntimeException(String.format("The network type %s has not been defined/implemented.",
                        networkType));
        }
    }
}
