package simulation.simulator;

import simulation.json.input.FaultConfigJson;
import simulation.json.input.NetworkConfigurationJson;
import simulation.json.input.RngConfigJson;
import simulation.json.input.RunConfigJson;
import simulation.json.input.SwitchConfigJson;
import simulation.json.input.ValidatorConfigJson;
import simulation.network.entity.BFTMessage;
import simulation.network.entity.EndpointNode;
import simulation.network.entity.Validator;
import simulation.network.entity.fault.UnresponsiveValidator;
import simulation.network.entity.timer.TimerNotifier;
import simulation.network.router.Switch;
import simulation.network.topology.ArrayTopololgy;
import simulation.network.topology.ButterflyTopology;
import simulation.network.topology.DragonflyTopology;
import simulation.network.topology.SimpleTopology;
import simulation.protocol.ConsensusProgram;
import simulation.protocol.hotstuff.HSMessage;
import simulation.protocol.hotstuff.HSReplica;
import simulation.protocol.ibft.IBFTMessage;
import simulation.protocol.ibft.IBFTNode;
import simulation.util.Pair;
import simulation.util.rng.DegenerateDistribution;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;
import simulation.util.rng.UniformDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        RandomNumberGenerator nodeRng = getRngFromConfig(validatorSettings.getNodeProcessingDistribution());
        FaultConfigJson faultSettings = validatorSettings.getFaultSettings();

        switch (consensusProtocol) {
        case "hs": case "hotstuff":
            SimulatorImpl<HSMessage> hsSimulator = new SimulatorImpl<>();
            Pair<List<Validator<HSMessage>>, Map<Integer, String>> hsPair = createValidatorNodes(numNodes,
                    nodeRng, consensusLimit, hsSimulator, faultSettings);

            List<Validator<HSMessage>> hsNodes = hsPair.first();
            Map<Integer, String> idNameMap = hsPair.second();

            for (int i = 0; i < numNodes; i++) {
                Validator<HSMessage> currentNode = hsNodes.get(i);
                for (int j = 0; j < validatorSettings.getNumPrograms(); j++) {
                    String programName = idNameMap.get(i) + "-P" + j;
                    ConsensusProgram<HSMessage> program = new HSReplica(programName, i, baseTimeLimit,
                            numNodes, currentNode);
                    currentNode.addConsensusProgram(program);
                }
            }

            hsSimulator.setNodes(hsNodes);
            fixNetworkConnections(json, hsSimulator);
            return hsSimulator;
        case "ibft":
            SimulatorImpl<IBFTMessage> ibftSimulator = new SimulatorImpl<>();
            Pair<List<Validator<IBFTMessage>>, Map<Integer, String>> ibftPair = createValidatorNodes(numNodes,
                    nodeRng, consensusLimit, ibftSimulator, faultSettings);

            List<Validator<IBFTMessage>> ibftNodes = ibftPair.first();
            idNameMap = ibftPair.second();

            for (int i = 0; i < numNodes; i++) {
                Validator<IBFTMessage> currentNode = ibftNodes.get(i);
                for (int j = 0; j < validatorSettings.getNumPrograms(); j++) {
                    String programName = idNameMap.get(i) + "-P" + j;
                    ConsensusProgram<IBFTMessage> program = new IBFTNode(programName, i, baseTimeLimit,
                            numNodes, currentNode);
                    currentNode.addConsensusProgram(program);
                }
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
     * @param nodeRng Processing time for node.
     * @param consensusLimit Limit of consensus to be simulated.
     * @param timerNotifier Time notification for the validator. Used for setting timers.
     * @param faultSettings Fault node settings.
     * @return Pair of list of validators and map of ids to node name.
     */
    private static <T extends BFTMessage> Pair<List<Validator<T>>, Map<Integer, String>> createValidatorNodes(
            int numNodes, RandomNumberGenerator nodeRng, int consensusLimit, TimerNotifier<Validator<T>> timerNotifier,
            FaultConfigJson faultSettings) {
        List<Validator<T>> nodes = new ArrayList<>();
        Map<Integer, String> idNameMap = new HashMap<>();
        int numFaults = faultSettings.getNumFaults();
        String faultType = faultSettings.getFaultType();
        for (int i = 0; i < numNodes; i++) {
            String nodeName = "Val-" + i;
            idNameMap.put(i, nodeName);
        }
        for (int i = 0; i < numNodes; i++) {
            String nodeName = "Val-" + i;
            Validator<T> faultyNode;
            if (i < numFaults) {
                switch (faultType) {
                    case "unresponsive": case "ur":
                        faultyNode = new UnresponsiveValidator<>(nodeName, timerNotifier);
                        break;
                    default:
                        throw new RuntimeException(String.format("Unrecognized fault type (%s).", faultType));
                }
                nodes.add(faultyNode);
            } else {
                nodes.add(new Validator<>(nodeName, idNameMap, consensusLimit, timerNotifier, nodeRng));
            }
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
        RandomNumberGenerator switchServiceTimeGenerator =
                getRngFromConfig(switchSettings.getSwitchProcessingDistribution());
        double messageChannelSuccessRate = switchSettings.getMessageChannelSuccessRate();
        String networkType = networkSettings.getNetworkType();
        List<Integer> networkParameters = networkSettings.getNetworkParameters();
        switch (networkType) {
            case "foldedclos": case "fc":
                return ButterflyTopology.arrangeFoldedClosStructure(nodes, networkParameters,
                        messageChannelSuccessRate, switchServiceTimeGenerator);
            case "butterfly": case "b":
                return ButterflyTopology.arrangeButterflyStructure(nodes, networkParameters,
                        messageChannelSuccessRate, switchServiceTimeGenerator);
            case "clique": case "c":
                return SimpleTopology.arrangeCliqueStructure(nodes, messageChannelSuccessRate,
                        switchServiceTimeGenerator);
            case "torus": case "t":
                return ArrayTopololgy.arrangeTorusStructure(nodes, networkParameters, messageChannelSuccessRate,
                        switchServiceTimeGenerator);
            case "mesh": case "m":
                return ArrayTopololgy.arrangeMeshStructure(nodes, networkParameters, messageChannelSuccessRate,
                        switchServiceTimeGenerator);
            case "dragonfly": case "df":
                return DragonflyTopology.arrangeDragonflyStructure(nodes, networkParameters, messageChannelSuccessRate,
                        switchServiceTimeGenerator);
            default:
                throw new RuntimeException(String.format("The network type %s has not been defined/implemented.",
                        networkType));
        }
    }

    /**
     * Returns the appropriate rng distribution from the given {@code rngConfigJson}.
     */
    private static RandomNumberGenerator getRngFromConfig(RngConfigJson rngConfigJson) {
        String distributionType = rngConfigJson.getDistributionType();
        List<Double> distributionParameters = rngConfigJson.getParameters();

        switch (distributionType) {
            case "degen": case "d": case "degenerate":
                return new DegenerateDistribution(distributionParameters.get(0));
            case "exp": case "e": case "exponential":
                return new ExponentialDistribution(distributionParameters.get(0));
            case "uni": case "u": case "uniform":
                return new UniformDistribution(distributionParameters.get(0), distributionParameters.get(1));
            default:
                throw new RuntimeException("Not a valid distribution specification:\n" + rngConfigJson);
        }
    }
}
