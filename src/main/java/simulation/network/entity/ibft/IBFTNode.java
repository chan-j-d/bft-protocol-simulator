package simulation.network.entity.ibft;

import static simulation.network.entity.ibft.IBFTMessage.NULL_VALUE;

import simulation.network.entity.TimedNetworkNode;
import simulation.network.entity.NodeTimerNotifier;
import simulation.network.entity.Payload;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IBFTNode extends TimedNetworkNode<IBFTMessage> {

    /**
     * Dummy value to be passed around as part of the protocol.
     */
    private static final int DUMMY_VALUE = 1;
    public static final int FIRST_CONSENSUS_INSTANCE = 1;

    // Simulation variables
    private final double baseTimeLimit;
    private final int consensusLimit;
    private int N;
    private int F;
    private Map<Integer, IBFTNode> allNodes;

    // Helper attributes
    private int timerExpiryCount; // Used to differentiate multiple timers in the same instance & round
    private IBFTMessageHolder messageHolder;
    /**
     * Stores payloads while node is processing a message.
     * All payloads are retrieved and sent out after message processing.
     */
    private List<Payload<IBFTMessage>> tempPayloadStore;

    private final int p_i; // identifier
    private int lambda_i; // consensus instance identifier
    private int r_i; // round number
    private int pr_i; // round at which process has prepared
    private int pv_i; // value for which process has prepared
    private List<IBFTMessage> preparedMessageJustification;
    private Map<Integer, List<IBFTMessage>> consensusQuorum;
    private Map<Integer, Integer> otherNodeHeights;
    private int inputValue_i; // value passed as input to instance


    public IBFTNode(String name, int identifier, double baseTimeLimit, NodeTimerNotifier<IBFTMessage> timerNotifier,
            int N, int consensusLimit) {
        super(name, timerNotifier);
        this.p_i = identifier;
        this.allNodes = new HashMap<>();
        this.baseTimeLimit = baseTimeLimit;
        this.timerExpiryCount = 0;
        this.N = N;
        this.F = (this.N - 1) / 3;
        this.consensusLimit = consensusLimit;

        this.tempPayloadStore = new ArrayList<>();
        this.messageHolder = new IBFTMessageHolder(getQuorumCount(), FIRST_CONSENSUS_INSTANCE);
        this.consensusQuorum = new HashMap<>();
        this.otherNodeHeights = new HashMap<>();
    }

    public void setAllNodes(List<IBFTNode> allNodes) {
        this.allNodes = allNodes.stream()
                .collect(Collectors.toMap(node -> node.p_i, node -> node));
    }

    /**
     * Retries and returns payloads generated from a processing step.
     * Empties the payload list.
     *
     * @return List of payloads that were generated from a processing step.
     */
    private List<Payload<IBFTMessage>> getProcessedPayloads() {
        List<Payload<IBFTMessage>> payloads = tempPayloadStore;
        tempPayloadStore = new ArrayList<>();
        return payloads;
    }

    @Override
    public List<Payload<IBFTMessage>> processPayload(double time, Payload<IBFTMessage> payload) {
        super.processPayload(time, payload);
        IBFTMessage message = payload.getMessage();
        processMessage(message);
        return getProcessedPayloads();
    }

    @Override
    public List<Payload<IBFTMessage>> initializationPayloads() {
        start(FIRST_CONSENSUS_INSTANCE, DUMMY_VALUE);
        return getProcessedPayloads();
    }

    // Utility methods
    private void startTimer() {
        timerExpiryCount++; // Every time a timer starts, a unique one is set.
        notifyAtTime(getTime() + timerFunction(r_i), createTimerNotificationMessage());
    }

    private void broadcastMessage(IBFTMessage message) {
        tempPayloadStore.addAll(sendMessage(message, allNodes.values()));
    }

    private int getQuorumCount() {
        return Math.floorDiv((N + F), 2) + 1;
    }

    private double timerFunction(int round) {
        return Math.pow(2, round - 1) * baseTimeLimit;
    }

    private static int getLeader(int consensusInstance, int roundNumber, int N) {
        return (consensusInstance + roundNumber) % N;
    }

    // Timer expire handling
    @Override
    public List<Payload<IBFTMessage>> notifyTime(double time, IBFTMessage message) {
        int round = message.getRound();
        int lambda = message.getLambda();
        int messageTimerExpiryCount = message.getValue();
        if (timerExpiryCount == messageTimerExpiryCount && lambda == lambda_i && round == r_i) {
            timerExpiryOperation();
        }
        return getProcessedPayloads();
    }

    /**
     * Returns a formatted timer notification message for the node itself.
     *
     * @return formatted timer notification message containing consensus instance and round.
     */
    private IBFTMessage createTimerNotificationMessage() {
        return createSingleValueMessage(IBFTMessageType.TIMER_EXPIRY, timerExpiryCount);
    }

    // Message util
    public IBFTMessage createSingleValueMessage(IBFTMessageType type, int value) {
        return IBFTMessage.createValueMessage(p_i, type, lambda_i, r_i, value);
    }

    public IBFTMessage createSingleValueMessage(IBFTMessageType type, int value, List<IBFTMessage> piggybackMessages) {
        return IBFTMessage.createValueMessage(p_i, type, lambda_i, r_i, value, piggybackMessages);
    }

    public IBFTMessage createPreparedValuesMessage(IBFTMessageType type) {
        return IBFTMessage.createPreparedValuesMessage(p_i, type, lambda_i, r_i, pr_i, pv_i);
    }

    public IBFTMessage createPreparedValuesMessage(IBFTMessageType type, List<IBFTMessage> piggybackMessages) {
        return IBFTMessage.createPreparedValuesMessage(p_i, type, lambda_i, r_i, pr_i, pv_i, piggybackMessages);
    }

    //TODO implement sending of quorum messages to old nodes

    // Round start handling
    private void start(int lambda, int value) {
        timerExpiryCount = 0;
        lambda_i = lambda;
        r_i = 1;
        pr_i = NULL_VALUE;
        pv_i = NULL_VALUE;
        preparedMessageJustification = List.of();
        inputValue_i = value;
        if (lambda > consensusLimit) {
            return;
        }
        newRoundCleanup();
        if (getLeader(lambda_i, r_i, N) == p_i) {
            broadcastMessage(createSingleValueMessage(IBFTMessageType.PREPREPARED, inputValue_i));
        }
        startTimer();
        prePrepareOperation();
    }

    private void newRoundCleanup() {
        int minBlockHeight = otherNodeHeights.values().stream().mapToInt(x -> x).min().orElse(0);
        Set<Integer> toRemoveKeySet = List.copyOf(consensusQuorum.keySet()).stream().filter(x -> x >= minBlockHeight)
                .collect(Collectors.toSet());
        for (int oldConsensusInstance : toRemoveKeySet) {
            consensusQuorum.remove(oldConsensusInstance);
        }
    }

    // Message parsing methods
    private void processMessage(IBFTMessage message) {
        IBFTMessageType messageType = message.getMessageType();
        int sender = message.getIdentifier();
        int lambda = message.getLambda();
        otherNodeHeights.compute(sender, (k, v) -> (v == null) ? lambda : Math.max(v, lambda));

        if (lambda >= lambda_i) {
            messageHolder.addMessageToBacklog(message);
            switch (messageType) {
                case PREPREPARED:
                    prePrepareOperation();
                    break;
                case PREPARED:
                    prepareOperation();
                    break;
                case COMMIT:
                    message.getPiggybackMessages().forEach(pbMessage -> messageHolder.addMessageToBacklog(pbMessage));
                    commitOperation();
                    break;
                case ROUND_CHANGE:
                    int messageRound = message.getRound();
                    if (messageRound > r_i) {
                        fPlusOneRoundChangeOperation();
                    } else if (messageRound == r_i) {
                        leaderRoundChangeOperation();
                    }
                    break;
            }
        } else if (messageType == IBFTMessageType.ROUND_CHANGE) {
            tempPayloadStore.add(sendMessage(createSingleValueMessage(IBFTMessageType.COMMIT, NULL_VALUE,
                    consensusQuorum.get(lambda)), allNodes.get(sender)));
        }
    }

    // Round change handling
    private void timerExpiryOperation() {
        r_i++;
        startTimer();
        if (pr_i == NULL_VALUE && pv_i == NULL_VALUE) {
            broadcastMessage(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE));
        } else {
            broadcastMessage(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE, preparedMessageJustification));
        }
        prePrepareOperation();
        prepareOperation();
    }

    private void fPlusOneRoundChangeOperation() {
        if (messageHolder.hasMoreHigherRoundChangeMessagesThan(lambda_i, r_i)) {
            r_i = messageHolder.getNextGreaterRoundChangeMessage(lambda_i, r_i);
            startTimer();
            broadcastMessage(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE));
            prePrepareOperation();
            prepareOperation();
        }
    }

    private void leaderRoundChangeOperation() {
        if (p_i == getLeader(lambda_i, r_i, N)) {
            if (messageHolder.hasQuorumOfMessages(IBFTMessageType.ROUND_CHANGE, lambda_i, r_i)) {
                List<IBFTMessage> roundChangeMessages =
                        messageHolder.getQuorumOfMessages(IBFTMessageType.ROUND_CHANGE,
                                lambda_i, r_i);
                if (justifyRoundChange(roundChangeMessages)) {
                    Pair<Integer, Integer> prPvPair = highestPrepared(roundChangeMessages);
                    int pr = prPvPair.first();
                    int pv = prPvPair.second();
                    if (pr != NULL_VALUE && pv != NULL_VALUE) {
                        inputValue_i = pv;
                    }
                    broadcastMessage(createSingleValueMessage(
                            IBFTMessageType.PREPREPARED, inputValue_i, roundChangeMessages));
                }
            }
        }
    }

    // Message processing
    private void prePrepareOperation() {
        List<IBFTMessage> preprepareMessages = messageHolder.getMessages(IBFTMessageType.PREPREPARED, lambda_i, r_i);
        for (IBFTMessage message : preprepareMessages) {
            int sender = message.getIdentifier();
            if (sender == getLeader(lambda_i, r_i, N) && justifyPrePrepare(message)) {
                startTimer();
                inputValue_i = message.getValue();
                broadcastMessage(createSingleValueMessage(IBFTMessageType.PREPARED, inputValue_i));
            }
        }
    }

    private void prepareOperation() {
        if (messageHolder.hasQuorumOfMessages(IBFTMessageType.PREPARED, lambda_i, r_i)) {
            List<IBFTMessage> prepareMessages =
                    messageHolder.getQuorumOfMessages(IBFTMessageType.PREPARED, lambda_i, r_i);
            pr_i = r_i;
            pv_i = prepareMessages.get(0).getValue();
            preparedMessageJustification = prepareMessages;
            broadcastMessage(createSingleValueMessage(IBFTMessageType.COMMIT, inputValue_i));
        }
    }

    private void commitOperation() {
        if (messageHolder.hasCommitQuorumOfMessages(lambda_i)) {
            Pair<Integer, List<IBFTMessage>> valueMessagesPair = messageHolder.getRoundValueToCommit(lambda_i);
            commit(lambda_i, valueMessagesPair.first(), valueMessagesPair.second());
            messageHolder.advanceConsensusInstance(lambda_i, lambda_i + 1);
            lambda_i++;
            start(lambda_i, DUMMY_VALUE);
        }
    }

    private void commit(int consensusInstance, int value, List<IBFTMessage> messages) {
        consensusQuorum.put(consensusInstance, messages);
    }

    // Message justification

    /**
     * Returns true if the quorum of round change messages is justified.
     *
     * @param roundChangeMessageQuorum quorum of valid round change messages
     * @return true if the round change quorum is justified.
     */
    private boolean justifyRoundChange(List<IBFTMessage> roundChangeMessageQuorum) {
        Pair<Integer, Integer> quorumAnalysis = highestPrepared(roundChangeMessageQuorum);
        int highestPr = quorumAnalysis.first();
        int highestPv = quorumAnalysis.second();
        return j1Justification(roundChangeMessageQuorum) ||
                j2Justification(roundChangeMessageQuorum, lambda_i, highestPr, highestPv);
    }

    private boolean j1Justification(List<IBFTMessage> messages) {
        return messages.stream().filter(message ->
                message.getPreparedRound() == NULL_VALUE && message.getPreparedValue() == NULL_VALUE).count() >=
                getQuorumCount();
    }

    private boolean j2Justification(List<IBFTMessage> messages, int lambda_i, int highestPr, int highestPv) {
        return messages.size() >= getQuorumCount() &&
                messages.stream().anyMatch(message -> message.getPiggybackMessages().stream().filter(pbm ->
                                pbm.getMessageType() == IBFTMessageType.PREPARED &&
                                pbm.getLambda() == lambda_i &&
                                pbm.getPreparedValue() == highestPr &&
                                pbm.getPreparedValue() == highestPv)
                        .count() >= getQuorumCount());
    }

    /**
     * Returns true if the received pre-prepare message is justified.
     *
     * @param message of type pre-prepare to be analyzed.
     * @return true if the received pre-prepare message is justified.
     */
    private boolean justifyPrePrepare(IBFTMessage message) {
        int messageRound = message.getRound();
        if (messageRound == 1) {
            return true;
        }

        List<IBFTMessage> piggybackMessages = message.getPiggybackMessages();
        List<IBFTMessage> validPiggybackMessages = piggybackMessages.stream()
                .filter(m -> m.getMessageType() == IBFTMessageType.ROUND_CHANGE &&
                        m.getRound() == messageRound &&
                        m.getLambda() == message.getLambda())
                .collect(Collectors.toList());
        System.out.println(message);
        System.out.println("j1: " + (validPiggybackMessages.size() >= getQuorumCount()));
        System.out.println("j2: " + justifyRoundChange(validPiggybackMessages));
        return validPiggybackMessages.size() >= getQuorumCount() && justifyRoundChange(validPiggybackMessages);
    }

    /**
     * Returns number of double null messages and highest prepared round with its associated prepared value.
     *
     * @param messageQuorum to be analyzed.
     * @return Pair with first being the number of double null messages, and second being the pair of prepared numbers.
     */
    private Pair<Integer, Integer> highestPrepared(List<? extends IBFTMessage> messageQuorum) {
        int pv = NULL_VALUE;
        int pr = NULL_VALUE;
        for (IBFTMessage message : messageQuorum) {
            int message_pv = message.getPreparedValue();
            int message_pr = message.getPreparedRound();
            if (pr == NULL_VALUE || message_pr > pr) {
                pr = message_pr;
                pv = message_pv;
            }  // else don't update
        }
        return new Pair<>(pr, pv);
    }

    @Override
    public String toString() {
        return String.format("%s (%d, %d)",
                super.toString(),
                lambda_i,
                r_i);
    }
}
