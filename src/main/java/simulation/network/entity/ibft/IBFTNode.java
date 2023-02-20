package simulation.network.entity.ibft;

import static simulation.network.entity.ibft.IBFTMessage.NULL_VALUE;

import simulation.network.entity.TimedNetworkNode;
import simulation.network.entity.NodeTimerNotifier;
import simulation.network.entity.Payload;
import simulation.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IBFTNode extends TimedNetworkNode {

    /**
     * Dummy value to be passed around as part of the protocol.
     */
    private static final int DUMMY_VALUE = 1;

    // Simulation variables
    private final double baseTimeLimit;
    private int N;
    private int F;
    private List<IBFTNode> allNodes;

    // Helper attributes
    private int timerExpiryCount; // Used to differentiate multiple timers in the same instance & round
    private IBFTMessageHolder messageHolder;
    /**
     * Stores payloads while node is processing a message.
     * All payloads are retrieved and sent out after message processing.
     */
    private List<Payload> tempPayloadStore;

    private final int p_i; // identifier
    private int lambda_i; // consensus instance identifier
    private int r_i; // round number
    private int pr_i; // round at which process has prepared
    private int pv_i; // value for which process has prepared
    private int inputValue_i; // value passed as input to instance


    public IBFTNode(String name, int identifier, double baseTimeLimit, NodeTimerNotifier timerNotifier, int N) {
        super(name, timerNotifier);
        this.p_i = identifier;
        this.allNodes = new ArrayList<>();
        this.baseTimeLimit = baseTimeLimit;
        this.timerExpiryCount = 0;
        this.N = N;
        this.F = (this.N - 1) / 3;

        this.tempPayloadStore = new ArrayList<>();
        this.messageHolder = new IBFTMessageHolder(getQuorumCount());
    }

    public void setAllNodes(List<IBFTNode> allNodes) {
        this.allNodes = new ArrayList<>(allNodes);
    }

    /**
     * Retries and returns payloads generated from a processing step.
     * Empties the payload list.
     *
     * @return List of payloads that were generated from a processing step.
     */
    private List<Payload> getProcessedPayloads() {
        List<Payload> payloads = tempPayloadStore;
        tempPayloadStore = new ArrayList<>();
        return payloads;
    }

    @Override
    public List<Payload> processPayload(double time, Payload payload) {
        super.processPayload(time, payload);
        IBFTMessage message = IBFTMessage.createIBFTMessageFromString(payload.getMessage());
        processMessage(message);
        return getProcessedPayloads();
    }

    @Override
    public List<Payload> initializationPayloads() {
        start(1, DUMMY_VALUE);
        return getProcessedPayloads();
    }

    // Utility methods
    private void startTimer() {
        timerExpiryCount++; // Every time a timer starts, a unique one is set.
        notifyAtTime(getTime() + timerFunction(r_i), createTimerNotificationMessage().toString());
    }

    private void broadcastMessage(IBFTMessage message) {
        tempPayloadStore.addAll(sendMessage(message.toString(), allNodes));
    }

    private int getQuorumCount() {
        return Math.floorDiv((N + F), 2) + 1;
    }

    private double timerFunction(int round) {
        return Math.pow(2, round - 1) * baseTimeLimit;
    }
    private static int getNextRoundNumber(int roundNumber) {
        return roundNumber + 1;
    }

    private static int getLeader(int consensusInstance, int roundNumber, int N) {
        return (consensusInstance + roundNumber) % N;
    }

    // Timer expire handling
    @Override
    public List<Payload> notifyTime(double time, String stringMessage) {
        IBFTMessage message = IBFTMessage.createIBFTMessageFromString(stringMessage);
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

    public IBFTMessage createPreparedValuesMessage(IBFTMessageType type) {
        return IBFTMessage.createPreparedValuesMessage(p_i, type, lambda_i, r_i, pr_i, pv_i);
    }

    // Round start handling
    private void start(int lambda, int value) {
        timerExpiryCount = 0;

        lambda_i = lambda;
        r_i = 1;
        pr_i = NULL_VALUE;
        pv_i = NULL_VALUE;
        inputValue_i = value;
        if (getLeader(lambda_i, r_i, N) == p_i) {
            broadcastMessage(createSingleValueMessage(IBFTMessageType.PREPREPARED, inputValue_i));
        }
        startTimer();
        prePrepareOperation();
    }

    // Message parsing methods
    private void processMessage(IBFTMessage message) {
        IBFTMessageType messageType = message.getMessageType();
        messageHolder.addMessageToBacklog(message);

        switch (messageType) {
        case PREPREPARED:
            prePrepareOperation();
            break;
        case PREPARED:
            prepareOperation();
            break;
        case COMMIT:
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
    }

    // Round change handling
    private void timerExpiryOperation() {
        r_i++;
        startTimer();
        broadcastMessage(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE));
        prePrepareOperation();
        prepareOperation();
    }

    private void fPlusOneRoundChangeOperation() {
        if (messageHolder.hasMoreHigherRoundChangeMessagesThan(lambda_i, r_i)) {
            int minimumRound = messageHolder.getNextGreaterRoundChangeMessage(lambda_i, r_i);
            r_i = minimumRound;
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
                    broadcastMessage(createSingleValueMessage(IBFTMessageType.PREPREPARED, inputValue_i));
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
        // currently not necessary
        return;
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
        return String.format("%s (%d, %d, %d)",
                super.toString(),
                p_i,
                lambda_i,
                r_i);
    }
}
