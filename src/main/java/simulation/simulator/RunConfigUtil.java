package simulation.simulator;

import simulation.json.input.NetworkConfigurationJson;
import simulation.json.input.RunConfigJson;
import simulation.json.input.SwitchConfigJson;
import simulation.json.input.ValidatorConfigJson;
import simulation.network.entity.BFTMessage;
import simulation.network.entity.EndpointNode;
import simulation.network.entity.Validator;
import simulation.network.entity.timer.TimerNotifier;
import simulation.network.router.Switch;
import simulation.network.topology.NetworkTopology;
import simulation.protocol.ConsensusProgram;
import simulation.protocol.hotstuff.HSMessage;
import simulation.protocol.hotstuff.HSReplica;
import simulation.protocol.ibft.IBFTMessage;
import simulation.protocol.ibft.IBFTNode;
import simulation.util.Pair;
import simulation.util.rng.DegenerateDistribution;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Contains utility methods for reading a run configuration.
 */
public class RunConfigUtil {

    /**
     * Creates a {@code Simulator} from the given run configuration {@code json}.
     */
    public static Simulator createSimulator(RunConfigJson json) {
        ValidatorConfigJson validatorSettings = json.getValidatorSettings();
        String consensusProtocol = validatorSettings.getConsensusProtocol();
        int numNodes = validatorSettings.getNumNodes();
        double baseTimeLimit = validatorSettings.getBaseTimeLimit();
        int consensusLimit = validatorSettings.getNumConsensus();
        double validatorServiceRate = validatorSettings.getNodeProcessingRate();

        switch (consensusProtocol) {
        case "HS": case "HotStuff":
            SimulatorImpl<HSMessage> hsSimulator = new SimulatorImpl<>();
            Pair<List<Validator<HSMessage>>, Map<Integer, String>> hsPair = createValidatorNodes(numNodes,
                    validatorServiceRate, consensusLimit, hsSimulator);

            List<Validator<HSMessage>> hsNodes = hsPair.first();
            Map<Integer, String> idNameMap = hsPair.second();

            for (int i = 0; i < numNodes; i++) {
                Validator<HSMessage> currentNode = hsNodes.get(i);
                ConsensusProgram<HSMessage> program = new HSReplica(idNameMap.get(i), i, baseTimeLimit,
                        numNodes, idNameMap, currentNode);
                currentNode.setConsensusProgram(program);
            }

            hsSimulator.setNodes(hsNodes);
            fixNetworkConnections(json, hsSimulator);
            return hsSimulator;
        case "IBFT":
            SimulatorImpl<IBFTMessage> ibftSimulator = new SimulatorImpl<>();
            Pair<List<Validator<IBFTMessage>>, Map<Integer, String>> ibftPair = createValidatorNodes(numNodes,
                    validatorServiceRate, consensusLimit, ibftSimulator);

            List<Validator<IBFTMessage>> ibftNodes = ibftPair.first();
            idNameMap = ibftPair.second();

            for (int i = 0; i < numNodes; i++) {
                Validator<IBFTMessage> currentNode = ibftNodes.get(i);
                ConsensusProgram<IBFTMessage> program = new IBFTNode(idNameMap.get(i), i, baseTimeLimit,
                        numNodes, idNameMap, currentNode);
                currentNode.setConsensusProgram(program);
            }

            ibftSimulator.setNodes(ibftNodes);
            fixNetworkConnections(json, ibftSimulator);
            return ibftSimulator;
        default:
            throw new RuntimeException(String.format("%s is an unrecognised validator node type.", consensusProtocol));
        }
    }

    /**
     * Creates the validator nodes required for setup.
     *
     * @param numNodes Number of nodes to be created.
     * @param validatorServiceRate Service rate of validators.
     * @param consensusLimit Limit of consensus to be simulated.
     * @param timerNotifier Time notification for the validator. Used for setting timers.
     * @return Pair of list of validators and map of ids to node name.
     */
    private static <T extends BFTMessage> Pair<List<Validator<T>>, Map<Integer, String>> createValidatorNodes(
            int numNodes, double validatorServiceRate, int consensusLimit, TimerNotifier<Validator<T>> timerNotifier) {
        List<Validator<T>> nodes = new ArrayList<>();
        Map<Integer, String> idNameMap = new HashMap<>();
        for (int i = 0; i < numNodes; i++) {
            String nodeName = "HS-" + i;
            idNameMap.put(i, nodeName);
            RandomNumberGenerator serviceTimeGenerator = new ExponentialDistribution(validatorServiceRate);
            nodes.add(new Validator<>(nodeName, consensusLimit, timerNotifier, serviceTimeGenerator));
        }
        return new Pair<>(nodes, idNameMap);
    }

    /**
     * Fixes the arrangement of the nodes in {@code simulator} according to the given run configuration {@code json}.
     */
    private static <T extends BFTMessage> void fixNetworkConnections(RunConfigJson json, SimulatorImpl<T> simulator) {
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
